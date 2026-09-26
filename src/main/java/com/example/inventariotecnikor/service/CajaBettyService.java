package com.example.inventariotecnikor.service;

import com.example.inventariotecnikor.model.MovimientoCajaBetty;
import com.example.inventariotecnikor.model.VentaBetty;
import com.example.inventariotecnikor.repository.MovimientoCajaBettyRepository;
import com.example.inventariotecnikor.repository.VentaBettyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class CajaBettyService {

    private final VentaBettyRepository ventaRepo;
    private final MovimientoCajaBettyRepository cajaRepo;

    public CajaBettyService(VentaBettyRepository ventaRepo,
                             MovimientoCajaBettyRepository cajaRepo) {
        this.ventaRepo = ventaRepo;
        this.cajaRepo = cajaRepo;
    }

    /**
     * Cierre de caja para un rango de fechas.
     */
    public CierreCajaBetty cierre(LocalDate desde, LocalDate hasta) {
        List<VentaBetty> ventas = ventaRepo.findByFechaBetweenOrderByFechaDesc(
                desde.atStartOfDay(), hasta.plusDays(1).atStartOfDay());
        List<MovimientoCajaBetty> movimientos = cajaRepo.findByFechaBetweenOrderByFechaDesc(
                desde.atStartOfDay(), hasta.plusDays(1).atStartOfDay());

        // Solo ventas COMPLETADAS
        List<VentaBetty> ventasActivas = ventas.stream()
                .filter(v -> "COMPLETADA".equals(v.getEstado()))
                .toList();

        BigDecimal totalEfectivo = ventasActivas.stream()
                .filter(v -> "EFECTIVO".equals(v.getFormaPago()))
                .map(VentaBetty::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal totalTransferencia = ventasActivas.stream()
                .filter(v -> "TRANSFERENCIA".equals(v.getFormaPago()))
                .map(VentaBetty::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal totalIngresos = movimientos.stream()
                .filter(m -> "INGRESO".equals(m.getTipo()))
                .map(MovimientoCajaBetty::getMonto)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal totalEgresos = movimientos.stream()
                .filter(m -> "EGRESO".equals(m.getTipo()))
                .map(MovimientoCajaBetty::getMonto)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        // Efectivo esperado = ventas en efectivo + ingresos manuales - egresos manuales
        // (los movimientos automaticos de ventas ya estan incluidos en totalIngresos)
        // Para evitar doble conteo, calculamos solo con los movimientos manuales
        BigDecimal ingresosManual = movimientos.stream()
                .filter(m -> "INGRESO".equals(m.getTipo()))
                .filter(m -> m.getMotivo() != null && !m.getMotivo().startsWith("Venta #"))
                .map(MovimientoCajaBetty::getMonto)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal egresosManual = movimientos.stream()
                .filter(m -> "EGRESO".equals(m.getTipo()))
                .filter(m -> m.getMotivo() != null && !m.getMotivo().startsWith("Anulacion venta #"))
                .map(MovimientoCajaBetty::getMonto)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal efectivoEsperado = totalEfectivo.add(ingresosManual).subtract(egresosManual);

        return new CierreCajaBetty(
                totalEfectivo, totalTransferencia,
                totalIngresos, totalEgresos,
                efectivoEsperado, ventas, movimientos);
    }

    @Transactional
    public void registrarIngreso(BigDecimal monto, String motivo, String responsable) {
        validar(monto, motivo);
        cajaRepo.save(new MovimientoCajaBetty("INGRESO", monto, motivo.trim(), limpiar(responsable)));
    }

    @Transactional
    public void registrarEgreso(BigDecimal monto, String motivo, String responsable) {
        validar(monto, motivo);
        cajaRepo.save(new MovimientoCajaBetty("EGRESO", monto, motivo.trim(), limpiar(responsable)));
    }

    private static void validar(BigDecimal monto, String motivo) {
        if (monto == null || monto.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El monto debe ser mayor que cero.");
        }
        if (motivo == null || motivo.isBlank()) {
            throw new IllegalArgumentException("El motivo es obligatorio.");
        }
    }

    private static String limpiar(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }

    /**
     * Record con el resumen del cierre de caja Betty.
     */
    public record CierreCajaBetty(
            BigDecimal totalEfectivo,
            BigDecimal totalTransferencia,
            BigDecimal totalIngresos,
            BigDecimal totalEgresos,
            BigDecimal efectivoEsperado,
            List<VentaBetty> ventas,
            List<MovimientoCajaBetty> movimientos
    ) {}
}
