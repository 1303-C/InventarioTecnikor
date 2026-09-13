package com.example.inventariotecnikor.model;

/** Estado de un prestamo de lavadora. */
public enum EstadoAlquiler {

    ACTIVO("Activo"),
    DEVUELTO("Devuelto");

    private final String etiqueta;

    EstadoAlquiler(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }
}
