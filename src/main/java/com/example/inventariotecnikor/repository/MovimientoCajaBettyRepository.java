package com.example.inventariotecnikor.repository;

import com.example.inventariotecnikor.model.MovimientoCajaBetty;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface MovimientoCajaBettyRepository extends JpaRepository<MovimientoCajaBetty, Long> {
    List<MovimientoCajaBetty> findByFechaBetweenOrderByFechaDesc(LocalDateTime desde, LocalDateTime hasta);
}
