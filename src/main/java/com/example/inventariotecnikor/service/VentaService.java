package com.example.inventariotecnikor.service;

import com.example.inventariotecnikor.exception.RecursoNoEncontradoException;
import com.example.inventariotecnikor.model.FormaPago;
import com.example.inventariotecnikor.model.LineaVenta;
import com.example.inventariotecnikor.model.Producto;
import com.example.inventariotecnikor.model.Venta;
import com.example.inventariotecnikor.repository.ProductoRepository;
import com.example.inventariotecnikor.repository.VentaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Registro de ventas de mostrador.
 *
 * registrar(...) hace TODO en una sola transaccion: crea la venta con sus
 * lineas y descuenta el stock de cada producto llamando a
 * {@link MovimientoService#registrarSalida}. Si falta stock en cualquier
 * linea se lanza StockInsuficienteException y Spring revierte la venta
 * entera (no queda media venta ni stock descontado a medias).
 *
 * El stock se sigue moviendo por un unico camino (MovimientoService), asi
 * que cada venta deja tambien su rastro en el historial del producto con el
 * motivo "Venta #N".
 */
@Service
@Transactional(readOnly = true)
public class VentaService {

    private final VentaRepository ventaRepository;
    private final ProductoRepository productoRepository;
    private final MovimientoService movimientoService;

    public VentaService(VentaRepository ventaRepository,
                        ProductoRepository productoRepository,
                        MovimientoService movimientoService) {
        this.ventaRepository = ventaRepository;
        this.productoRepository = productoRepository;
        this.movimientoService = movimientoService;
    }

    /** Una linea pedida desde el carrito: que producto y cuantas unidades. */
    public record LineaSolicitada(Long productoId, int cantidad) {
    }

    /**
     * @param solicitadas    lineas del carrito (no vacio)
     * @param formaPago       EFECTIVO / TARJETA / TRANSFERENCIA
     * @param montoRecibido   solo para EFECTIVO; debe cubrir el total
     * @param responsable     quien cobra (texto libre, puede ir vacio)
     * @param clienteNombre   opcional
     * @param clienteDocumento opcional
     */
    @Transactional
    public Venta registrar(List<LineaSolicitada> solicitadas,
                           FormaPago formaPago,
                           BigDecimal montoRecibido,
                           String responsable,
                           String clienteNombre,
                           String clienteDocumento) {

        if (solicitadas == null || solicitadas.isEmpty()) {
            throw new IllegalArgumentException("La venta no tiene productos.");
        }
        if (formaPago == null) {
            throw new IllegalArgumentException("Falta la forma de pago.");
        }

        long numero = ventaRepository.ultimoNumero() + 1;
        Venta venta = new Venta(numero, formaPago, limpiar(responsable));
        venta.setCliente(limpiar(clienteNombre), limpiar(clienteDocumento));

        for (LineaSolicitada s : solicitadas) {
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

        venta.recalcularTotales();
        venta.registrarPago(formaPago, montoRecibido);
        return ventaRepository.save(venta);
    }

    // ------------------------------------------------------------------
    //  Consultas
    // ------------------------------------------------------------------

    public Venta obtenerPorId(Long id) {
        return ventaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe la venta con id: " + id));
    }

    /** Ventas de un dia concreto, de la mas reciente a la mas antigua. */
    public List<Venta> ventasDelDia(LocalDate dia) {
        return ventaRepository.findByFechaBetweenOrderByFechaDesc(
                dia.atStartOfDay(), dia.plusDays(1).atStartOfDay());
    }

    private static String limpiar(String texto) {
        return (texto == null || texto.isBlank()) ? null : texto.trim();
    }
}
