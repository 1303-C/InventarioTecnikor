package com.example.inventariotecnikor.repository;

import com.example.inventariotecnikor.model.MovimientoCaja;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Acceso a datos de los movimientos de caja (ingresos/egresos que no son
 * venta). Solo se insertan y se consultan filas.
 */
public interface MovimientoCajaRepository extends JpaRepository<MovimientoCaja, Long> {

    List<MovimientoCaja> findByFechaBetweenOrderByFechaDesc(LocalDateTime desde, LocalDateTime hasta);
}
