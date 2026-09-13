package com.example.inventariotecnikor.service;

import com.example.inventariotecnikor.exception.RecursoNoEncontradoException;
import com.example.inventariotecnikor.model.EstadoLavadora;
import com.example.inventariotecnikor.model.Lavadora;
import com.example.inventariotecnikor.model.PlanAlquiler;
import com.example.inventariotecnikor.repository.LavadoraRepository;
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
class LavadoraServiceTest {

    @Mock
    LavadoraRepository lavadoraRepository;

    @Mock
    PlanAlquilerService planAlquilerService;

    @InjectMocks
    LavadoraService lavadoraService;

    private PlanAlquiler pequena;

    @BeforeEach
    void setUp() {
        pequena = new PlanAlquiler("Pequena", new BigDecimal("5000.00"));
    }

    @Test
    void alta_guarda_la_lavadora_con_su_plan() {
        when(lavadoraRepository.existsByCodigo("PEQ-01")).thenReturn(false);
        when(planAlquilerService.obtenerPorId(1L)).thenReturn(pequena);
        when(lavadoraRepository.save(any(Lavadora.class))).thenAnswer(inv -> inv.getArgument(0));

        Lavadora creada = lavadoraService.alta("PEQ-01", 1L);

        assertThat(creada.getCodigo()).isEqualTo("PEQ-01");
        assertThat(creada.getPlan()).isSameAs(pequena);
        assertThat(creada.getEstado()).isEqualTo(EstadoLavadora.DISPONIBLE);
    }

    @Test
    void alta_con_codigo_repetido_lanza_IllegalArgument() {
        when(lavadoraRepository.existsByCodigo("PEQ-01")).thenReturn(true);

        assertThatThrownBy(() -> lavadoraService.alta("PEQ-01", 1L))
                .isInstanceOf(IllegalArgumentException.class);

        verify(lavadoraRepository, never()).save(any());
    }

    @Test
    void cambiarEstado_de_disponible_a_fuera_de_servicio_funciona() {
        Lavadora l = new Lavadora("PEQ-01", pequena);
        when(lavadoraRepository.findById(1L)).thenReturn(Optional.of(l));

        lavadoraService.cambiarEstado(1L, EstadoLavadora.FUERA_DE_SERVICIO);

        assertThat(l.getEstado()).isEqualTo(EstadoLavadora.FUERA_DE_SERVICIO);
    }

    @Test
    void cambiarEstado_de_una_lavadora_prestada_exige_devolverla_primero() {
        Lavadora l = new Lavadora("PEQ-01", pequena);
        l.setEstado(EstadoLavadora.PRESTADA);
        when(lavadoraRepository.findById(1L)).thenReturn(Optional.of(l));

        assertThatThrownBy(() -> lavadoraService.cambiarEstado(1L, EstadoLavadora.FUERA_DE_SERVICIO))
                .isInstanceOf(IllegalArgumentException.class);

        assertThat(l.getEstado()).isEqualTo(EstadoLavadora.PRESTADA);
    }

    @Test
    void obtenerPorId_de_una_lavadora_inexistente_lanza_RecursoNoEncontrado() {
        when(lavadoraRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> lavadoraService.obtenerPorId(99L))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }
}
