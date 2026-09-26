package com.example.inventariotecnikor.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "precios_perfume_betty",
       uniqueConstraints = @UniqueConstraint(columnNames = {"ml", "onzas"}))
public class PrecioPerfumeBetty {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Integer ml;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal onzas;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal precio;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Integer getMl() { return ml; }
    public void setMl(Integer ml) { this.ml = ml; }

    public BigDecimal getOnzas() { return onzas; }
    public void setOnzas(BigDecimal onzas) { this.onzas = onzas; }

    public BigDecimal getPrecio() { return precio; }
    public void setPrecio(BigDecimal precio) { this.precio = precio; }
}
