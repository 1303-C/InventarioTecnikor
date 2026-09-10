package com.example.inventariotecnikor.web;

import com.example.inventariotecnikor.model.Categoria;
import com.example.inventariotecnikor.model.FormaPago;
import com.example.inventariotecnikor.model.Producto;
import com.example.inventariotecnikor.model.Venta;
import com.example.inventariotecnikor.service.ProductoService;
import com.example.inventariotecnikor.service.VentaService;
import com.example.inventariotecnikor.service.ticket.TicketPrinter;
import com.example.inventariotecnikor.web.venta.CarritoVenta;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(VentaController.class)
@Import(CarritoVenta.class)
class VentaControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    ProductoService productoService;
    @MockitoBean
    VentaService ventaService;
    @MockitoBean
    TicketPrinter ticketPrinter;
    @MockitoBean
    CorrectorTeclado correctorTeclado;

    private static void set(Object destino, String campo, Object valor) {
        try {
            Field f = destino.getClass().getDeclaredField(campo);
            f.setAccessible(true);
            f.set(destino, valor);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    private static Producto producto(long id, String codigo, String precio) {
        Producto p = new Producto(codigo, "Bomba de agua", Categoria.LAVADORA, 1);
        p.setPrecioVenta(new BigDecimal(precio));
        set(p, "id", id);
        return p;
    }

    private static Venta venta(long id, long numero, FormaPago fp) {
        Venta v = new Venta(numero, fp, "Ana");
        set(v, "id", id);
        set(v, "fecha", LocalDateTime.now());
        return v;
    }

    @Test
    void get_nueva_muestra_la_pantalla_de_venta() throws Exception {
        mvc.perform(get("/ventas/nueva"))
                .andExpect(status().isOk())
                .andExpect(view().name("ventas/nueva"))
                .andExpect(model().attributeExists("carrito", "formasPago", "cobro"));
    }

    @Test
    void agregar_linea_con_codigo_conocido_mete_el_producto_y_avisa() throws Exception {
        when(productoService.buscarPorQrOpcional("ABC")).thenReturn(Optional.of(producto(1, "ABC", "100")));

        mvc.perform(post("/ventas/nueva/lineas").param("codigo", "ABC"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/ventas/nueva"))
                .andExpect(flash().attributeExists("mensajeExito"));
    }

    @Test
    void agregar_linea_con_codigo_desconocido_avisa_del_error() throws Exception {
        when(productoService.buscarPorQrOpcional("XXX")).thenReturn(Optional.empty());
        when(correctorTeclado.comoUs("XXX")).thenReturn("XXX"); // sin cambio -> no reintenta

        mvc.perform(post("/ventas/nueva/lineas").param("codigo", "XXX"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/ventas/nueva"))
                .andExpect(flash().attributeExists("mensajeError"));
    }

    @Test
    void cobrar_con_carrito_vacio_no_registra_nada() throws Exception {
        mvc.perform(post("/ventas")
                        .param("formaPago", "TARJETA"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/ventas/nueva"))
                .andExpect(flash().attributeExists("mensajeError"));

        verifyNoInteractions(ventaService);
    }

    @Test
    void cobrar_registra_la_venta_imprime_el_ticket_y_redirige_al_detalle() throws Exception {
        MockHttpSession sesion = new MockHttpSession();
        when(productoService.buscarPorQrOpcional("ABC")).thenReturn(Optional.of(producto(1, "ABC", "100")));
        when(ventaService.registrar(any(), eq(FormaPago.TARJETA), isNull(), any(), any(), any()))
                .thenReturn(venta(55, 1, FormaPago.TARJETA));

        // llena el carrito en esta sesion
        mvc.perform(post("/ventas/nueva/lineas").param("codigo", "ABC").session(sesion))
                .andExpect(status().is3xxRedirection());

        mvc.perform(post("/ventas").param("formaPago", "TARJETA").session(sesion))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/ventas/55"))
                .andExpect(flash().attributeExists("mensajeExito"));

        verify(ventaService).registrar(
                argThat(lineas -> lineas.size() == 1), eq(FormaPago.TARJETA), isNull(),
                any(), any(), any());
        verify(ticketPrinter).imprimir(any(Venta.class));
    }

    @Test
    void cobrar_en_efectivo_sin_monto_recibido_no_registra() throws Exception {
        MockHttpSession sesion = new MockHttpSession();
        when(productoService.buscarPorQrOpcional("ABC")).thenReturn(Optional.of(producto(1, "ABC", "100")));

        mvc.perform(post("/ventas/nueva/lineas").param("codigo", "ABC").session(sesion))
                .andExpect(status().is3xxRedirection());

        mvc.perform(post("/ventas").param("formaPago", "EFECTIVO").session(sesion))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/ventas/nueva"))
                .andExpect(flash().attributeExists("mensajeError"));

        verify(ventaService, never()).registrar(any(), any(), any(), anyString(), any(), any());
    }

    @Test
    void get_detalle_muestra_la_venta() throws Exception {
        when(ventaService.obtenerPorId(55L)).thenReturn(venta(55, 9, FormaPago.TARJETA));

        mvc.perform(get("/ventas/55"))
                .andExpect(status().isOk())
                .andExpect(view().name("ventas/detalle"))
                .andExpect(model().attributeExists("venta"));
    }

    @Test
    void get_historial_lista_las_ventas_del_dia() throws Exception {
        when(ventaService.ventasDelDia(any())).thenReturn(List.of());

        mvc.perform(get("/ventas").param("fecha", "2026-09-10"))
                .andExpect(status().isOk())
                .andExpect(view().name("ventas/historial"))
                .andExpect(model().attributeExists("ventas", "fecha", "totalDia"));
    }
}
