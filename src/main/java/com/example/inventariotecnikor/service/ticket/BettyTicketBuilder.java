package com.example.inventariotecnikor.service.ticket;

import com.example.inventariotecnikor.config.BettyProperties;
import com.example.inventariotecnikor.config.TicketProperties;
import com.example.inventariotecnikor.model.LineaVentaBetty;
import com.example.inventariotecnikor.model.VentaBetty;
import com.github.anastaciocintra.escpos.EscPos;
import com.github.anastaciocintra.escpos.EscPos.CharacterCodeTable;
import com.github.anastaciocintra.escpos.EscPos.CutMode;
import com.github.anastaciocintra.escpos.EscPos.PinConnector;
import com.github.anastaciocintra.escpos.EscPosConst.Justification;
import com.github.anastaciocintra.escpos.Style;
import com.github.anastaciocintra.escpos.Style.FontSize;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Construye los bytes ESC/POS del ticket de Betty - Belleza y Cuidado.
 * Reutiliza la misma infraestructura (TicketOutput) de Tecnikor.
 */
@Component
public class BettyTicketBuilder {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final BettyProperties betty;
    private final TicketProperties ticket;

    public BettyTicketBuilder(BettyProperties betty, TicketProperties ticket) {
        this.betty = betty;
        this.ticket = ticket;
    }

    /**
     * @param venta       la venta ya guardada
     * @param abrirCajon  true solo si la forma de pago fue EFECTIVO
     */
    public byte[] construir(VentaBetty venta, boolean abrirCajon) throws IOException {
        int ancho = Math.max(24, ticket.getAnchoCaracteres());
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        try (EscPos escpos = new EscPos(buffer)) {
            escpos.initializePrinter();
            escpos.setCharacterCodeTable(CharacterCodeTable.WPC1252);

            Style centrado = new Style().setJustification(Justification.Center);
            Style titulo = new Style(centrado).setBold(true).setFontSize(FontSize._2, FontSize._2);
            Style negrita = new Style().setBold(true);

            // --- Encabezado ---
            escpos.writeLF(titulo, tiene(betty.getNombre()) ? betty.getNombre().trim() : "Betty");
            escpos.writeLF(centrado, tiene(betty.getEslogan()) ? betty.getEslogan().trim() : "Belleza y Cuidado");
            if (tiene(betty.getNit())) {
                escpos.writeLF(centrado, "NIT: " + betty.getNit().trim());
            }
            if (tiene(betty.getTelefono())) {
                escpos.writeLF(centrado, "Tel: " + betty.getTelefono().trim());
            }

            separador(escpos, ancho);
            escpos.writeLF("Ticket #" + venta.getNumero());
            escpos.writeLF(FECHA.format(venta.getFecha() != null ? venta.getFecha() : LocalDateTime.now()));
            separador(escpos, ancho);

            // --- Lineas ---
            for (LineaVentaBetty linea : venta.getLineas()) {
                escpos.writeLF(recortar(linea.getDescripcion(), ancho));
                String izquierda = "  " + linea.getCantidad() + " x " + dinero(linea.getPrecioUnit());
                escpos.writeLF(dosColumnas(izquierda, dinero(linea.getSubtotal()), ancho));
            }

            separador(escpos, ancho);

            // --- Total y forma de pago ---
            escpos.writeLF(negrita, dosColumnas("TOTAL", dinero(venta.getTotal()), ancho));
            escpos.writeLF(dosColumnas("Forma de pago", venta.getFormaPago(), ancho));

            escpos.feed(1);
            escpos.writeLF(centrado, "Gracias por su compra");
            escpos.writeLF(centrado, "Betty - Belleza y Cuidado");
            escpos.feed(6);

            if (ticket.isCortar()) {
                escpos.cut(CutMode.FULL);
            }
            if (abrirCajon) {
                escpos.pulsePin(PinConnector.Pin_2, 100, 200);
            }
            escpos.flush();
        }

        return buffer.toByteArray();
    }

    // ------------------------------------------------------------------

    private static void separador(EscPos escpos, int ancho) throws IOException {
        escpos.writeLF("-".repeat(ancho));
    }

    private static String dosColumnas(String izquierda, String derecha, int ancho) {
        String izq = recortar(izquierda, Math.max(1, ancho - derecha.length() - 1));
        int huecos = ancho - izq.length() - derecha.length();
        if (huecos < 1) huecos = 1;
        return izq + " ".repeat(huecos) + derecha;
    }

    private static String recortar(String texto, int max) {
        if (texto == null) return "";
        return texto.length() <= max ? texto : texto.substring(0, max);
    }

    private static String dinero(BigDecimal valor) {
        if (valor == null) return "$ 0";
        DecimalFormatSymbols simbolos = new DecimalFormatSymbols(Locale.US);
        simbolos.setGroupingSeparator('.');
        return "$ " + new DecimalFormat("#,##0", simbolos).format(valor);
    }

    private static boolean tiene(String s) {
        return s != null && !s.isBlank();
    }
}
