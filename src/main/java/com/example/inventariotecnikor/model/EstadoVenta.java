package com.example.inventariotecnikor.model;

/**
 * Estado de una venta.
 *
 * De momento toda venta nace COMPLETADA. ANULADA queda reservada para
 * cuando se implemente la devolucion (reingreso de stock incluido).
 */
public enum EstadoVenta {

    COMPLETADA("Completada"),
    ANULADA("Anulada");

    private final String etiqueta;

    EstadoVenta(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }
}
