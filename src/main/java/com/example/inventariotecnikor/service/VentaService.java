package com.example.inventariotecnikor.service;

import com.example.inventariotecnikor.exception.RecursoNoEncontradoException;
import com.example.inventariotecnikor.model.FormaPago;
import com.example.inventariotecnikor.model.Lavadora;
import com.example.inventariotecnikor.model.LineaVenta;
import com.example.inventariotecnikor.model.Producto;
import com.example.inventariotecnikor.model.TipoLinea;
import com.example.inventariotecnikor.model.Venta;
import com.example.inventariotecnikor.repository.ProductoRepository;
import com.example.inventariotecnikor.repository.VentaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Registro de ventas de mostrador: repuestos (producto), alquiler de
 * lavadoras por horas y servicios de mantenimiento (mano de obra libre).
 *
 * registrar(...) hace TODO en una sola transaccion:
 *  - por cada linea de PRODUCTO, descuenta el stock llamando a
 *    {@link MovimientoService#registrarSalida}. Si falta stock,
 *    StockInsuficienteException revierte la venta entera.
 *  - por cada linea de ALQUILER, valida que la lavadora este disponible y,
 *    ya guardada la venta, arranca el prestamo con
 *    {@link AlquilerService#iniciar}, que la deja "Prestada".
 *  - las lineas de MANTENIMIENTO son solo texto y monto libres: no tocan
 *    stock ni lavadoras.
 *
 * El stock se sigue moviendo por un unico camino (MovimientoService), asi
 * que cada venta de producto deja tambien su rastro en el historial del
 * producto con el motivo "Venta #N".
 */
@Service
@Transactional(readOnly = true)
public class VentaService {

    private final VentaRepository ventaRepository;
    private final ProductoRepository productoRepository;
    private final MovimientoService movimientoService;
    private final AlquilerService alquilerService;

    public VentaService(VentaRepository ventaRepository,
                        ProductoRepository productoRepository,
                        MovimientoService movimientoService,
                        AlquilerService alquilerService) {
        this.ventaRepository = ventaRepository;
        this.productoRepository = productoRepository;
        this.movimientoService = movimientoService;
        this.alquilerService = alquilerService;
    }

    /** Una linea de producto pedida desde el carrito: que producto y cuantas unidades. */
    public record LineaSolicitada(Long productoId, int cantidad) {
    }

    /**
     * Una linea libre (ALQUILER o MANTENIMIENTO): sin producto, descripcion y
     * precio escritos a mano. Para ALQUILER, "cantidad" son las horas y hay
     * que indicar que lavadora se presta y a nombre de quien; para
     * MANTENIMIENTO, lavadoraId/clienteAlquiler van en null.
     */
    public record LineaLibreSolicitada(TipoLinea tipo, String descripcion, int cantidad,
                                       BigDecimal precioUnitario, Long lavadoraId, String clienteAlquiler) {
    }

    /** Alquiler ya validado, pendiente de arrancar una vez la venta este guardada. */
    private record PendienteAlquiler(Lavadora lavadora, String cliente, int horas) {
    }

    /**
     * @param productos       lineas de repuestos (puede ir vacia)
     * @param libres          lineas de alquiler/mantenimiento (puede ir vacia)
     * @param formaPago       EFECTIVO / TARJETA / TRANSFERENCIA
     * @param montoRecibido   solo para EFECTIVO; debe cubrir el total
     * @param responsable     quien cobra (texto libre, puede ir vacio)
     * @param clienteNombre   opcional
     * @param clienteDocumento opcional
     */
    @Transactional
    public Venta registrar(List<LineaSolicitada> productos,
                           List<LineaLibreSolicitada> libres,
                           FormaPago formaPago,
                           BigDecimal montoRecibido,
                           String responsable,
                           String clienteNombre,
                           String clienteDocumento) {

        boolean sinProductos = productos == null || productos.isEmpty();
        boolean sinLibres = libres == null || libres.isEmpty();
        if (sinProductos && sinLibres) {
            throw new IllegalArgumentException("La venta no tiene ninguna linea.");
        }
        if (formaPago == null) {
            throw new IllegalArgumentException("Falta la forma de pago.");
        }

        long numero = ventaRepository.ultimoNumero() + 1;
        Venta venta = new Venta(numero, formaPago, limpiar(responsable));
        venta.setCliente(limpiar(clienteNombre), limpiar(clienteDocumento));

        if (!sinProductos) {
            for (LineaSolicitada s : productos) {
                if (s.cantidad() <= 0) {
                    throw new IllegalArgumentException(
                            "La cantidad de cada linea debe ser mayor que cero.");
                }
                Producto producto = productoRepository.findById(s.productoId())
                        .orElseThrow(() -> new RecursoNoEncontradoException(
                                "No existe el producto con id: " + s.productoId()));
                if (producto.getPrecioVenta() == null) {
                    throw new IllegalArgumentException(
                            "El producto \"" + producto.getNombre() + "\" no tiene precio de venta.");
                }

                venta.addLinea(new LineaVenta(producto, s.cantidad(), producto.getPrecioVenta(), 0));

                // Baja de stock por el camino unico. Si no hay stock, revienta
                // aqui y la transaccion revierte la venta completa.
                movimientoService.registrarSalida(
                        s.productoId(), s.cantidad(), "Venta #" + numero, venta.getResponsable());
            }
        }

        List<PendienteAlquiler> pendientes = new ArrayList<>();
        if (!sinLibres) {
            for (LineaLibreSolicitada l : libres) {
                if (l.tipo() == null || l.tipo() == TipoLinea.PRODUCTO) {
                    throw new IllegalArgumentException(
                            "Una linea libre debe ser de tipo ALQUILER o MANTENIMIENTO.");
                }

                Lavadora lavadora = null;
                if (l.tipo() == TipoLinea.ALQUILER) {
                    if (l.lavadoraId() == null) {
                        throw new IllegalArgumentException("Falta elegir la lavadora del alquiler.");
                    }
                    // Valida disponibilidad ANTES de cobrar: si esta prestada, no se cobra.
                    lavadora = alquilerService.lavadoraDisponible(l.lavadoraId());
                }

                venta.addLinea(new LineaVenta(l.tipo(), l.descripcion(), l.cantidad(), l.precioUnitario()));

                if (lavadora != null) {
                    pendientes.add(new PendienteAlquiler(lavadora, l.clienteAlquiler(), l.cantidad()));
                }
            }
        }

        venta.recalcularTotales();
        venta.registrarPago(formaPago, montoRecibido);
        Venta guardada = ventaRepository.save(venta);

        // Ya cobrado: ahora si se entrega la lavadora y queda "Prestada".
        for (PendienteAlquiler p : pendientes) {
            alquilerService.iniciar(p.lavadora(), guardada, p.cliente(), p.horas());
        }

        return guardada;
    }

    /**
     * Anula una venta: devuelve el stock de cada linea de PRODUCTO (una
     * ENTRADA por linea, con motivo "Anulacion Venta #N") y, si genero algun
     * alquiler que siga activo, la lavadora vuelve a estar disponible. Las
     * lineas de MANTENIMIENTO no tocan nada mas. Todo en una transaccion: o
     * se revierte y se anula todo, o no se anula nada.
     *
     * No se puede anular una venta ya anulada.
     */
    @Transactional
    public Venta anular(Long ventaId, String motivo, String responsable) {
        if (motivo == null || motivo.isBlank()) {
            throw new IllegalArgumentException("Indica el motivo de la anulacion.");
        }
        Venta venta = obtenerPorId(ventaId);

        for (LineaVenta linea : venta.getLineas()) {
            if (linea.getTipo() == TipoLinea.PRODUCTO) {
                movimientoService.registrarEntrada(linea.getProducto().getId(), linea.getCantidad(),
                        "Anulacion Venta #" + venta.getNumero(), limpiar(responsable));
            }
        }
        alquilerService.revertirPorVenta(venta.getId());

        venta.anular(motivo.trim(), limpiar(responsable));
        return venta;
    }

    // ------------------------------------------------------------------
    //  Consultas
    // ------------------------------------------------------------------

    public Venta obtenerPorId(Long id) {
        return ventaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe la venta con id: " + id));
    }

    /**
     * Ventas entre dos fechas (ambas incluidas), de la mas reciente a la mas
     * antigua. La usan el historial y, para el cierre de caja, CajaService.
     */
    public List<Venta> ventasEnRango(LocalDate desde, LocalDate hasta) {
        if (desde == null || hasta == null) {
            throw new IllegalArgumentException("Faltan las fechas del rango.");
        }
        if (hasta.isBefore(desde)) {
            throw new IllegalArgumentException("La fecha \"hasta\" no puede ser anterior a \"desde\".");
        }
        return ventaRepository.findByFechaBetweenOrderByFechaDesc(
                desde.atStartOfDay(), hasta.plusDays(1).atStartOfDay());
    }

    private static String limpiar(String texto) {
        return (texto == null || texto.isBlank()) ? null : texto.trim();
    }
}
