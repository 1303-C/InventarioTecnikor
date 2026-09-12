package com.example.inventariotecnikor.model;

/**
 * Sentido de un movimiento de caja (dinero en efectivo que entra o sale del
 * cajon SIN ser una venta): pago a un proveedor, un gasto, un abono, la base
 * con la que se abre el dia...
 *
 * Igual que {@link TipoMovimiento} con el stock, el signo permite sumar sin
 * un if/switch al calcular el efectivo esperado en el cajon.
 */
public enum TipoMovimientoCaja {

    INGRESO(1, "Ingreso"),
    EGRESO(-1, "Egreso");

    private final int signo;
    private final String etiqueta;

    TipoMovimientoCaja(int signo, String etiqueta) {
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
