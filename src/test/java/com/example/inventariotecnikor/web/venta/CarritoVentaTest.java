package com.example.inventariotecnikor.web.venta;

import com.example.inventariotecnikor.model.Categoria;
import com.example.inventariotecnikor.model.Producto;
import com.example.inventariotecnikor.service.VentaService;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Logica del carrito de venta en sesion: agregar/agrupar, cambiar cantidad,
 * quitar, totales y traduccion a lo que espera VentaService.
 */
class CarritoVentaTest {

    private static Producto producto(long id, String codigo, String nombre, String precio) {
        Producto p = new Producto(codigo, nombre, Categoria.LAVADORA, 1);
        p.setPrecioVenta(new BigDecimal(precio));
        try {
            Field f = Producto.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(p, id);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
        return p;
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
}
