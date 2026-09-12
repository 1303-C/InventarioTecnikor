package com.example.inventariotecnikor.service;

import com.example.inventariotecnikor.model.EstadoVenta;
import com.example.inventariotecnikor.model.FormaPago;
import com.example.inventariotecnikor.model.MovimientoCaja;
import com.example.inventariotecnikor.model.OrigenMovimientoCaja;
import com.example.inventariotecnikor.model.TipoMovimientoCaja;
import com.example.inventariotecnikor.model.Venta;
import com.example.inventariotecnikor.repository.MovimientoCajaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Caja: cierre de un rango de fechas y movimientos de efectivo que no son
 * venta (pago a proveedor, gastos, abonos, la base con la que se abre el
 * dia...).
 *
 * "efectivo esperado en el cajon" = ventas en efectivo del rango + ingresos
 * de caja - egresos de caja. No incluye ninguna base anterior al rango que
 * no se haya registrado como ingreso: si abres el dia con una base, hay que
 * anotarla como un ingreso ("Apertura de caja").
 */
@Service
@Transactional(readOnly = true)
public class CajaService {

    private final VentaService ventaService;
    private final MovimientoCajaRepository movimientoCajaRepository;

    public CajaService(VentaService ventaService, MovimientoCajaRepository movimientoCajaRepository) {
        this.ventaService = ventaService;
        this.movimientoCajaRepository = movimientoCajaRepository;
    }

    // ------------------------------------------------------------------
    //  Registrar movimientos manuales
    // ------------------------------------------------------------------

    @Transactional
    public MovimientoCaja registrarIngreso(BigDecimal monto, String motivo, String responsable) {
        return registrar(TipoMovimientoCaja.INGRESO, monto, motivo, responsable, OrigenMovimientoCaja.MANUAL);
    }

    @Transactional
    public MovimientoCaja registrarEgreso(BigDecimal monto, String motivo, String responsable) {
        return registrar(TipoMovimientoCaja.EGRESO, monto, motivo, responsable, OrigenMovimientoCaja.MANUAL);
    }

    private MovimientoCaja registrar(TipoMovimientoCaja tipo, BigDecimal monto, String motivo, String responsable,
                                     OrigenMovimientoCaja origen) {
        if (monto == null || monto.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El monto debe ser mayor que cero.");
        }
        if (motivo == null || motivo.isBlank()) {
            throw new IllegalArgumentException("Todo movimiento de caja necesita un motivo.");
        }
        return movimientoCajaRepository.save(new MovimientoCaja(
                tipo, monto.setScale(2, RoundingMode.HALF_UP), motivo.trim(), limpiar(responsable), origen));
    }

    // ------------------------------------------------------------------
    //  Cierre / arqueo
    // ------------------------------------------------------------------

    public List<MovimientoCaja> movimientosEnRango(LocalDate desde, LocalDate hasta) {
        validarRango(desde, hasta);
        return movimientoCajaRepository.findByFechaBetweenOrderByFechaDesc(
                desde.atStartOfDay(), hasta.plusDays(1).atStartOfDay());
    }

    /**
     * Cierre de un rango de fechas (un solo dia si desde == hasta): ventas
     * por forma de pago mas los movimientos de caja del rango. Las ventas
     * ANULADAS no cuentan.
     */
    public CierreCaja cierre(LocalDate desde, LocalDate hasta) {
        validarRango(desde, hasta);

        List<Venta> ventas = ventaService.ventasEnRango(desde, hasta).stream()
                .filter(v -> v.getEstado() != EstadoVenta.ANULADA)
                .toList();
        List<MovimientoCaja> movimientos = movimientosEnRango(desde, hasta);

        List<CierreCaja.PorFormaPago> desglose = new ArrayList<>();
        BigDecimal totalVentas = cero();
        BigDecimal efectivoVentas = cero();
        for (FormaPago forma : FormaPago.values()) {
            List<Venta> delTipo = ventas.stream().filter(v -> v.getFormaPago() == forma).toList();
            BigDecimal totalTipo = delTipo.stream()
                    .map(Venta::getTotal)
                    .reduce(cero(), BigDecimal::add)
                    .setScale(2, RoundingMode.HALF_UP);

            desglose.add(new CierreCaja.PorFormaPago(forma, delTipo.size(), totalTipo));
            totalVentas = totalVentas.add(totalTipo);
            if (forma == FormaPago.EFECTIVO) {
                efectivoVentas = totalTipo;
            }
        }

        BigDecimal ingresosCaja = sumaPorTipo(movimientos, TipoMovimientoCaja.INGRESO);
        BigDecimal egresosCaja = sumaPorTipo(movimientos, TipoMovimientoCaja.EGRESO);
        BigDecimal efectivoEsperado = efectivoVentas.add(ingresosCaja).subtract(egresosCaja);
        BigDecimal ajustesArqueo = movimientos.stream()
                .filter(m -> m.getOrigen() == OrigenMovimientoCaja.ARQUEO)
                .map(m -> m.getMonto().multiply(BigDecimal.valueOf(m.getTipo().getSigno())))
                .reduce(cero(), BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        return new CierreCaja(desde, hasta, ventas.size(), totalVentas.setScale(2, RoundingMode.HALF_UP),
                desglose, ingresosCaja, egresosCaja, efectivoEsperado, ajustesArqueo, movimientos);
    }

    /**
     * Arqueo: compara el efectivo CONTADO fisicamente contra el esperado del
     * rango. Si hay diferencia, la registra sola como movimiento de caja
     * (INGRESO si sobra, EGRESO si falta) con el rastro del arqueo en el
     * motivo, igual que MovimientoService.ajustarA(...) con el stock. Si
     * coincide, no registra nada.
     */
    @Transactional
    public Optional<MovimientoCaja> arquear(LocalDate desde, LocalDate hasta,
                                            BigDecimal contado, String responsable) {
        if (contado == null || contado.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("El efectivo contado no puede ser negativo.");
        }
        BigDecimal esperado = cierre(desde, hasta).efectivoEsperado();
        BigDecimal diferencia = contado.setScale(2, RoundingMode.HALF_UP)
                .subtract(esperado);

        if (diferencia.compareTo(BigDecimal.ZERO) == 0) {
            return Optional.empty();
        }

        String rango = desde.equals(hasta) ? desde.toString() : desde + " a " + hasta;
        String detalle = "Arqueo " + rango + ": esperado " + esperado + ", contado " + contado;

        return Optional.of(diferencia.compareTo(BigDecimal.ZERO) > 0
                ? registrar(TipoMovimientoCaja.INGRESO, diferencia, detalle, responsable, OrigenMovimientoCaja.ARQUEO)
                : registrar(TipoMovimientoCaja.EGRESO, diferencia.abs(), detalle, responsable, OrigenMovimientoCaja.ARQUEO));
    }

    // ------------------------------------------------------------------

    private static BigDecimal sumaPorTipo(List<MovimientoCaja> movimientos, TipoMovimientoCaja tipo) {
        return movimientos.stream()
                .filter(m -> m.getTipo() == tipo)
                .map(MovimientoCaja::getMonto)
                .reduce(cero(), BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private static void validarRango(LocalDate desde, LocalDate hasta) {
        if (desde == null || hasta == null) {
            throw new IllegalArgumentException("Faltan las fechas del rango.");
        }
        if (hasta.isBefore(desde)) {
            throw new IllegalArgumentException("La fecha \"hasta\" no puede ser anterior a \"desde\".");
        }
    }

    private static BigDecimal cero() {
        return BigDecimal.ZERO.setScale(2);
    }

    private static String limpiar(String texto) {
        return (texto == null || texto.isBlank()) ? null : texto.trim();
    }
}
