package com.example.inventariotecnikor.model;

/**
 * Como pago el cliente una venta.
 *
 * Solo EFECTIVO mueve el cajon monedero y necesita "monto recibido" para
 * calcular el cambio; TARJETA y TRANSFERENCIA se cobran por el importe exacto.
 */
public enum FormaPago {

    EFECTIVO("Efectivo"),
    TARJETA("Tarjeta"),
    TRANSFERENCIA("Transferencia");

    private final String etiqueta;

    FormaPago(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    /** El pago en efectivo es el unico que pide monto recibido y da cambio. */
    public boolean esEfectivo() {
        return this == EFECTIVO;
    }
}
