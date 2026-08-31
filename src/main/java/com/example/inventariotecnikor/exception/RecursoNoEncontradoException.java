package com.example.inventariotecnikor.exception;

/**
 * Se lanza cuando se pide un producto (o cualquier recurso) que no existe:
 * un id que no esta en la BD, un codigo QR que nadie registro, etc.
 *
 * Hereda de RuntimeException (no "checked") por dos motivos:
 *  - No obliga a poner "throws ..." en toda la cadena de llamadas.
 *  - Spring hace rollback automatico de la transaccion ante una
 *    RuntimeException; ante una excepcion "checked" NO lo haria por defecto.
 */
public class RecursoNoEncontradoException extends RuntimeException {

    public RecursoNoEncontradoException(String mensaje) {
        super(mensaje);
    }
}
