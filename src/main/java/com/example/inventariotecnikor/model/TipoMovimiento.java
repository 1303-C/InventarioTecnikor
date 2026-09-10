package com.example.inventariotecnikor.model;

/**
 * Sentido de un movimiento de inventario.
 *
 * Guardamos el "signo" (+1 / -1) en el propio enum para que la logica de
 * negocio pueda calcular el nuevo stock sin un if/switch:
 *
 *     nuevoStock = stockActual + tipo.getSigno() * cantidad;
 *
 * AJUSTE es distinto: no suma ni resta una cantidad, sino que FIJA el stock
 * al numero contado fisicamente. Por eso su signo es 0 y no pasa por
 * MovimientoService.registrar(...), sino por ajustarA(...).
 */
public enum TipoMovimiento {

    ENTRADA(1, "Entrada"),
    SALIDA(-1, "Salida"),
    AJUSTE(0, "Ajuste");

    private final int signo;
    private final String etiqueta;

    TipoMovimiento(int signo, String etiqueta) {
        this.signo = signo;
        this.etiqueta = etiqueta;
    }

    public int getSigno() {
        return signo;
    }

    public String getEtiqueta() {
        return etiqueta;
    }
}
