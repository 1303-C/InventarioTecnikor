package com.example.inventariotecnikor.service;

import com.example.inventariotecnikor.exception.RecursoNoEncontradoException;
import com.example.inventariotecnikor.model.PlanAlquiler;
import com.example.inventariotecnikor.repository.PlanAlquilerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlanAlquilerServiceTest {

    @Mock
    PlanAlquilerRepository planAlquilerRepository;

    @InjectMocks
    PlanAlquilerService planAlquilerService;

    private PlanAlquiler pequena;

    @BeforeEach
    void setUp() {
        pequena = new PlanAlquiler("Pequena", new BigDecimal("5000.00"));
    }

    @Test
    void alta_guarda_el_plan_si_el_nombre_no_existe() {
        when(planAlquilerRepository.existsByNombre("Grande")).thenReturn(false);
        when(planAlquilerRepository.save(any(PlanAlquiler.class))).thenAnswer(inv -> inv.getArgument(0));

        PlanAlquiler creado = planAlquilerService.alta("Grande", new BigDecimal("8000"));

        assertThat(creado.getNombre()).isEqualTo("Grande");
        assertThat(creado.getTarifaPorHora()).isEqualByComparingTo("8000");
    }

    @Test
    void alta_con_nombre_repetido_lanza_IllegalArgument() {
        when(planAlquilerRepository.existsByNombre("Pequena")).thenReturn(true);

        assertThatThrownBy(() -> planAlquilerService.alta("Pequena", BigDecimal.TEN))
                .isInstanceOf(IllegalArgumentException.class);

        verify(planAlquilerRepository, never()).save(any());
    }

    @Test
    void alta_con_tarifa_negativa_lanza_IllegalArgument() {
        assertThatThrownBy(() -> planAlquilerService.alta("Nuevo", new BigDecimal("-1")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void actualizarTarifa_cambia_la_tarifa_del_plan_existente() {
        when(planAlquilerRepository.findById(1L)).thenReturn(Optional.of(pequena));

        planAlquilerService.actualizarTarifa(1L, new BigDecimal("6000"));

        assertThat(pequena.getTarifaPorHora()).isEqualByComparingTo("6000");
    }

    @Test
    void actualizarTarifa_de_un_plan_inexistente_lanza_RecursoNoEncontrado() {
        when(planAlquilerRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> planAlquilerService.actualizarTarifa(99L, BigDecimal.TEN))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void desactivar_y_reactivar_cambian_el_flag_activo() {
        when(planAlquilerRepository.findById(1L)).thenReturn(Optional.of(pequena));

        planAlquilerService.desactivar(1L);
        assertThat(pequena.isActivo()).isFalse();

        planAlquilerService.reactivar(1L);
        assertThat(pequena.isActivo()).isTrue();
    }
}
