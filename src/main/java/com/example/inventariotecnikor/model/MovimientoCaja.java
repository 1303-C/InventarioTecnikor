package com.example.inventariotecnikor.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entrada o salida de efectivo del cajon que NO es una venta: pago a
 * proveedor, un gasto, un abono de cliente, la base con la que se abre el
 * dia... Es el mismo patron que MovimientoInventario: un libro mayor que
 * solo se inserta y se consulta, nunca se edita ni se borra.
 *
 * Las ventas en efectivo YA mueven el cajon (se calculan aparte, desde
 * Venta); esta tabla es solo lo que se registra a mano.
 */
@Entity
@Table(
        name = "movimientos_caja",
        indexes = {
                @Index(name = "idx_movimientos_caja_fecha", columnList = "fecha")
        }
)
public class MovimientoCaja {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TipoMovimientoCaja tipo;

    /** Siempre positivo; el sentido lo da "tipo". */
    @Positive
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal monto;

    /** Obligatorio: un movimiento de caja sin motivo no sirve para cuadrar nada. */
    @NotBlank
    @Size(max = 200)
    @Column(nullable = false, length = 200)
    private String motivo;

    @Size(max = 80)
    @Column(length = 80)
    private String responsable;

    @Column(nullable = false, updatable = false)
    private LocalDateTime fecha;

    protected MovimientoCaja() {
    }

    public MovimientoCaja(TipoMovimientoCaja tipo, BigDecimal monto, String motivo, String responsable) {
        this.tipo = tipo;
        this.monto = monto;
        this.motivo = motivo;
        this.responsable = responsable;
    }

    @PrePersist
    void alCrear() {
        this.fecha = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public TipoMovimientoCaja getTipo() {
        return tipo;
    }

    public BigDecimal getMonto() {
        return monto;
    }

    public String getMotivo() {
        return motivo;
    }

    public String getResponsable() {
        return responsable;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }
}
