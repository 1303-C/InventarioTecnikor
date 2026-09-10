package com.example.inventariotecnikor.service.ticket;

import javax.print.Doc;
import javax.print.DocFlavor;
import javax.print.DocPrintJob;
import javax.print.PrintException;
import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import javax.print.SimpleDoc;
import javax.print.attribute.HashPrintRequestAttributeSet;
import java.io.IOException;

/**
 * Manda el ticket a una impresora instalada en Windows, buscandola por su
 * nombre y enviando los bytes ESC/POS "en crudo" (DocFlavor AUTOSENSE), sin
 * pasar por el driver grafico. Sirve cuando la POS esta conectada por USB.
 */
public class RawPrinterOutput implements TicketOutput {

    private final String nombreImpresora;

    public RawPrinterOutput(String nombreImpresora) {
        this.nombreImpresora = nombreImpresora;
    }

    @Override
    public void enviar(byte[] datos) throws IOException {
        PrintService servicio = buscarServicio();
        Doc doc = new SimpleDoc(datos, DocFlavor.BYTE_ARRAY.AUTOSENSE, null);
        DocPrintJob trabajo = servicio.createPrintJob();
        try {
            trabajo.print(doc, new HashPrintRequestAttributeSet());
        } catch (PrintException e) {
            throw new IOException("Fallo al enviar el ticket a la impresora \"" + nombreImpresora + "\".", e);
        }
    }

    private PrintService buscarServicio() throws IOException {
        for (PrintService s : PrintServiceLookup.lookupPrintServices(null, null)) {
            if (s.getName().equalsIgnoreCase(nombreImpresora)) {
                return s;
            }
        }
        throw new IOException("No hay ninguna impresora instalada con el nombre \"" + nombreImpresora + "\".");
    }

    @Override
    public String describir() {
        return "impresora Windows \"" + nombreImpresora + "\"";
    }
}
