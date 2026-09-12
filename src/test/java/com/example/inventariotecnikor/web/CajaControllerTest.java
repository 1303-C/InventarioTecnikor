package com.example.inventariotecnikor.web;

import com.example.inventariotecnikor.model.FormaPago;
import com.example.inventariotecnikor.model.MovimientoCaja;
import com.example.inventariotecnikor.model.OrigenMovimientoCaja;
import com.example.inventariotecnikor.model.TipoMovimientoCaja;
import com.example.inventariotecnikor.service.CajaService;
import com.example.inventariotecnikor.service.CierreCaja;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(CajaController.class)
class CajaControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    CajaService cajaService;

    private static CierreCaja cierreVacio(LocalDate desde, LocalDate hasta) {
        BigDecimal cero = new BigDecimal("0.00");
        return new CierreCaja(desde, hasta, 0, cero, List.of(
                new CierreCaja.PorFormaPago(FormaPago.EFECTIVO, 0, cero),
                new CierreCaja.PorFormaPago(FormaPago.TARJETA, 0, cero),
                new CierreCaja.PorFormaPago(FormaPago.TRANSFERENCIA, 0, cero)),
                cero, cero, cero, cero, List.of());
    }

    @Test
    void get_cierre_muestra_el_cierre_del_rango_pedido() throws Exception {
        LocalDate desde = LocalDate.of(2026, 9, 1);
        LocalDate hasta = LocalDate.of(2026, 9, 10);
        when(cajaService.cierre(desde, hasta)).thenReturn(cierreVacio(desde, hasta));

        mvc.perform(get("/caja").param("desde", "2026-09-01").param("hasta", "2026-09-10"))
                .andExpect(status().isOk())
                .andExpect(view().name("caja/cierre"))
                .andExpect(model().attributeExists("cierre"));
    }

    @Test
    void get_cierre_sin_parametros_usa_hoy() throws Exception {
        LocalDate hoy = LocalDate.now();
        when(cajaService.cierre(hoy, hoy)).thenReturn(cierreVacio(hoy, hoy));

        mvc.perform(get("/caja")).andExpect(status().isOk());

        verify(cajaService).cierre(hoy, hoy);
    }

    @Test
    void post_movimientos_registra_un_ingreso_y_redirige_al_rango() throws Exception {
        mvc.perform(post("/caja/movimientos")
                        .param("tipo", "INGRESO")
                        .param("monto", "50000")
                        .param("motivo", "apertura de caja")
                        .param("desde", "2026-09-01")
                        .param("hasta", "2026-09-01"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/caja?desde=2026-09-01&hasta=2026-09-01"))
                .andExpect(flash().attributeExists("mensajeExito"));

        verify(cajaService).registrarIngreso(eq(new BigDecimal("50000")), eq("apertura de caja"), any());
    }

    @Test
    void post_movimientos_con_error_de_negocio_avisa_del_error() throws Exception {
        doThrow(new IllegalArgumentException("El monto debe ser mayor que cero."))
                .when(cajaService).registrarEgreso(any(), any(), any());

        mvc.perform(post("/caja/movimientos")
                        .param("tipo", "EGRESO")
                        .param("monto", "0")
                        .param("motivo", "x")
                        .param("desde", "2026-09-01")
                        .param("hasta", "2026-09-01"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("mensajeError"));
    }

    @Test
    void post_arqueo_sin_diferencia_avisa_que_cuadra() throws Exception {
        when(cajaService.arquear(any(), any(), any(), any())).thenReturn(Optional.empty());

        mvc.perform(post("/caja/arqueo")
                        .param("desde", "2026-09-01")
                        .param("hasta", "2026-09-01")
                        .param("contado", "0"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/caja?desde=2026-09-01&hasta=2026-09-01"))
                .andExpect(flash().attributeExists("mensajeExito"));
    }

    @Test
    void post_arqueo_con_diferencia_registra_y_avisa() throws Exception {
        when(cajaService.arquear(any(), any(), any(), any()))
                .thenReturn(Optional.of(new MovimientoCaja(TipoMovimientoCaja.INGRESO,
                        new BigDecimal("15000.00"), "Arqueo: sobran 15000", "Ana", OrigenMovimientoCaja.ARQUEO)));

        mvc.perform(post("/caja/arqueo")
                        .param("desde", "2026-09-01")
                        .param("hasta", "2026-09-01")
                        .param("contado", "15000"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("mensajeExito"));
    }
}
