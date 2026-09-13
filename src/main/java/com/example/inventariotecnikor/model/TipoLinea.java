package com.example.inventariotecnikor.model;

/**
 * Que es una linea de venta: un repuesto del inventario, un alquiler de
 * lavadora por horas, o un servicio de mantenimiento (mano de obra, precio
 * libre). Solo PRODUCTO lleva Producto detras y mueve stock; ALQUILER y
 * MANTENIMIENTO son "lineas libres": descripcion y precio escritos a mano
 * en el momento de cobrar.
 *
 * Sirve tambien para el desglose del cierre de caja por linea de negocio.
 */
public enum TipoLinea {

    PRODUCTO("Producto"),
    ALQUILER("Alquiler"),
    MANTENIMIENTO("Mantenimiento");

    private final String etiqueta;

    TipoLinea(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }
}
