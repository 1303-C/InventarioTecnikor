package com.example.inventariotecnikor.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "movimientos_caja_betty")
public class MovimientoCajaBetty {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDateTime fecha;

    /** "INGRESO" o "EGRESO" */
    private String tipo;

    private String motivo;

    @Column(precision = 12, scale = 2)
    private BigDecimal monto;

    private String responsable;

    public MovimientoCajaBetty() {}

    public MovimientoCajaBetty(String tipo, BigDecimal monto, String motivo, String responsable) {
        this.tipo = tipo;
        this.monto = monto;
        this.motivo = motivo;
        this.responsable = responsable;
        this.fecha = LocalDateTime.now();
    }

    public Long getId() { return id; }

    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }

    public BigDecimal getMonto() { return monto; }
    public void setMonto(BigDecimal monto) { this.monto = monto; }

    public String getResponsable() { return responsable; }
    public void setResponsable(String responsable) { this.responsable = responsable; }
}
