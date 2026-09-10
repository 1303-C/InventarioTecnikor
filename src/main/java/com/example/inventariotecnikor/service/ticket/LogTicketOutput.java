package com.example.inventariotecnikor.service.ticket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * "Impresora" de mentira: no manda nada, solo escribe en el log cuantos
 * bytes habria enviado. Es la que se usa con tecnikor.ticket.transporte=none,
 * para poder trabajar sin impresora conectada.
 */
public class LogTicketOutput implements TicketOutput {

    private static final Logger log = LoggerFactory.getLogger(LogTicketOutput.class);

    @Override
    public void enviar(byte[] datos) {
        log.info("Ticket NO impreso (transporte=none): {} bytes ESC/POS descartados.", datos.length);
    }

    @Override
    public String describir() {
        return "sin impresora (transporte=none)";
    }
}
