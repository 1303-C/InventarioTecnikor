package com.example.inventariotecnikor.web.betty;

import java.math.BigDecimal;

/**
 * Un item dentro del carrito de venta de Betty.
 * Descripcion libre + precio unitario + cantidad.
 */
public class ItemCarritoBetty {

    private final String descripcion;
    private final BigDecimal precioUnit;
    private final int cantidad;

    public ItemCarritoBetty(String descripcion, BigDecimal precioUnit, int cantidad) {
        this.descripcion = descripcion;
        this.precioUnit = precioUnit;
        this.cantidad = cantidad;
    }

    public String getDescripcion() { return descripcion; }
    public BigDecimal getPrecioUnit() { return precioUnit; }
    public int getCantidad() { return cantidad; }

    public BigDecimal getSubtotal() {
        return precioUnit.multiply(BigDecimal.valueOf(cantidad));
    }
}
