package com.example.inventariotecnikor.repository;

import com.example.inventariotecnikor.model.MovimientoInventario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Acceso a datos del historial de movimientos (entradas/salidas).
 *
 * Solo se insertan y se consultan filas; nunca se editan ni se borran.
 */
public interface MovimientoInventarioRepository extends JpaRepository<MovimientoInventario, Long> {

    /** Historial completo de un producto, del mas reciente al mas antiguo. */
    List<MovimientoInventario> findByProductoIdOrderByFechaDesc(Long productoId);

    /**
     * Ultimos movimientos de todo el inventario, paginados (para el
     * dashboard). Pageable lleva numero de pagina, tamano y orden;
     * Page<> devuelve ademas el total de elementos y de paginas.
     */
    Page<MovimientoInventario> findAllByOrderByFechaDesc(Pageable pageable);
}
