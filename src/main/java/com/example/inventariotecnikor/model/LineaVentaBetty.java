package com.example.inventariotecnikor.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "lineas_venta_betty")
public class LineaVentaBetty {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String descripcion;

    @Column(precision = 12, scale = 2)
    private BigDecimal precioUnit;

    private Integer cantidad;

    @Column(precision = 12, scale = 2)
    private BigDecimal subtotal;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "venta_id")
    private VentaBetty venta;

    public LineaVentaBetty() {}

    public LineaVentaBetty(String descripcion, BigDecimal precioUnit, int cantidad) {
        this.descripcion = descripcion;
        this.precioUnit = precioUnit;
        this.cantidad = cantidad;
        this.subtotal = precioUnit.multiply(BigDecimal.valueOf(cantidad));
    }

    public Long getId() { return id; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public BigDecimal getPrecioUnit() { return precioUnit; }
    public void setPrecioUnit(BigDecimal precioUnit) { this.precioUnit = precioUnit; }

    public Integer getCantidad() { return cantidad; }
    public void setCantidad(Integer cantidad) { this.cantidad = cantidad; }

    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }

    public VentaBetty getVenta() { return venta; }
    public void setVenta(VentaBetty venta) { this.venta = venta; }
}
