package com.example.inventariotecnikor.service;

import com.example.inventariotecnikor.exception.RecursoNoEncontradoException;
import com.example.inventariotecnikor.exception.StockInsuficienteException;
import com.example.inventariotecnikor.model.MovimientoInventario;
import com.example.inventariotecnikor.model.Producto;
import com.example.inventariotecnikor.model.TipoMovimiento;
import com.example.inventariotecnikor.repository.MovimientoInventarioRepository;
import com.example.inventariotecnikor.repository.ProductoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Registro de entradas y salidas de stock.
 *
 * Es el UNICO sitio donde cambia el stock de un producto. Cada cambio deja
 * una fila en movimientos_inventario, asi que siempre se puede reconstruir
 * el historial y auditar quien movio que.
 */
@Service
@Transactional(readOnly = true)
public class MovimientoService {

    private final ProductoRepository productoRepository;
    private final MovimientoInventarioRepository movimientoRepository;

    public MovimientoService(ProductoRepository productoRepository,
                             MovimientoInventarioRepository movimientoRepository) {
        this.productoRepository = productoRepository;
        this.movimientoRepository = movimientoRepository;
    }

    /**
     * Registra un movimiento y ajusta el stock del producto en la misma
     * transaccion: o se guardan las DOS cosas (movimiento + nuevo stock) o
     * no se guarda ninguna. Si algo falla a mitad (p. ej. stock
     * insuficiente), Spring revierte todo automaticamente porque lanzamos
     * una RuntimeException.
     *
     * @param productoId  producto afectado
     * @param tipo        ENTRADA (suma) o SALIDA (resta)
     * @param cantidad    unidades, siempre > 0
     * @param motivo      texto libre: "compra proveedor", "venta #123", "ajuste conteo"
     * @param responsable quien registra el movimiento
     */
    @Transactional
    public MovimientoInventario registrar(Long productoId,
                                          TipoMovimiento tipo,
                                          int cantidad,
                                          String motivo,
                                          String responsable) {

        if (cantidad <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser mayor que cero.");
        }

        Producto producto = productoRepository.findById(productoId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe el producto con id: " + productoId));

        // signo() vale +1 para ENTRADA y -1 para SALIDA
        int nuevoStock = producto.getStockActual() + tipo.getSigno() * cantidad;

        if (nuevoStock < 0) {
            throw new StockInsuficienteException(
                    "No hay stock suficiente de \"" + producto.getNombre() + "\". "
                    + "Disponible: " + producto.getStockActual() + ", salida pedida: " + cantidad + ".");
        }

        producto.setStockActual(nuevoStock); // dirty checking -> UPDATE al confirmar

        MovimientoInventario movimiento = new MovimientoInventario(
                producto, tipo, cantidad, nuevoStock, motivo, responsable);
        return movimientoRepository.save(movimiento);
    }

    /** Atajos legibles para los controladores. */
    @Transactional
    public MovimientoInventario registrarEntrada(Long productoId, int cantidad, String motivo, String responsable) {
        return registrar(productoId, TipoMovimiento.ENTRADA, cantidad, motivo, responsable);
    }

    @Transactional
    public MovimientoInventario registrarSalida(Long productoId, int cantidad, String motivo, String responsable) {
        return registrar(productoId, TipoMovimiento.SALIDA, cantidad, motivo, responsable);
    }

    // ------------------------------------------------------------------
    //  Consultas de historial
    // ------------------------------------------------------------------

    public List<MovimientoInventario> historialDe(Long productoId) {
        return movimientoRepository.findByProductoIdOrderByFechaDesc(productoId);
    }

    /**
     * Ultimos movimientos de todo el inventario, paginados (para el dashboard).
     * pagina empieza en 0.
     */
    public Page<MovimientoInventario> ultimos(int pagina, int tamano) {
        Pageable pageable = PageRequest.of(pagina, tamano);
        return movimientoRepository.findAllByOrderByFechaDesc(pageable);
    }
}
