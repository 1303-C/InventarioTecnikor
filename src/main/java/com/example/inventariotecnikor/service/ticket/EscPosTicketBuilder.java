package com.example.inventariotecnikor.service.ticket;

import com.example.inventariotecnikor.config.NegocioProperties;
import com.example.inventariotecnikor.config.TicketProperties;
import com.example.inventariotecnikor.model.LineaVenta;
import com.example.inventariotecnikor.model.Venta;
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
 * Convierte una {@link Venta} en los bytes ESC/POS del ticket de 80mm:
 * encabezado del negocio, lineas, total, forma de pago, y al final el corte
 * de papel y (si el pago fue en efectivo) el pulso para abrir el cajon.
 *
 * Es una clase "pura": recibe la venta y devuelve un byte[]. No abre la
 * impresora; de eso se encarga {@link EscPosTicketPrinter} con el
 * {@link TicketOutput} configurado. Asi se puede testear el contenido del
 * ticket sin hardware.
 */
@Component
public class EscPosTicketBuilder {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final NegocioProperties negocio;
    private final TicketProperties ticket;

    public EscPosTicketBuilder(NegocioProperties negocio, TicketProperties ticket) {
        this.negocio = negocio;
        this.ticket = ticket;
    }

    public byte[] construir(Venta venta) throws IOException {
        int ancho = Math.max(24, ticket.getAnchoCaracteres());
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        try (EscPos escpos = new EscPos(buffer)) {
            escpos.initializePrinter();
            escpos.setCharacterCodeTable(CharacterCodeTable.WPC1252);

            Style centrado = new Style().setJustification(Justification.Center);
            Style titulo = new Style(centrado).setBold(true).setFontSize(FontSize._2, FontSize._2);
            Style negrita = new Style().setBold(true);

            // --- Encabezado ---
            escpos.writeLF(titulo, textoOSimbolo(negocio.getNombre(), "TECNIKOR"));
            if (tiene(negocio.getDireccion())) {
                escpos.writeLF(centrado, negocio.getDireccion());
            }
            if (tiene(negocio.getTelefono())) {
                escpos.writeLF(centrado, "Tel: " + negocio.getTelefono().trim());
            }
            if (tiene(negocio.getNit())) {
                escpos.writeLF(centrado, "NIT: " + negocio.getNit().trim());
            }

            separador(escpos, ancho);
            escpos.writeLF("Ticket #" + venta.getNumero());
            escpos.writeLF(FECHA.format(venta.getFecha() != null ? venta.getFecha() : LocalDateTime.now()));
            if (tiene(venta.getResponsable())) {
                escpos.writeLF("Atendio: " + venta.getResponsable().trim());
            }
            if (tiene(venta.getClienteNombre())) {
                escpos.writeLF("Cliente: " + venta.getClienteNombre().trim());
            }
            separador(escpos, ancho);

            // --- Lineas ---
            for (LineaVenta linea : venta.getLineas()) {
                escpos.writeLF(recortar(linea.getDescripcion(), ancho));
                String izquierda = "  " + linea.getCantidad() + " x " + dinero(linea.getPrecioUnitario());
                escpos.writeLF(dosColumnas(izquierda, dinero(linea.getImporte()), ancho));
            }

            separador(escpos, ancho);

            // --- Totales y pago ---
            escpos.writeLF(negrita, dosColumnas("TOTAL", dinero(venta.getTotal()), ancho));
            escpos.writeLF(dosColumnas("Forma de pago", venta.getFormaPago().getEtiqueta(), ancho));
            if (venta.getFormaPago().esEfectivo()) {
                escpos.writeLF(dosColumnas("Recibido", dinero(venta.getMontoRecibido()), ancho));
                escpos.writeLF(dosColumnas("Cambio", dinero(venta.getCambio()), ancho));
            }

            escpos.feed(1);
            escpos.writeLF(centrado, "Gracias por su compra");
            escpos.writeLF(centrado, "Vuelva pronto");
            // La cuchilla necesita un margen minimo de papel en blanco antes
            // de cortar; con menos, corta pegado a "Vuelva pronto".
            escpos.feed(6);

            if (ticket.isCortar()) {
                escpos.cut(CutMode.FULL);
            }
            if (ticket.isAbrirCajonEnEfectivo() && venta.getFormaPago().esEfectivo()) {
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

    /** Deja "izquierda" a la izquierda y "derecha" pegada al margen derecho. */
    static String dosColumnas(String izquierda, String derecha, int ancho) {
        String izq = recortar(izquierda, Math.max(1, ancho - derecha.length() - 1));
        int huecos = ancho - izq.length() - derecha.length();
        if (huecos < 1) {
            huecos = 1;
        }
        return izq + " ".repeat(huecos) + derecha;
    }

    static String recortar(String texto, int max) {
        if (texto == null) {
            return "";
        }
        return texto.length() <= max ? texto : texto.substring(0, max);
    }

    /** "$ 1.234.567" (miles con punto, sin decimales), como en las etiquetas. */
    static String dinero(BigDecimal valor) {
        if (valor == null) {
            return "$ 0";
        }
        DecimalFormatSymbols simbolos = new DecimalFormatSymbols(Locale.US);
        simbolos.setGroupingSeparator('.');
        return "$ " + new DecimalFormat("#,##0", simbolos).format(valor);
    }

    private static boolean tiene(String s) {
        return s != null && !s.isBlank();
    }

    private static String textoOSimbolo(String s, String pordefecto) {
        return tiene(s) ? s.trim() : pordefecto;
    }
}
