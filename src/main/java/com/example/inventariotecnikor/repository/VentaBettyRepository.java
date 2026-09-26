package com.example.inventariotecnikor.repository;

import com.example.inventariotecnikor.model.VentaBetty;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface VentaBettyRepository extends JpaRepository<VentaBetty, Long> {
    Optional<VentaBetty> findTopByOrderByNumeroDesc();
    List<VentaBetty> findByFechaBetweenOrderByFechaDesc(LocalDateTime desde, LocalDateTime hasta);
}
