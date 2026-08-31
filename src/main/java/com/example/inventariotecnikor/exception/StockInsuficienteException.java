package com.example.inventariotecnikor.exception;

/**
 * Se lanza al intentar registrar una SALIDA mayor que el stock disponible.
 * Impide que el stock quede negativo.
 */
public class StockInsuficienteException extends RuntimeException {

    public StockInsuficienteException(String mensaje) {
        super(mensaje);
    }
}
