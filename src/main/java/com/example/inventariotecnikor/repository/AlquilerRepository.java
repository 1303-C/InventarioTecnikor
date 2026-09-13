package com.example.inventariotecnikor.repository;

import com.example.inventariotecnikor.model.Alquiler;
import com.example.inventariotecnikor.model.EstadoAlquiler;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AlquilerRepository extends JpaRepository<Alquiler, Long> {

    List<Alquiler> findByEstadoOrderByFechaFinEstimadaAsc(EstadoAlquiler estado);
}
