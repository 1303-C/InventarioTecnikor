package com.example.inventariotecnikor.service.ticket;

import com.example.inventariotecnikor.config.TicketProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Elige el canal de impresion segun tecnikor.ticket.transporte.
 *
 * El bean se crea al arrancar pero NO abre la conexion todavia: eso pasa en
 * cada ticket (enviar), asi que la app arranca aunque la impresora este
 * apagada o mal configurada.
 */
@Configuration
public class TicketOutputConfig {

    private static final Logger log = LoggerFactory.getLogger(TicketOutputConfig.class);

    @Bean
    public TicketOutput ticketOutput(TicketProperties props) {
        TicketOutput salida = switch (props.getTransporte()) {
            case TCP -> {
                if (props.getHost() == null || props.getHost().isBlank()) {
                    throw new IllegalStateException(
                            "tecnikor.ticket.transporte=tcp necesita tecnikor.ticket.host.");
                }
                yield new TcpTicketOutput(props.getHost(), props.getPuerto(), props.getTimeoutMs());
            }
            case RAW -> {
                if (props.getNombreImpresora() == null || props.getNombreImpresora().isBlank()) {
                    throw new IllegalStateException(
                            "tecnikor.ticket.transporte=raw necesita tecnikor.ticket.nombre-impresora.");
                }
                yield new RawPrinterOutput(props.getNombreImpresora());
            }
            case NONE -> new LogTicketOutput();
        };
        log.info("Impresora de tickets: {}", salida.describir());
        return salida;
    }
}
