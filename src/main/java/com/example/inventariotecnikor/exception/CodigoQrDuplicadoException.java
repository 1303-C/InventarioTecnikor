package com.example.inventariotecnikor.exception;

/**
 * Se lanza al dar de alta un producto con un codigo QR que ya existe.
 * El codigo QR es unico: es el identificador que se escanea.
 */
public class CodigoQrDuplicadoException extends RuntimeException {

    public CodigoQrDuplicadoException(String mensaje) {
        super(mensaje);
    }
}
