package com.example.inventariotecnikor.service.ticket;

import com.example.inventariotecnikor.config.NegocioProperties;
import com.example.inventariotecnikor.config.TicketProperties;
import com.example.inventariotecnikor.model.Categoria;
import com.example.inventariotecnikor.model.FormaPago;
import com.example.inventariotecnikor.model.LineaVenta;
import com.example.inventariotecnikor.model.Producto;
import com.example.inventariotecnikor.model.Venta;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.charset.Charset;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Comprueba el CONTENIDO del ticket ESC/POS sin impresora: se construye el
 * byte[] y se mira que lleve el texto esperado y los comandos de corte de
 * papel (GS V = 1D 56) y de apertura de cajon (ESC p = 1B 70).
 */
class EscPosTicketBuilderTest {

    private static final Charset CP1252 = Charset.forName("windows-1252");
    private static final byte[] CMD_CORTE = {0x1D, 0x56};
    private static final byte[] CMD_CAJON = {0x1B, 0x70};

    private NegocioProperties negocio;
    private TicketProperties ticket;

    @BeforeEach
    void setUp() {
        negocio = new NegocioProperties();
        negocio.setNombre("Tecnikor");
        negocio.setDireccion("Calle 13 #20-64, Centro, Cumaral, Meta");
        negocio.setTelefono("3117049613");
        negocio.setNit("");

        ticket = new TicketProperties(); // defaults: cortar=true, abrirCajonEnEfectivo=true, ancho=42
    }

    private Producto producto(String codigo, String nombre, String precio) {
        Producto p = new Producto(codigo, nombre, Categoria.LAVADORA, 1);
        p.setPrecioVenta(new BigDecimal(precio));
        return p;
    }

    private Venta ventaEfectivo() {
        Venta v = new Venta(7, FormaPago.EFECTIVO, "Carlos");
        v.addLinea(new LineaVenta(producto("LAV-001", "Bomba de agua", "100.00"), 2,
                new BigDecimal("100.00"), 0));
        v.addLinea(new LineaVenta(producto("LAV-002", "Correa", "25.50"), 1,
                new BigDecimal("25.50"), 0));
        v.recalcularTotales();
        v.registrarPago(FormaPago.EFECTIVO, new BigDecimal("300.00"));
        return v;
    }

    private Venta ventaTarjeta() {
        Venta v = new Venta(8, FormaPago.TARJETA, "Ana");
        v.addLinea(new LineaVenta(producto("LAV-001", "Bomba de agua", "100.00"), 1,
                new BigDecimal("100.00"), 0));
        v.recalcularTotales();
        v.registrarPago(FormaPago.TARJETA, null);
        return v;
    }

    private static boolean contiene(byte[] datos, byte[] secuencia) {
        outer:
        for (int i = 0; i <= datos.length - secuencia.length; i++) {
            for (int j = 0; j < secuencia.length; j++) {
                if (datos[i + j] != secuencia[j]) {
                    continue outer;
                }
            }
            return true;
        }
        return false;
    }

    @Test
    void el_ticket_lleva_encabezado_lineas_y_totales() throws Exception {
        String txt = new String(new EscPosTicketBuilder(negocio, ticket).construir(ventaEfectivo()), CP1252);

        assertThat(txt).contains("Tecnikor");
        assertThat(txt).contains("Calle 13 #20-64, Centro, Cumaral, Meta");
        assertThat(txt).contains("Tel: 3117049613");
        assertThat(txt).contains("Ticket #7");
        assertThat(txt).contains("Atendio: Carlos");
        assertThat(txt).contains("Bomba de agua");
        assertThat(txt).contains("Correa");
        assertThat(txt).contains("TOTAL");
        assertThat(txt).contains("Gracias por su compra");
        assertThat(txt).contains("Vuelva pronto");
    }

    @Test
    void sin_nit_no_pinta_la_linea_de_nit() throws Exception {
        String txt = new String(new EscPosTicketBuilder(negocio, ticket).construir(ventaEfectivo()), CP1252);
        assertThat(txt).doesNotContain("NIT:");
    }

    @Test
    void en_efectivo_pinta_recibido_y_cambio_y_manda_el_pulso_del_cajon() throws Exception {
        byte[] datos = new EscPosTicketBuilder(negocio, ticket).construir(ventaEfectivo());
        String txt = new String(datos, CP1252);

        assertThat(txt).contains("Recibido");
        assertThat(txt).contains("Cambio");
        assertThat(contiene(datos, CMD_CAJON)).as("pulso de cajon").isTrue();
    }

    @Test
    void con_tarjeta_no_hay_cambio_ni_pulso_de_cajon() throws Exception {
        byte[] datos = new EscPosTicketBuilder(negocio, ticket).construir(ventaTarjeta());
        String txt = new String(datos, CP1252);

        assertThat(txt).contains("Forma de pago");
        assertThat(txt).contains("Tarjeta");
        assertThat(txt).doesNotContain("Cambio");
        assertThat(contiene(datos, CMD_CAJON)).as("pulso de cajon").isFalse();
    }

    @Test
    void por_defecto_corta_el_papel() throws Exception {
        byte[] datos = new EscPosTicketBuilder(negocio, ticket).construir(ventaEfectivo());
        assertThat(contiene(datos, CMD_CORTE)).as("comando de corte").isTrue();
    }

    @Test
    void si_cortar_esta_desactivado_no_manda_el_comando_de_corte() throws Exception {
        ticket.setCortar(false);
        byte[] datos = new EscPosTicketBuilder(negocio, ticket).construir(ventaEfectivo());
        assertThat(contiene(datos, CMD_CORTE)).as("comando de corte").isFalse();
    }

    @Test
    void si_abrir_cajon_esta_desactivado_no_manda_el_pulso_ni_en_efectivo() throws Exception {
        ticket.setAbrirCajonEnEfectivo(false);
        byte[] datos = new EscPosTicketBuilder(negocio, ticket).construir(ventaEfectivo());
        assertThat(contiene(datos, CMD_CAJON)).as("pulso de cajon").isFalse();
    }

    // --- helpers de formato ---

    @Test
    void dinero_usa_punto_de_miles_y_sin_decimales() {
        assertThat(EscPosTicketBuilder.dinero(new BigDecimal("1234567.89"))).isEqualTo("$ 1.234.568");
        assertThat(EscPosTicketBuilder.dinero(new BigDecimal("0"))).isEqualTo("$ 0");
        assertThat(EscPosTicketBuilder.dinero(null)).isEqualTo("$ 0");
    }

    @Test
    void dosColumnas_pega_la_derecha_al_margen() {
        String linea = EscPosTicketBuilder.dosColumnas("TOTAL", "$ 100", 20);
        assertThat(linea).hasSize(20);
        assertThat(linea).startsWith("TOTAL");
        assertThat(linea).endsWith("$ 100");
    }

    @Test
    void dosColumnas_recorta_la_izquierda_si_no_cabe() {
        String linea = EscPosTicketBuilder.dosColumnas(
                "Descripcion larguisima que no cabe", "$ 1.000", 20);
        assertThat(linea).hasSize(20);
        assertThat(linea).endsWith("$ 1.000");
    }
}
