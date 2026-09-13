package com.example.inventariotecnikor.service;

import com.example.inventariotecnikor.exception.RecursoNoEncontradoException;
import com.example.inventariotecnikor.model.Alquiler;
import com.example.inventariotecnikor.model.EstadoAlquiler;
import com.example.inventariotecnikor.model.EstadoLavadora;
import com.example.inventariotecnikor.model.FormaPago;
import com.example.inventariotecnikor.model.Lavadora;
import com.example.inventariotecnikor.model.PlanAlquiler;
import com.example.inventariotecnikor.model.Venta;
import com.example.inventariotecnikor.repository.AlquilerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AlquilerServiceTest {

    @Mock
    AlquilerRepository alquilerRepository;

    @Mock
    LavadoraService lavadoraService;

    @InjectMocks
    AlquilerService alquilerService;

    private Lavadora lavadora;
    private Venta venta;

    @BeforeEach
    void setUp() {
        PlanAlquiler plan = new PlanAlquiler("Pequena", new BigDecimal("5000.00"));
        lavadora = new Lavadora("PEQ-01", plan);
        venta = new Venta(1, FormaPago.EFECTIVO, "Carlos");
    }

    @Test
    void lavadoraDisponible_devuelve_la_lavadora_si_esta_disponible() {
        when(lavadoraService.obtenerPorId(1L)).thenReturn(lavadora);

        assertThat(alquilerService.lavadoraDisponible(1L)).isSameAs(lavadora);
    }

    @Test
    void lavadoraDisponible_lanza_si_esta_prestada() {
        lavadora.setEstado(EstadoLavadora.PRESTADA);
        when(lavadoraService.obtenerPorId(1L)).thenReturn(lavadora);

        assertThatThrownBy(() -> alquilerService.lavadoraDisponible(1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("PEQ-01");
    }

    @Test
    void iniciar_crea_el_alquiler_y_deja_la_lavadora_prestada() {
        when(alquilerRepository.save(any(Alquiler.class))).thenAnswer(inv -> inv.getArgument(0));

        Alquiler alquiler = alquilerService.iniciar(lavadora, venta, "Juan", 3);

        assertThat(lavadora.getEstado()).isEqualTo(EstadoLavadora.PRESTADA);
        assertThat(alquiler.getLavadora()).isSameAs(lavadora);
        assertThat(alquiler.getVenta()).isSameAs(venta);
        assertThat(alquiler.getCliente()).isEqualTo("Juan");
        assertThat(alquiler.getHoras()).isEqualTo(3);
    }

    @Test
    void marcarDevuelto_cierra_el_alquiler_y_deja_la_lavadora_disponible() {
        lavadora.setEstado(EstadoLavadora.PRESTADA);
        Alquiler alquiler = new Alquiler(lavadora, venta, "Juan", 3);
        when(alquilerRepository.findById(1L)).thenReturn(Optional.of(alquiler));

        alquilerService.marcarDevuelto(1L);

        assertThat(alquiler.getEstado()).isEqualTo(EstadoAlquiler.DEVUELTO);
        assertThat(alquiler.getFechaDevolucion()).isNotNull();
        assertThat(lavadora.getEstado()).isEqualTo(EstadoLavadora.DISPONIBLE);
    }

    @Test
    void marcarDevuelto_dos_veces_lanza_IllegalArgument() {
        Alquiler alquiler = new Alquiler(lavadora, venta, "Juan", 3);
        alquiler.marcarDevuelto();
        when(alquilerRepository.findById(1L)).thenReturn(Optional.of(alquiler));

        assertThatThrownBy(() -> alquilerService.marcarDevuelto(1L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void obtenerPorId_de_un_alquiler_inexistente_lanza_RecursoNoEncontrado() {
        when(alquilerRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> alquilerService.obtenerPorId(99L))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    // ------------------------------------------------------------------
    //  revertirPorVenta (lo usa VentaService.anular)
    // ------------------------------------------------------------------

    @Test
    void revertirPorVenta_libera_la_lavadora_de_un_alquiler_activo() {
        lavadora.setEstado(EstadoLavadora.PRESTADA);
        Alquiler alquiler = new Alquiler(lavadora, venta, "Juan", 3);
        when(alquilerRepository.findByVentaId(1L)).thenReturn(List.of(alquiler));

        alquilerService.revertirPorVenta(1L);

        assertThat(alquiler.getEstado()).isEqualTo(EstadoAlquiler.DEVUELTO);
        assertThat(lavadora.getEstado()).isEqualTo(EstadoLavadora.DISPONIBLE);
    }

    @Test
    void revertirPorVenta_no_toca_un_alquiler_que_ya_estaba_devuelto() {
        Alquiler alquiler = new Alquiler(lavadora, venta, "Juan", 3);
        alquiler.marcarDevuelto();
        var fechaOriginal = alquiler.getFechaDevolucion();
        when(alquilerRepository.findByVentaId(1L)).thenReturn(List.of(alquiler));

        alquilerService.revertirPorVenta(1L);

        assertThat(alquiler.getFechaDevolucion()).isEqualTo(fechaOriginal);
    }

    @Test
    void revertirPorVenta_sin_alquileres_no_hace_nada() {
        when(alquilerRepository.findByVentaId(1L)).thenReturn(List.of());

        alquilerService.revertirPorVenta(1L); // no deberia lanzar
    }

    // ------------------------------------------------------------------
    //  historial
    // ------------------------------------------------------------------

    @Test
    void historial_consulta_los_devueltos_del_rango() {
        LocalDate desde = LocalDate.of(2026, 9, 1);
        LocalDate hasta = LocalDate.of(2026, 9, 10);
        when(alquilerRepository.findByEstadoAndFechaDevolucionBetweenOrderByFechaDevolucionDesc(
                EstadoAlquiler.DEVUELTO, desde.atStartOfDay(), hasta.plusDays(1).atStartOfDay()))
                .thenReturn(List.of());

        assertThat(alquilerService.historial(desde, hasta)).isEmpty();
    }

    @Test
    void historial_con_hasta_anterior_a_desde_lanza_IllegalArgument() {
        assertThatThrownBy(() -> alquilerService.historial(
                LocalDate.of(2026, 9, 10), LocalDate.of(2026, 9, 1)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
