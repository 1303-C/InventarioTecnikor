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
import java.util.Optional;

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

        if (tipo == TipoMovimiento.AJUSTE) {
            throw new IllegalArgumentException(
                    "Los ajustes por conteo se registran con ajustarA(...), no con registrar(...).");
        }

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

    /**
     * Ajuste por conteo fisico: se indica cuantas unidades hay REALMENTE en
     * la estanteria y el sistema calcula la diferencia, corrige el stock y
     * deja un movimiento de tipo AJUSTE con rastro del saldo anterior en el
     * motivo ("Conteo: 8 -> 5 (-3)").
     *
     * A diferencia de una ENTRADA/SALIDA, aqui:
     *  - cantidadReal es el TOTAL contado (>= 0), no un incremento.
     *  - un ajuste a la baja NUNCA lanza StockInsuficiente: el conteo manda.
     *  - la "cantidad" que se guarda en el movimiento es |diferencia|, y el
     *    sentido (subio/bajo) queda escrito en el motivo.
     *
     * @param cantidadReal unidades contadas, >= 0
     * @param motivo       nota opcional del operario (p. ej. "roturas")
     * @param responsable  quien hizo el conteo
     * @return el movimiento generado, o Optional.empty() si el conteo ya
     *         coincidia con el stock actual (no habia nada que corregir)
     */
    @Transactional
    public Optional<MovimientoInventario> ajustarA(Long productoId,
                                                   int cantidadReal,
                                                   String motivo,
                                                   String responsable) {

        if (cantidadReal < 0) {
            throw new IllegalArgumentException("La cantidad contada no puede ser negativa.");
        }

        Producto producto = productoRepository.findById(productoId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe el producto con id: " + productoId));

        int anterior = producto.getStockActual();
        if (cantidadReal == anterior) {
            return Optional.empty(); // el conteo cuadra: no se registra nada
        }

        int diferencia = cantidadReal - anterior;
        producto.setStockActual(cantidadReal); // dirty checking -> UPDATE al confirmar

        String detalle = "Conteo: " + anterior + " -> " + cantidadReal
                + " (" + (diferencia > 0 ? "+" : "") + diferencia + ")";
        String motivoFinal = (motivo == null || motivo.isBlank())
                ? detalle
                : detalle + " - " + motivo.trim();
        if (motivoFinal.length() > 200) {
            motivoFinal = motivoFinal.substring(0, 200);
        }

        MovimientoInventario movimiento = new MovimientoInventario(
                producto, TipoMovimiento.AJUSTE, Math.abs(diferencia), cantidadReal,
                motivoFinal, responsable);
        return Optional.of(movimientoRepository.save(movimiento));
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
