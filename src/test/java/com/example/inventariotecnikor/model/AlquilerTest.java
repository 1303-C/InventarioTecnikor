package com.example.inventariotecnikor.model;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/** estaVencido() y marcarDevuelto() de un prestamo de lavadora. */
class AlquilerTest {

    private static void set(Object destino, String campo, Object valor) {
        try {
            Field f = destino.getClass().getDeclaredField(campo);
            f.setAccessible(true);
            f.set(destino, valor);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    private Alquiler alquiler(LocalDateTime fechaFinEstimada) {
        Lavadora lavadora = new Lavadora("PEQ-01", new PlanAlquiler("Pequena", new BigDecimal("5000")));
        Venta venta = new Venta(1, FormaPago.EFECTIVO, "Carlos");
        Alquiler a = new Alquiler(lavadora, venta, "Juan", 3);
        set(a, "fechaFinEstimada", fechaFinEstimada);
        return a;
    }

    @Test
    void no_esta_vencido_si_todavia_falta_para_la_hora_de_devolucion() {
        Alquiler a = alquiler(LocalDateTime.now().plusHours(1));
        assertThat(a.estaVencido()).isFalse();
    }

    @Test
    void esta_vencido_si_ya_paso_la_hora_de_devolucion_y_sigue_activo() {
        Alquiler a = alquiler(LocalDateTime.now().minusMinutes(1));
        assertThat(a.estaVencido()).isTrue();
    }

    @Test
    void un_alquiler_ya_devuelto_no_cuenta_como_vencido_aunque_se_haya_pasado() {
        Alquiler a = alquiler(LocalDateTime.now().minusHours(5));
        a.marcarDevuelto();

        assertThat(a.estaVencido()).isFalse();
        assertThat(a.getEstado()).isEqualTo(EstadoAlquiler.DEVUELTO);
        assertThat(a.getFechaDevolucion()).isNotNull();
    }
}
