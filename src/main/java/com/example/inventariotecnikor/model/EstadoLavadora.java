package com.example.inventariotecnikor.model;

/** Disponibilidad de una lavadora de alquiler. */
public enum EstadoLavadora {

    DISPONIBLE("Disponible"),
    PRESTADA("Prestada"),
    FUERA_DE_SERVICIO("Fuera de servicio");

    private final String etiqueta;

    EstadoLavadora(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }
}
