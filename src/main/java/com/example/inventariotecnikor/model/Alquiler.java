package com.example.inventariotecnikor.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * Registro de un prestamo de una {@link Lavadora}: a quien, desde cuando,
 * cuantas horas se acordaron (y por lo tanto cuando deberia volver) y
 * cuando volvio de verdad.
 *
 * El cobro NO vive aqui: se hace al momento de entregar la lavadora, como
 * una linea mas de una {@link Venta} (tipo ALQUILER). Este registro es el
 * lado "operativo": para saber que lavadora esta afuera y desde cuando.
 * "venta" queda de referencia para poder ver de donde salio el cobro.
 */
@Entity
@Table(
        name = "alquileres",
        indexes = {
                @Index(name = "idx_alquileres_estado", columnList = "estado")
        }
)
public class Alquiler {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lavadora_id", nullable = false)
    private Lavadora lavadora;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "venta_id", nullable = false)
    private Venta venta;

    @Column(length = 150)
    private String cliente;

    @Column(nullable = false)
    private int horas;

    @Column(name = "fecha_inicio", nullable = false, updatable = false)
    private LocalDateTime fechaInicio;

    /** fechaInicio + horas: cuando deberia volver. */
    @Column(name = "fecha_fin_estimada", nullable = false)
    private LocalDateTime fechaFinEstimada;

    @Column(name = "fecha_devolucion")
    private LocalDateTime fechaDevolucion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private EstadoAlquiler estado = EstadoAlquiler.ACTIVO;

    protected Alquiler() {
    }

    public Alquiler(Lavadora lavadora, Venta venta, String cliente, int horas) {
        this.lavadora = lavadora;
        this.venta = venta;
        this.cliente = cliente;
        this.horas = horas;
    }

    @PrePersist
    void alCrear() {
        this.fechaInicio = LocalDateTime.now();
        this.fechaFinEstimada = this.fechaInicio.plusHours(horas);
    }

    /** Sigue activo y ya paso la hora en que deberia haber vuelto. */
    public boolean estaVencido() {
        return estado == EstadoAlquiler.ACTIVO && fechaFinEstimada.isBefore(LocalDateTime.now());
    }

    /** Marca la lavadora como devuelta ahora mismo. */
    public void marcarDevuelto() {
        this.estado = EstadoAlquiler.DEVUELTO;
        this.fechaDevolucion = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Lavadora getLavadora() {
        return lavadora;
    }

    public Venta getVenta() {
        return venta;
    }

    public String getCliente() {
        return cliente;
    }

    public int getHoras() {
        return horas;
    }

    public LocalDateTime getFechaInicio() {
        return fechaInicio;
    }

    public LocalDateTime getFechaFinEstimada() {
        return fechaFinEstimada;
    }

    public LocalDateTime getFechaDevolucion() {
        return fechaDevolucion;
    }

    public EstadoAlquiler getEstado() {
        return estado;
    }
}
