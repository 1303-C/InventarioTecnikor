package com.example.inventariotecnikor.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Categoria de lavadora para alquiler (Pequena / Grande / Premium, hoy) con
 * su tarifa por hora. Es un catalogo chico y editable: los precios todavia
 * no estan definidos, asi que se pueden ajustar desde una pantalla sin
 * tocar codigo.
 *
 * No es un Producto: no tiene QR ni stock. El "inventario" de este catalogo
 * son las {@link Lavadora} (unidades fisicas) que apuntan a un plan.
 */
@Entity
@Table(name = "planes_alquiler")
public class PlanAlquiler {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 60)
    @Column(nullable = false, unique = true, length = 60)
    private String nombre;

    @PositiveOrZero
    @Column(name = "tarifa_por_hora", nullable = false, precision = 12, scale = 2)
    private BigDecimal tarifaPorHora;

    /** Baja logica: para dejar de ofrecerlo sin perder el historial de lo ya alquilado. */
    @Column(nullable = false)
    private boolean activo = true;

    protected PlanAlquiler() {
    }

    public PlanAlquiler(String nombre, BigDecimal tarifaPorHora) {
        this.nombre = nombre;
        this.tarifaPorHora = tarifaPorHora;
    }

    public Long getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public BigDecimal getTarifaPorHora() {
        return tarifaPorHora;
    }

    public void setTarifaPorHora(BigDecimal tarifaPorHora) {
        this.tarifaPorHora = tarifaPorHora;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }
}
