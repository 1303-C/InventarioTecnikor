package com.example.inventariotecnikor.web.venta;

import com.example.inventariotecnikor.model.Categoria;
import com.example.inventariotecnikor.model.Lavadora;
import com.example.inventariotecnikor.model.PlanAlquiler;
import com.example.inventariotecnikor.model.Producto;
import com.example.inventariotecnikor.model.TipoLinea;
import com.example.inventariotecnikor.service.VentaService;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Logica del carrito de venta en sesion: agregar/agrupar, cambiar cantidad,
 * quitar, totales y traduccion a lo que espera VentaService. Cubre tanto
 * los repuestos (items) como el alquiler/mantenimiento (lineas libres).
 */
class CarritoVentaTest {

    private static void set(Object destino, String campo, Object valor) {
        try {
            Field f = destino.getClass().getDeclaredField(campo);
            f.setAccessible(true);
            f.set(destino, valor);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    private static Producto producto(long id, String codigo, String nombre, String precio) {
        Producto p = new Producto(codigo, nombre, Categoria.LAVADORA, 1);
        p.setPrecioVenta(new BigDecimal(precio));
        set(p, "id", id);
        return p;
    }

    private static Lavadora lavadora(long id, String codigo, String tarifaPorHora) {
        PlanAlquiler plan = new PlanAlquiler("Pequena", new BigDecimal(tarifaPorHora));
        Lavadora l = new Lavadora(codigo, plan);
        set(l, "id", id);
        return l;
    }

    @Test
    void agregar_nuevo_crea_una_linea_con_cantidad_1() {
        CarritoVenta carrito = new CarritoVenta();
        carrito.agregar(producto(1, "A", "Bomba", "100"), 1);

        assertThat(carrito.getItems()).hasSize(1);
        assertThat(carrito.getItems().iterator().next().getCantidad()).isEqualTo(1);
        assertThat(carrito.isVacio()).isFalse();
    }

    @Test
    void agregar_el_mismo_producto_suma_la_cantidad_en_una_sola_linea() {
        CarritoVenta carrito = new CarritoVenta();
        Producto bomba = producto(1, "A", "Bomba", "100");
        carrito.agregar(bomba, 1);
        carrito.agregar(bomba, 2);

        assertThat(carrito.getItems()).hasSize(1);
        assertThat(carrito.getTotalUnidades()).isEqualTo(3);
    }

    @Test
    void cambiarCantidad_fija_el_valor() {
        CarritoVenta carrito = new CarritoVenta();
        carrito.agregar(producto(1, "A", "Bomba", "100"), 1);

        carrito.cambiarCantidad(1L, 5);

        assertThat(carrito.getTotalUnidades()).isEqualTo(5);
    }

    @Test
    void cambiarCantidad_a_cero_quita_la_linea() {
        CarritoVenta carrito = new CarritoVenta();
        carrito.agregar(producto(1, "A", "Bomba", "100"), 3);

        carrito.cambiarCantidad(1L, 0);

        assertThat(carrito.isVacio()).isTrue();
    }

    @Test
    void quitar_elimina_la_linea() {
        CarritoVenta carrito = new CarritoVenta();
        carrito.agregar(producto(1, "A", "Bomba", "100"), 1);
        carrito.agregar(producto(2, "B", "Correa", "20"), 1);

        carrito.quitar(1L);

        assertThat(carrito.getItems()).hasSize(1);
        assertThat(carrito.getItems().iterator().next().getDescripcion()).isEqualTo("Correa");
    }

    @Test
    void total_e_importe_suman_todas_las_lineas() {
        CarritoVenta carrito = new CarritoVenta();
        carrito.agregar(producto(1, "A", "Bomba", "100.00"), 2);   // 200
        carrito.agregar(producto(2, "B", "Correa", "25.50"), 3);   // 76.50

        assertThat(carrito.getTotal()).isEqualByComparingTo("276.50");
        assertThat(carrito.getTotalUnidades()).isEqualTo(5);
    }

    @Test
    void aLineasSolicitadas_mapea_id_y_cantidad_en_orden() {
        CarritoVenta carrito = new CarritoVenta();
        carrito.agregar(producto(7, "A", "Bomba", "100"), 2);
        carrito.agregar(producto(3, "B", "Correa", "20"), 1);

        List<VentaService.LineaSolicitada> lineas = carrito.aLineasSolicitadas();

        assertThat(lineas).containsExactly(
                new VentaService.LineaSolicitada(7L, 2),
                new VentaService.LineaSolicitada(3L, 1));
    }

    @Test
    void vaciar_deja_el_carrito_vacio() {
        CarritoVenta carrito = new CarritoVenta();
        carrito.agregar(producto(1, "A", "Bomba", "100"), 1);

        carrito.vaciar();

        assertThat(carrito.isVacio()).isTrue();
        assertThat(carrito.getTotal()).isEqualByComparingTo("0");
    }

    // ------------------------------------------------------------------
    //  Lineas libres: alquiler / mantenimiento
    // ------------------------------------------------------------------

    @Test
    void agregarAlquiler_crea_una_linea_con_la_tarifa_del_plan() {
        CarritoVenta carrito = new CarritoVenta();
        carrito.agregarAlquiler(lavadora(9, "PEQ-01", "5000.00"), 3, "Juan");

        assertThat(carrito.getLibres()).hasSize(1);
        var linea = carrito.getLibres().get(0);
        assertThat(linea.getTipo()).isEqualTo(TipoLinea.ALQUILER);
        assertThat(linea.getCantidad()).isEqualTo(3);
        assertThat(linea.getPrecioUnitario()).isEqualByComparingTo("5000.00");
        assertThat(linea.getImporte()).isEqualByComparingTo("15000.00");
        assertThat(linea.getDescripcion()).contains("PEQ-01").contains("Juan");
        assertThat(carrito.isVacio()).isFalse();
    }

    @Test
    void agregarAlquiler_con_horas_no_positivas_lanza_IllegalArgument() {
        CarritoVenta carrito = new CarritoVenta();

        assertThatThrownBy(() -> carrito.agregarAlquiler(lavadora(9, "PEQ-01", "5000"), 0, "Juan"))
                .isInstanceOf(IllegalArgumentException.class);

        assertThat(carrito.getLibres()).isEmpty();
    }

    @Test
    void agregarServicio_crea_una_linea_de_mantenimiento_a_monto_libre() {
        CarritoVenta carrito = new CarritoVenta();
        carrito.agregarServicio("Mano de obra", new BigDecimal("50000"));

        var linea = carrito.getLibres().get(0);
        assertThat(linea.getTipo()).isEqualTo(TipoLinea.MANTENIMIENTO);
        assertThat(linea.getCantidad()).isEqualTo(1);
        assertThat(linea.getImporte()).isEqualByComparingTo("50000");
    }

    @Test
    void dos_servicios_no_se_agrupan_quedan_en_lineas_separadas() {
        CarritoVenta carrito = new CarritoVenta();
        carrito.agregarServicio("Visita/diagnostico", new BigDecimal("30000"));
        carrito.agregarServicio("Mano de obra", new BigDecimal("50000"));

        assertThat(carrito.getLibres()).hasSize(2);
        assertThat(carrito.getTotal()).isEqualByComparingTo("80000");
    }

    @Test
    void quitarLibre_elimina_solo_esa_linea() {
        CarritoVenta carrito = new CarritoVenta();
        carrito.agregarServicio("Visita", new BigDecimal("30000"));
        carrito.agregarServicio("Mano de obra", new BigDecimal("50000"));
        Long primeraId = carrito.getLibres().get(0).getId();

        carrito.quitarLibre(primeraId);

        assertThat(carrito.getLibres()).hasSize(1);
        assertThat(carrito.getLibres().get(0).getDescripcion()).isEqualTo("Mano de obra");
    }

    @Test
    void el_total_suma_productos_y_lineas_libres() {
        CarritoVenta carrito = new CarritoVenta();
        carrito.agregar(producto(1, "A", "Bomba", "100.00"), 1);      // 100
        carrito.agregarServicio("Mano de obra", new BigDecimal("50000")); // 50000

        assertThat(carrito.getTotal()).isEqualByComparingTo("50100.00");
    }

    @Test
    void aLineasLibresSolicitadas_incluye_lavadora_y_cliente_solo_en_alquiler() {
        CarritoVenta carrito = new CarritoVenta();
        carrito.agregarAlquiler(lavadora(9, "PEQ-01", "5000"), 2, "Juan");
        carrito.agregarServicio("Visita", new BigDecimal("30000"));

        List<VentaService.LineaLibreSolicitada> libres = carrito.aLineasLibresSolicitadas();

        assertThat(libres).hasSize(2);
        var alquiler = libres.get(0);
        assertThat(alquiler.tipo()).isEqualTo(TipoLinea.ALQUILER);
        assertThat(alquiler.lavadoraId()).isEqualTo(9L);
        assertThat(alquiler.clienteAlquiler()).isEqualTo("Juan");

        var servicio = libres.get(1);
        assertThat(servicio.tipo()).isEqualTo(TipoLinea.MANTENIMIENTO);
        assertThat(servicio.lavadoraId()).isNull();
    }

    @Test
    void vaciar_tambien_limpia_las_lineas_libres() {
        CarritoVenta carrito = new CarritoVenta();
        carrito.agregarServicio("Visita", new BigDecimal("30000"));

        carrito.vaciar();

        assertThat(carrito.isVacio()).isTrue();
        assertThat(carrito.getLibres()).isEmpty();
    }
}
