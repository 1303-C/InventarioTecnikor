package com.example.inventariotecnikor.service.ticket;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * TcpTicketOutput contra un ServerSocket local: comprueba que los bytes
 * llegan tal cual y que una conexion rechazada se traduce en IOException.
 */
class TcpTicketOutputTest {

    @Test
    void entrega_los_bytes_por_el_socket() throws Exception {
        try (ServerSocket servidor = new ServerSocket(0)) {
            int puerto = servidor.getLocalPort();
            byte[] payload = "TICKET ESC/POS".getBytes(StandardCharsets.UTF_8);

            CompletableFuture<byte[]> recibido = new CompletableFuture<>();
            Thread aceptador = new Thread(() -> {
                try (Socket s = servidor.accept()) {
                    recibido.complete(s.getInputStream().readAllBytes());
                } catch (IOException e) {
                    recibido.completeExceptionally(e);
                }
            });
            aceptador.setDaemon(true);
            aceptador.start();

            new TcpTicketOutput("127.0.0.1", puerto, 2000).enviar(payload);

            assertThat(recibido.get(2, TimeUnit.SECONDS)).isEqualTo(payload);
        }
    }

    @Test
    void si_no_hay_nadie_escuchando_lanza_IOException() throws Exception {
        int puertoLibre;
        try (ServerSocket temporal = new ServerSocket(0)) {
            puertoLibre = temporal.getLocalPort();
        } // se cierra aqui: el puerto queda sin escuchar

        assertThatThrownBy(() ->
                new TcpTicketOutput("127.0.0.1", puertoLibre, 300).enviar(new byte[]{1, 2, 3}))
                .isInstanceOf(IOException.class);
    }
}
