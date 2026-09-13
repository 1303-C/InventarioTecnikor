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
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Una linea del ticket.
 *
 * La mayoria son de tipo PRODUCTO: un repuesto, cuantas unidades y a que
 * precio, con los datos del producto COPIADOS aqui al vender (asi el ticket
 * historico no cambia aunque despues se renombre el producto o se le suba
 * el precio).
 *
 * ALQUILER y MANTENIMIENTO son lineas "libres": no hay Producto detras (el
 * alquiler de una lavadora por horas, la mano de obra de una revision), asi
 * que "producto" y "codigoQr" quedan en null y la descripcion/precio se
 * escriben a mano en el momento de cobrar. No mueven stock.
 *
 * OJO: "producto_id" es nullable a proposito desde la migracion de
 * 2026-09-12 (ver db/migraciones): en una BD creada antes de las lineas
 * libres, esa columna era NOT NULL y hubo que reconstruir la tabla. "tipo"
 * tambien es nullable por lo mismo que "origen" en MovimientoCaja: se
 * agrega con ALTER TABLE sobre una tabla que ya existia, y SQLite no deja
 * columnas NOT NULL sin default ahi. Una fila vieja sin "tipo" se trata
 * como PRODUCTO (getTipo()).
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

    @Enumerated(EnumType.STRING)
    @Column(length = 15)
    private TipoLinea tipo;

    /** Solo en lineas de tipo PRODUCTO. Se conserva para los reportes por producto. */
    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "producto_id")
    private Producto producto;

    /** Copia del nombre del producto (o el texto libre de alquiler/servicio). */
    @Column(nullable = false, length = 150)
    private String descripcion;

    /** Copia del codigo QR. Null en lineas libres (alquiler/mantenimiento). */
    @Column(name = "codigo_qr", length = 64)
    private String codigoQr;

    @Positive
    @Column(nullable = false)
    private int cantidad;

    /** Precio unitario aplicado (copia de producto.precioVenta, o el monto libre). */
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

    /** Linea de un repuesto del inventario: baja stock, lleva IVA (hoy siempre 0). */
    public LineaVenta(Producto producto, int cantidad, BigDecimal precioUnitario, int porcentajeIva) {
        this(TipoLinea.PRODUCTO, producto, producto.getNombre(), producto.getCodigoQr(),
                cantidad, precioUnitario, porcentajeIva);
    }

    /**
     * Linea libre: ALQUILER (horas de una lavadora) o MANTENIMIENTO (mano de
     * obra a precio libre). Sin producto, sin IVA, no mueve stock.
     *
     * @param cantidad para ALQUILER son las horas; para MANTENIMIENTO
     *                 normalmente 1 (puede haber varias lineas, ej.
     *                 "Visita/diagnostico" + "Mano de obra").
     */
    public LineaVenta(TipoLinea tipo, String descripcion, int cantidad, BigDecimal precioUnitario) {
        this(exigirTipoLibre(tipo), null, descripcion, null, cantidad, precioUnitario, 0);
    }

    private LineaVenta(TipoLinea tipo, Producto producto, String descripcion, String codigoQr,
                       int cantidad, BigDecimal precioUnitario, int porcentajeIva) {
        if (cantidad <= 0) {
            throw new IllegalArgumentException("La cantidad de la linea debe ser mayor que cero.");
        }
        if (precioUnitario == null || precioUnitario.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("La linea necesita un precio unitario mayor que cero.");
        }
        if (descripcion == null || descripcion.isBlank()) {
            throw new IllegalArgumentException("La linea necesita una descripcion.");
        }
        this.tipo = tipo;
        this.producto = producto;
        this.descripcion = descripcion;
        this.codigoQr = codigoQr;
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

    private static TipoLinea exigirTipoLibre(TipoLinea tipo) {
        if (tipo == null || tipo == TipoLinea.PRODUCTO) {
            throw new IllegalArgumentException(
                    "Una linea libre debe ser ALQUILER o MANTENIMIENTO, no " + tipo + ".");
        }
        return tipo;
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

    /** Filas antiguas sin la columna poblada cuentan como PRODUCTO. */
    public TipoLinea getTipo() {
        return tipo != null ? tipo : TipoLinea.PRODUCTO;
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
