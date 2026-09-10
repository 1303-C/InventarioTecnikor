package com.example.inventariotecnikor.service.ticket;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * Manda el ticket por red a una impresora POS que escucha en un puerto TCP
 * (casi siempre el 9100, "raw printing"). Abre la conexion, escribe los
 * bytes y la cierra en cada ticket: sencillo y suficiente para el volumen
 * de un mostrador.
 */
public class TcpTicketOutput implements TicketOutput {

    private final String host;
    private final int puerto;
    private final int timeoutMs;

    public TcpTicketOutput(String host, int puerto, int timeoutMs) {
        this.host = host;
        this.puerto = puerto;
        this.timeoutMs = timeoutMs;
    }

    @Override
    public void enviar(byte[] datos) throws IOException {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, puerto), timeoutMs);
            socket.setSoTimeout(timeoutMs);
            OutputStream salida = socket.getOutputStream();
            salida.write(datos);
            salida.flush();
        }
    }

    @Override
    public String describir() {
        return "red " + host + ":" + puerto;
    }
}
