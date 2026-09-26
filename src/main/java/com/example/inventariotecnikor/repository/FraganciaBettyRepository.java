package com.example.inventariotecnikor.repository;

import com.example.inventariotecnikor.model.FraganciaBetty;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FraganciaBettyRepository extends JpaRepository<FraganciaBetty, Long> {
    List<FraganciaBetty> findByActivoTrueOrderByNombreAsc();
}
