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
import jakarta.validation.constraints.Positive;

import java.time.LocalDateTime;

/**
 * Registro historico de cada entrada o salida de stock de un producto.
 *
 * Es un "libro mayor": nunca se edita ni se borra una fila; solo se agregan.
 * El stock actual del producto se puede reconstruir sumando todos sus
 * movimientos, pero ademas guardamos aqui "stockResultante" como foto del
 * saldo justo despues de este movimiento (comodo para auditar y para
 * mostrar el historial sin recalcular).
 *
 * La relacion con Producto es @ManyToOne: muchos movimientos -> un producto.
 * En la BD se traduce a una columna "producto_id" con clave foranea.
 */
@Entity
@Table(
        name = "movimientos_inventario",
        indexes = {
                @Index(name = "idx_movimientos_producto", columnList = "producto_id"),
                @Index(name = "idx_movimientos_fecha", columnList = "fecha")
        }
)
public class MovimientoInventario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // LAZY = no traer el producto de la BD hasta que alguien lo pida.
    // optional=false = todo movimiento pertenece si o si a un producto.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    // OJO: Hibernate genera un CHECK (tipo in ('ENTRADA','SALIDA','AJUSTE'))
    // con la lista de constantes actual. En una BD nueva se crea bien, pero
    // ddl-auto=update NO actualiza un CHECK ya existente: si anades un valor
    // nuevo al enum, hay que migrar a mano la BD que ya estaba creada
    // (reconstruir la tabla). Ver docs/migraciones.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TipoMovimiento tipo;

    /** Unidades movidas. Siempre positivo; el sentido lo da "tipo". */
    @Positive
    @Column(nullable = false)
    private int cantidad;

    /** Saldo del producto inmediatamente despues de aplicar este movimiento. */
    @Column(name = "stock_resultante", nullable = false)
    private int stockResultante;

    /** Texto libre: "compra proveedor X", "venta factura 123", "ajuste conteo"... */
    @Column(length = 200)
    private String motivo;

    /** Quien lo registro (nombre del tecnico / usuario). */
    @Column(length = 80)
    private String responsable;

    @Column(nullable = false, updatable = false)
    private LocalDateTime fecha;

    protected MovimientoInventario() {
    }

    public MovimientoInventario(Producto producto,
                                TipoMovimiento tipo,
                                int cantidad,
                                int stockResultante,
                                String motivo,
                                String responsable) {
        this.producto = producto;
        this.tipo = tipo;
        this.cantidad = cantidad;
        this.stockResultante = stockResultante;
        this.motivo = motivo;
        this.responsable = responsable;
    }

    @PrePersist
    void alCrear() {
        this.fecha = LocalDateTime.now();
    }

    // --- Getters (sin setters: un movimiento es inmutable una vez creado) ---

    public Long getId() {
        return id;
    }

    public Producto getProducto() {
        return producto;
    }

    public TipoMovimiento getTipo() {
        return tipo;
    }

    public int getCantidad() {
        return cantidad;
    }

    public int getStockResultante() {
        return stockResultante;
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
