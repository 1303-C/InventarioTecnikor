package com.example.inventariotecnikor.exception;

/**
 * Falla al imprimir un ticket (impresora apagada, sin papel, sin red...).
 *
 * La venta ya quedo guardada cuando esto se lanza: no revierte nada, solo
 * avisa de que el papel no salio y hay que reimprimir.
 */
public class TicketException extends RuntimeException {

    public TicketException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
