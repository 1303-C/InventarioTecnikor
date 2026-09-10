package com.example.inventariotecnikor.web;

import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Parche para lectores de codigo mal configurados.
 *
 * Un lector USB de mano "teclea" la POSICION FISICA de cada tecla, no el
 * caracter. Si el lector esta en distribucion de teclado US y Windows en
 * "Espanol (Espana)", los simbolos salen cambiados de sitio. Mapeo real
 * observado con el lector de bodega:
 *
 *      se escanea  ->  llega a la app
 *          -               '
 *          /               -
 *          _               ?
 *
 * La solucion de fondo es configurar el lector (codigo de "Country /
 * Keyboard Layout" del manual) o poner Windows en ingles. Mientras tanto,
 * si un codigo escaneado NO aparece en la base, EscaneoController lo
 * reintenta pasandolo por este corrector, que deshace ese cambio de
 * distribucion.
 *
 * Solo se tocan simbolos: letras y numeros ocupan la misma tecla en ambas
 * distribuciones, asi que nunca se alteran.
 */
@Component
public class CorrectorTeclado {

    /**
     * Caracter que LLEGA (Windows lo interpreta con distribucion Espana)
     * -> caracter que se QUERIA teclear (distribucion US, que es lo que
     * suele venir impreso en la etiqueta). Cada par es la misma tecla
     * fisica leida con una distribucion y con la otra.
     */
    private static final Map<Character, Character> ESPANA_A_US = Map.ofEntries(
            Map.entry('\'', '-'),   // tecla a la derecha del 0
            Map.entry('?', '_'),    // ...esa misma tecla con Shift
            Map.entry('-', '/'),    // tecla a la derecha del punto
            Map.entry('_', '?'),    // ...esa misma tecla con Shift
            Map.entry('ç', '\\'), // c con cedilla -> backslash
            Map.entry('Ç', '|'),
            Map.entry('ñ', ';'),  // enie -> punto y coma
            Map.entry('Ñ', ':'),
            Map.entry('+', ']'),
            Map.entry('*', '}'),
            Map.entry('¡', '='),  // apertura de exclamacion -> igual
            Map.entry('¿', '+')   // apertura de interrogacion -> mas
    );

    /**
     * Devuelve el codigo reinterpretado como si se hubiera tecleado con
     * distribucion US. Si no habia ningun caracter que corregir devuelve
     * la MISMA cadena recibida (misma referencia), para que quien llama
     * pueda saltarse el segundo intento sin comparar.
     */
    public String comoUs(String codigo) {
        if (codigo == null || codigo.isEmpty()) {
            return codigo;
        }
        StringBuilder sb = new StringBuilder(codigo.length());
        boolean huboCambio = false;
        for (int i = 0; i < codigo.length(); i++) {
            char c = codigo.charAt(i);
            Character equivalente = ESPANA_A_US.get(c);
            if (equivalente != null) {
                sb.append(equivalente.charValue());
                huboCambio = true;
            } else {
                sb.append(c);
            }
        }
        return huboCambio ? sb.toString() : codigo;
    }
}
