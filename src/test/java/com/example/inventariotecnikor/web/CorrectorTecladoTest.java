package com.example.inventariotecnikor.web;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Comprueba que el corrector deshace el desajuste de distribucion de
 * teclado del lector (US impreso en la etiqueta, Windows en "Espanol").
 */
class CorrectorTecladoTest {

    private final CorrectorTeclado corrector = new CorrectorTeclado();

    @Test
    void deshace_el_guion_que_llega_como_apostrofe() {
        // La etiqueta dice PROD-001; el lector mal configurado entrega PROD'001
        assertThat(corrector.comoUs("PROD'001")).isEqualTo("PROD-001");
    }

    @Test
    void deshace_la_barra_que_llega_como_guion() {
        // La etiqueta dice A/B; el lector entrega A-B
        assertThat(corrector.comoUs("A-B")).isEqualTo("A/B");
    }

    @Test
    void corrige_varios_guiones_en_el_mismo_codigo() {
        // Etiqueta REP-2024-07 -> el lector entrega REP'2024'07
        assertThat(corrector.comoUs("REP'2024'07")).isEqualTo("REP-2024-07");
    }

    @Test
    void deja_igual_lo_que_no_tiene_simbolos_cambiados() {
        String limpio = "ABC123";
        assertThat(corrector.comoUs(limpio)).isSameAs(limpio);
    }

    @Test
    void tolera_null_y_cadena_vacia() {
        assertThat(corrector.comoUs(null)).isNull();
        assertThat(corrector.comoUs("")).isEmpty();
    }
}
