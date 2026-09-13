package com.example.inventariotecnikor.service;

import com.example.inventariotecnikor.exception.RecursoNoEncontradoException;
import com.example.inventariotecnikor.model.PlanAlquiler;
import com.example.inventariotecnikor.repository.PlanAlquilerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Catalogo de planes de alquiler (Pequena / Grande / Premium, hoy) y su
 * tarifa por hora. Los precios todavia no estan definidos, asi que esto es
 * deliberadamente editable desde una pantalla, sin tocar codigo.
 */
@Service
@Transactional(readOnly = true)
public class PlanAlquilerService {

    private final PlanAlquilerRepository planAlquilerRepository;

    public PlanAlquilerService(PlanAlquilerRepository planAlquilerRepository) {
        this.planAlquilerRepository = planAlquilerRepository;
    }

    public List<PlanAlquiler> listarActivos() {
        return planAlquilerRepository.findByActivoTrueOrderByNombreAsc();
    }

    public List<PlanAlquiler> listarTodos() {
        return planAlquilerRepository.findAll();
    }

    public PlanAlquiler obtenerPorId(Long id) {
        return planAlquilerRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el plan de alquiler con id: " + id));
    }

    @Transactional
    public PlanAlquiler alta(String nombre, BigDecimal tarifaPorHora) {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El plan necesita un nombre.");
        }
        if (planAlquilerRepository.existsByNombre(nombre.trim())) {
            throw new IllegalArgumentException("Ya existe un plan de alquiler llamado \"" + nombre.trim() + "\".");
        }
        return planAlquilerRepository.save(
                new PlanAlquiler(nombre.trim(), tarifaValida(tarifaPorHora)));
    }

    /** Dirty checking dentro de la transaccion: no hace falta save(). */
    @Transactional
    public PlanAlquiler actualizarTarifa(Long id, BigDecimal tarifaPorHora) {
        PlanAlquiler plan = obtenerPorId(id);
        plan.setTarifaPorHora(tarifaValida(tarifaPorHora));
        return plan;
    }

    @Transactional
    public void desactivar(Long id) {
        obtenerPorId(id).setActivo(false);
    }

    @Transactional
    public void reactivar(Long id) {
        obtenerPorId(id).setActivo(true);
    }

    private static BigDecimal tarifaValida(BigDecimal tarifa) {
        if (tarifa == null || tarifa.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("La tarifa por hora no puede ser negativa.");
        }
        return tarifa;
    }
}
