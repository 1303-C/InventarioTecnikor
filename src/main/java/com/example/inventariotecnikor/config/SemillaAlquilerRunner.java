package com.example.inventariotecnikor.config;

import com.example.inventariotecnikor.model.PlanAlquiler;
import com.example.inventariotecnikor.repository.PlanAlquilerRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Crea los 3 planes de alquiler (Pequena / Grande / Premium) la primera vez
 * que arranca la app, con tarifa en 0 para que solo haga falta ajustarla
 * cuando la definan (Planes de alquiler -> editar tarifa), sin tener que
 * crearlos a mano.
 */
@Component
public class SemillaAlquilerRunner implements ApplicationRunner {

    private final PlanAlquilerRepository planAlquilerRepository;

    public SemillaAlquilerRunner(PlanAlquilerRepository planAlquilerRepository) {
        this.planAlquilerRepository = planAlquilerRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (planAlquilerRepository.count() > 0) {
            return;
        }
        planAlquilerRepository.save(new PlanAlquiler("Pequena", BigDecimal.ZERO));
        planAlquilerRepository.save(new PlanAlquiler("Grande", BigDecimal.ZERO));
        planAlquilerRepository.save(new PlanAlquiler("Premium", BigDecimal.ZERO));
    }
}
