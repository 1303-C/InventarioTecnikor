package com.example.inventariotecnikor.repository;

import com.example.inventariotecnikor.model.PlanAlquiler;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlanAlquilerRepository extends JpaRepository<PlanAlquiler, Long> {

    List<PlanAlquiler> findByActivoTrueOrderByNombreAsc();

    boolean existsByNombre(String nombre);
}
