package com.example.inventariotecnikor.model;

/**
 * Tipo de electrodomestico al que pertenece el repuesto.
 *
 * En Java un enum es una clase completa: puede tener campos, constructor y
 * metodos. Aqui cada constante lleva una "etiqueta" legible para mostrar en
 * las vistas, sin tener que ensuciar el HTML con textos sueltos.
 */
public enum Categoria {

    NEVERA("Nevera / Refrigerador"),
    LAVADORA("Lavadora"),
    SECADORA("Secadora"),
    ESTUFA("Estufa"),
    OTRO("Otro");

    private final String etiqueta;

    Categoria(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }
}
