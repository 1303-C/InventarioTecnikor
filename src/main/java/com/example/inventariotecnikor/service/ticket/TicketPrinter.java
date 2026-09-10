package com.example.inventariotecnikor.service.ticket;

import com.example.inventariotecnikor.model.Venta;

/**
 * Imprime el ticket de una venta. La implementacion actual
 * ({@link EscPosTicketPrinter}) arma ESC/POS y lo manda por el
 * {@link TicketOutput} configurado.
 */
public interface TicketPrinter {

    /**
     * @throws com.example.inventariotecnikor.exception.TicketException si el
     *         papel no pudo salir (impresora apagada, sin papel, sin red...).
     *         La venta ya esta guardada: esto solo indica que hay que
     *         reimprimir.
     */
    void imprimir(Venta venta);
}
