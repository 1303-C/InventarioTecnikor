package com.example.inventariotecnikor.model;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * getOrigen() trata una fila sin la columna "origen" poblada (las que ya
 * existian en la BD antes de este campo, porque SQLite no deja agregarlo
 * como NOT NULL con ALTER TABLE) como MANUAL.
 */
class MovimientoCajaTest {

    @Test
    void un_movimiento_con_origen_sin_poblar_se_trata_como_manual() throws ReflectiveOperationException {
        MovimientoCaja m = new MovimientoCaja(
                TipoMovimientoCaja.INGRESO, new BigDecimal("100.00"), "fila antigua", null, OrigenMovimientoCaja.ARQUEO);

        Field f = MovimientoCaja.class.getDeclaredField("origen");
        f.setAccessible(true);
        f.set(m, null); // simula una fila creada antes de que existiera la columna

        assertThat(m.getOrigen()).isEqualTo(OrigenMovimientoCaja.MANUAL);
    }

    @Test
    void un_movimiento_nuevo_conserva_el_origen_con_el_que_se_creo() {
        MovimientoCaja m = new MovimientoCaja(
                TipoMovimientoCaja.EGRESO, new BigDecimal("50.00"), "arqueo", null, OrigenMovimientoCaja.ARQUEO);

        assertThat(m.getOrigen()).isEqualTo(OrigenMovimientoCaja.ARQUEO);
    }
}
