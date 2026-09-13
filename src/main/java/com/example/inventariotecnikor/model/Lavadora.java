package com.example.inventariotecnikor.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Una unidad fisica de lavadora que Tecnikor alquila (no es un Producto del
 * inventario de repuestos: no tiene QR de venta ni stock, es un activo
 * propio que sale y vuelve).
 */
@Entity
@Table(name = "lavadoras")
public class Lavadora {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Codigo interno para identificarla (ej. "PEQ-01"). Solo para uso interno. */
    @NotBlank
    @Size(max = 30)
    @Column(nullable = false, unique = true, length = 30)
    private String codigo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_id", nullable = false)
    private PlanAlquiler plan;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoLavadora estado = EstadoLavadora.DISPONIBLE;

    @Size(max = 200)
    @Column(length = 200)
    private String notas;

    protected Lavadora() {
    }

    public Lavadora(String codigo, PlanAlquiler plan) {
        this.codigo = codigo;
        this.plan = plan;
    }

    public Long getId() {
        return id;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public PlanAlquiler getPlan() {
        return plan;
    }

    public void setPlan(PlanAlquiler plan) {
        this.plan = plan;
    }

    public EstadoLavadora getEstado() {
        return estado;
    }

    public void setEstado(EstadoLavadora estado) {
        this.estado = estado;
    }

    public String getNotas() {
        return notas;
    }

    public void setNotas(String notas) {
        this.notas = notas;
    }

    public boolean estaDisponible() {
        return estado == EstadoLavadora.DISPONIBLE;
    }
}
