package com.example.inventariotecnikor.web;

import com.example.inventariotecnikor.model.Alquiler;
import com.example.inventariotecnikor.model.FormaPago;
import com.example.inventariotecnikor.model.Lavadora;
import com.example.inventariotecnikor.model.PlanAlquiler;
import com.example.inventariotecnikor.model.Venta;
import com.example.inventariotecnikor.service.AlquilerService;
import com.example.inventariotecnikor.service.LavadoraService;
import com.example.inventariotecnikor.service.PlanAlquilerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(AlquilerController.class)
@Import(com.example.inventariotecnikor.config.MarcaDeArranque.class)
class AlquilerControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    AlquilerService alquilerService;
    @MockitoBean
    LavadoraService lavadoraService;
    @MockitoBean
    PlanAlquilerService planAlquilerService;

    private static void set(Object destino, String campo, Object valor) {
        try {
            Field f = destino.getClass().getDeclaredField(campo);
            f.setAccessible(true);
            f.set(destino, valor);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    private static Alquiler alquilerActivo(long id) {
        PlanAlquiler plan = new PlanAlquiler("Pequena", new BigDecimal("5000"));
        Lavadora lavadora = new Lavadora("PEQ-01", plan);
        Venta venta = new Venta(1, FormaPago.EFECTIVO, "Carlos");
        Alquiler a = new Alquiler(lavadora, venta, "Juan", 3);
        set(a, "id", id);
        set(a, "fechaInicio", java.time.LocalDateTime.now());
        set(a, "fechaFinEstimada", java.time.LocalDateTime.now().plusHours(3));
        return a;
    }

    @Test
    void get_disponibilidad_muestra_flota_y_prestamos_activos() throws Exception {
        when(lavadoraService.listarTodas()).thenReturn(List.of());
        when(alquilerService.activos()).thenReturn(List.of(alquilerActivo(1)));

        mvc.perform(get("/alquiler"))
                .andExpect(status().isOk())
                .andExpect(view().name("alquiler/disponibilidad"));
    }

    @Test
    void post_devolver_marca_la_lavadora_devuelta() throws Exception {
        Alquiler a = alquilerActivo(1);
        a.marcarDevuelto();
        when(alquilerService.marcarDevuelto(1L)).thenReturn(a);

        mvc.perform(post("/alquiler/1/devolver"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("mensajeExito"));
    }

    @Test
    void get_historial_lista_los_devueltos_del_rango() throws Exception {
        when(alquilerService.historial(any(), any())).thenReturn(List.of());

        mvc.perform(get("/alquiler/historial").param("desde", "2026-09-01").param("hasta", "2026-09-10"))
                .andExpect(status().isOk())
                .andExpect(view().name("alquiler/historial"));

        verify(alquilerService).historial(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 10));
    }

    @Test
    void post_crear_lavadora_con_codigo_repetido_avisa_del_error() throws Exception {
        org.mockito.Mockito.doThrow(new IllegalArgumentException("Ya existe una lavadora con el codigo \"PEQ-01\"."))
                .when(lavadoraService).alta("PEQ-01", 1L);

        mvc.perform(post("/alquiler/lavadoras").param("codigo", "PEQ-01").param("planId", "1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("mensajeError"));
    }

    @Test
    void get_planes_muestra_el_catalogo() throws Exception {
        when(planAlquilerService.listarTodos()).thenReturn(List.of());

        mvc.perform(get("/alquiler/planes"))
                .andExpect(status().isOk())
                .andExpect(view().name("alquiler/planes"));
    }
}
