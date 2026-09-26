package com.example.inventariotecnikor.repository;

import com.example.inventariotecnikor.model.PrecioPerfumeBetty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface PrecioPerfumeBettyRepository extends JpaRepository<PrecioPerfumeBetty, Long> {
    Optional<PrecioPerfumeBetty> findByMlAndOnzas(Integer ml, BigDecimal onzas);
    List<PrecioPerfumeBetty> findAllByOrderByMlAscOnzasAsc();
    List<PrecioPerfumeBetty> findByMlOrderByOnzasAsc(Integer ml);

    @Query("SELECT DISTINCT p.ml FROM PrecioPerfumeBetty p ORDER BY p.ml ASC")
    List<Integer> findDistinctMlByOrderByMlAsc();
}
