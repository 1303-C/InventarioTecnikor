package com.example.inventariotecnikor.repository;

import com.example.inventariotecnikor.model.ProductoBetty;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductoBettyRepository extends JpaRepository<ProductoBetty, Long> {
    List<ProductoBetty> findByActivoTrueOrderByNombreAsc();
}
