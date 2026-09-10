package com.example.inventariotecnikor.service.ticket;

import com.example.inventariotecnikor.exception.TicketException;
import com.example.inventariotecnikor.model.Venta;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Arma el ticket ESC/POS ({@link EscPosTicketBuilder}) y lo manda por el
 * canal configurado ({@link TicketOutput}).
 *
 * Si algo falla al entregarlo lanza {@link TicketException}: la venta ya
 * quedo guardada, esto solo avisa de que hay que reimprimir.
 */
@Component
public class EscPosTicketPrinter implements TicketPrinter {

    private static final Logger log = LoggerFactory.getLogger(EscPosTicketPrinter.class);

    private final EscPosTicketBuilder builder;
    private final TicketOutput salida;

    public EscPosTicketPrinter(EscPosTicketBuilder builder, TicketOutput salida) {
        this.builder = builder;
        this.salida = salida;
    }

    @Override
    public void imprimir(Venta venta) {
        try {
            byte[] datos = builder.construir(venta);
            salida.enviar(datos);
            log.info("Ticket de la venta #{} enviado a {} ({} bytes).",
                    venta.getNumero(), salida.describir(), datos.length);
        } catch (IOException e) {
            throw new TicketException(
                    "No se pudo imprimir el ticket de la venta #" + venta.getNumero()
                            + " (" + salida.describir() + "). Revisa la impresora y reimprime.", e);
        }
    }
}
