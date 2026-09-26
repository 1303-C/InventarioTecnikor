package com.example.inventariotecnikor.repository;

import com.example.inventariotecnikor.model.TripleABetty;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TripleABettyRepository extends JpaRepository<TripleABetty, Long> {
    List<TripleABetty> findByActivoTrueOrderByNombreAsc();
}
