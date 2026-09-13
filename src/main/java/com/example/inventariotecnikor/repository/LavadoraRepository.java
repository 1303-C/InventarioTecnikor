package com.example.inventariotecnikor.repository;

import com.example.inventariotecnikor.model.EstadoLavadora;
import com.example.inventariotecnikor.model.Lavadora;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LavadoraRepository extends JpaRepository<Lavadora, Long> {

    List<Lavadora> findAllByOrderByCodigoAsc();

    List<Lavadora> findByPlanIdAndEstadoOrderByCodigoAsc(Long planId, EstadoLavadora estado);

    boolean existsByCodigo(String codigo);
}
