package com.example.inventariotecnikor.config;

import org.springframework.stereotype.Component;

/**
 * Un numero fijado una sola vez al arrancar la app (el instante en que se
 * crea este bean). Se usa para "romper" el cache del navegador en los
 * recursos estaticos (el CSS): el link a styles.css le agrega
 * "?v=<esta marca>", asi que cada vez que se reinicia la app el enlace
 * cambia y el navegador esta OBLIGADO a pedir el archivo de nuevo en vez
 * de quedarse con una copia vieja guardada.
 */
@Component("arranque")
public class MarcaDeArranque {

    private final long marca = System.currentTimeMillis();

    public long getMarca() {
        return marca;
    }
}
