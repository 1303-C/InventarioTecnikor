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
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Un repuesto de linea blanca en el inventario del taller.
 *
 * Cada producto tiene un codigo QR unico: es lo que se escanea para
 * encontrarlo o para registrar una entrada/salida.
 *
 * Notas para alguien que viene de EF Core / SQL Server:
 *  - @Entity + @Table = esto es una tabla. El nombre de tabla y de columnas
 *    los ponemos explicitos (snake_case) para no depender de la config por
 *    defecto de Hibernate.
 *  - @Id + @GeneratedValue(IDENTITY): equivale a una columna IDENTITY.
 *    En SQLite se traduce a "INTEGER PRIMARY KEY AUTOINCREMENT".
 *  - Las anotaciones jakarta.validation.constraints (@NotBlank, @Size...)
 *    NO tocan la base de datos; sirven para validar formularios mas
 *    adelante. Las dejo puestas desde ya porque describen bien las reglas.
 */
@Entity
@Table(
        name = "productos",
        // El indice UNICO de codigo_qr lo crea ya @Column(unique = true) mas
        // abajo; aqui solo declaramos un indice normal para filtrar por categoria.
        indexes = {
                @Index(name = "idx_productos_categoria", columnList = "categoria")
        }
)
public class Producto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Contenido del codigo QR pegado en la caja/estante. Identificador de negocio. */
    @NotBlank
    @Size(max = 64)
    @Column(name = "codigo_qr", nullable = false, unique = true, length = 64)
    private String codigoQr;

    @NotBlank
    @Size(max = 150)
    @Column(nullable = false, length = 150)
    private String nombre;

    @Size(max = 500)
    @Column(length = 500)
    private String descripcion;

    /** Numero de parte del fabricante (p. ej. "DA97-12345A"). */
    @Size(max = 80)
    @Column(name = "numero_parte", length = 80)
    private String numeroParte;

    @NotNull
    @Enumerated(EnumType.STRING) // guarda "NEVERA" como texto, no el ordinal 0/1
    @Column(nullable = false, length = 20)
    private Categoria categoria;

    @Size(max = 80)
    @Column(length = 80)
    private String marca;

    /** Ubicacion fisica en la bodega (pasillo/estante/bin). */
    @Size(max = 50)
    @Column(length = 50)
    private String ubicacion;

    @PositiveOrZero
    @Column(name = "stock_actual", nullable = false)
    private int stockActual;

    /** Umbral para el dashboard: si stockActual <= stockMinimo, es "stock bajo". */
    @PositiveOrZero
    @Column(name = "stock_minimo", nullable = false)
    private int stockMinimo;

    /** BigDecimal (no double) para dinero, igual que decimal en SQL Server. */
    @Column(name = "precio_venta", precision = 12, scale = 2)
    private BigDecimal precioVenta;

    /** Baja logica: en vez de borrar la fila, se marca inactivo. */
    @Column(nullable = false)
    private boolean activo = true;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_actualizacion", nullable = false)
    private LocalDateTime fechaActualizacion;

    // JPA exige un constructor sin argumentos (puede ser protected).
    protected Producto() {
    }

    public Producto(String codigoQr, String nombre, Categoria categoria, int stockMinimo) {
        this.codigoQr = codigoQr;
        this.nombre = nombre;
        this.categoria = categoria;
        this.stockMinimo = stockMinimo;
    }

    // --- Callbacks de ciclo de vida: rellenan las fechas automaticamente ---

    @PrePersist
    void alCrear() {
        LocalDateTime ahora = LocalDateTime.now();
        this.fechaCreacion = ahora;
        this.fechaActualizacion = ahora;
    }

    @PreUpdate
    void alActualizar() {
        this.fechaActualizacion = LocalDateTime.now();
    }

    // --- Logica de dominio ---

    /** Regla usada por el dashboard de stock bajo. */
    public boolean estaBajoDeStock() {
        return stockActual <= stockMinimo;
    }

    // --- Getters / setters ---

    public Long getId() {
        return id;
    }

    public String getCodigoQr() {
        return codigoQr;
    }

    public void setCodigoQr(String codigoQr) {
        this.codigoQr = codigoQr;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getNumeroParte() {
        return numeroParte;
    }

    public void setNumeroParte(String numeroParte) {
        this.numeroParte = numeroParte;
    }

    public Categoria getCategoria() {
        return categoria;
    }

    public void setCategoria(Categoria categoria) {
        this.categoria = categoria;
    }

    public String getMarca() {
        return marca;
    }

    public void setMarca(String marca) {
        this.marca = marca;
    }

    public String getUbicacion() {
        return ubicacion;
    }

    public void setUbicacion(String ubicacion) {
        this.ubicacion = ubicacion;
    }

    public int getStockActual() {
        return stockActual;
    }

    public void setStockActual(int stockActual) {
        this.stockActual = stockActual;
    }

    public int getStockMinimo() {
        return stockMinimo;
    }

    public void setStockMinimo(int stockMinimo) {
        this.stockMinimo = stockMinimo;
    }

    public BigDecimal getPrecioVenta() {
        return precioVenta;
    }

    public void setPrecioVenta(BigDecimal precioVenta) {
        this.precioVenta = precioVenta;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public LocalDateTime getFechaActualizacion() {
        return fechaActualizacion;
    }

    // Igualdad basada en la identidad de negocio (codigoQr), no en el id
    // autogenerado: asi dos referencias al mismo producto son "iguales"
    // aunque una aun no tenga id asignado.
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Producto other)) {
            return false;
        }
        return codigoQr != null && codigoQr.equals(other.codigoQr);
    }

    @Override
    public int hashCode() {
        return Objects.hash(codigoQr);
    }
}
