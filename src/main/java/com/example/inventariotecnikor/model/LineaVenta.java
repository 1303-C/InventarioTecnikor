package com.example.inventariotecnikor.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Una linea del ticket: un producto, cuantas unidades y a que precio.
 *
 * Los datos del producto (descripcion, codigo, precio) se COPIAN aqui al
 * vender. Asi el ticket historico no cambia aunque despues se renombre el
 * producto o se le suba el precio.
 *
 * IVA: por ahora el negocio no factura IVA, asi que porcentajeIva llega en 0
 * y valorIva queda en 0. Las columnas ya estan para cuando entre la factura
 * electronica DIAN, sin tener que migrar la tabla.
 */
@Entity
@Table(name = "lineas_venta")
public class LineaVenta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "venta_id", nullable = false)
    private Venta venta;

    /** Se conserva la referencia al producto para los reportes por producto. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    /** Copia del nombre del producto en el momento de la venta. */
    @Column(nullable = false, length = 150)
    private String descripcion;

    /** Copia del codigo QR en el momento de la venta. */
    @Column(name = "codigo_qr", length = 64)
    private String codigoQr;

    @Positive
    @Column(nullable = false)
    private int cantidad;

    /** Precio unitario aplicado (copia de producto.precioVenta al vender). */
    @Column(name = "precio_unitario", nullable = false, precision = 12, scale = 2)
    private BigDecimal precioUnitario;

    @Column(name = "porcentaje_iva", nullable = false)
    private int porcentajeIva;

    @Column(name = "valor_iva", nullable = false, precision = 12, scale = 2)
    private BigDecimal valorIva;

    /** Total de la linea: precioUnitario * cantidad + valorIva. */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal importe;

    protected LineaVenta() {
    }

    public LineaVenta(Producto producto, int cantidad, BigDecimal precioUnitario, int porcentajeIva) {
        if (cantidad <= 0) {
            throw new IllegalArgumentException("La cantidad de la linea debe ser mayor que cero.");
        }
        if (precioUnitario == null) {
            throw new IllegalArgumentException("La linea necesita un precio unitario.");
        }
        this.producto = producto;
        this.descripcion = producto.getNombre();
        this.codigoQr = producto.getCodigoQr();
        this.cantidad = cantidad;
        this.precioUnitario = precioUnitario.setScale(2, RoundingMode.HALF_UP);
        this.porcentajeIva = Math.max(0, porcentajeIva);

        BigDecimal base = this.precioUnitario.multiply(BigDecimal.valueOf(cantidad));
        this.valorIva = this.porcentajeIva == 0
                ? BigDecimal.ZERO.setScale(2)
                : base.multiply(BigDecimal.valueOf(this.porcentajeIva))
                      .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        this.importe = base.add(this.valorIva).setScale(2, RoundingMode.HALF_UP);
    }

    /** Base de la linea sin IVA (precioUnitario * cantidad). */
    public BigDecimal getBase() {
        return precioUnitario.multiply(BigDecimal.valueOf(cantidad)).setScale(2, RoundingMode.HALF_UP);
    }

    // Lo asigna Venta.addLinea(...) para mantener las dos puntas de la relacion.
    void setVenta(Venta venta) {
        this.venta = venta;
    }

    public Long getId() {
        return id;
    }

    public Venta getVenta() {
        return venta;
    }

    public Producto getProducto() {
        return producto;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public String getCodigoQr() {
        return codigoQr;
    }

    public int getCantidad() {
        return cantidad;
    }

    public BigDecimal getPrecioUnitario() {
        return precioUnitario;
    }

    public int getPorcentajeIva() {
        return porcentajeIva;
    }

    public BigDecimal getValorIva() {
        return valorIva;
    }

    public BigDecimal getImporte() {
        return importe;
    }
}
