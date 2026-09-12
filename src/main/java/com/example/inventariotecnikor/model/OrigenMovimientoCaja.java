package com.example.inventariotecnikor.model;

/**
 * De donde sale un {@link MovimientoCaja}: si lo escribio a mano el cajero
 * (MANUAL) o si lo genero solo un arqueo al registrar la diferencia entre
 * lo contado y lo esperado (ARQUEO). Sirve para distinguirlo en pantalla y
 * para poder sumar "cuanto genero el arqueo" sin tener que adivinarlo
 * leyendo el motivo.
 *
 * OJO: la columna es NULLABLE a proposito. SQLite no deja agregar con
 * ALTER TABLE una columna NOT NULL sin un valor por defecto, y esta
 * columna se agrega sobre una tabla movimientos_caja que ya pudo haber
 * sido creada (ver docs/migraciones). Un valor null en filas antiguas se
 * trata igual que MANUAL.
 */
public enum OrigenMovimientoCaja {

    MANUAL("Manual"),
    ARQUEO("Arqueo");

    private final String etiqueta;

    OrigenMovimientoCaja(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }
}
