package com.example.inventariotecnikor.service.ticket;

import java.io.IOException;

/**
 * Canal por el que se le mandan los bytes ya construidos a la impresora.
 *
 * Hay tres implementaciones (una activa segun tecnikor.ticket.transporte):
 *  - {@link LogTicketOutput}  : no imprime, solo registra en el log.
 *  - {@link TcpTicketOutput}  : por red (host:puerto).
 *  - {@link RawPrinterOutput} : impresora instalada en Windows, por nombre.
 */
public interface TicketOutput {

    /** Manda el ticket completo. Lanza IOException si no se pudo entregar. */
    void enviar(byte[] datos) throws IOException;

    /** Descripcion corta para los logs ("red 192.168.1.50:9100", etc.). */
    String describir();
}
