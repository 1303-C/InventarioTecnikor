package com.example.inventariotecnikor.service;

import com.example.inventariotecnikor.exception.RecursoNoEncontradoException;
import com.example.inventariotecnikor.model.EstadoLavadora;
import com.example.inventariotecnikor.model.Lavadora;
import com.example.inventariotecnikor.model.PlanAlquiler;
import com.example.inventariotecnikor.repository.LavadoraRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Flota de lavadoras que Tecnikor alquila. No son Producto del inventario
 * de repuestos: no tienen QR de venta ni stock, son activos propios que
 * salen y vuelven.
 */
@Service
@Transactional(readOnly = true)
public class LavadoraService {

    private final LavadoraRepository lavadoraRepository;
    private final PlanAlquilerService planAlquilerService;

    public LavadoraService(LavadoraRepository lavadoraRepository, PlanAlquilerService planAlquilerService) {
        this.lavadoraRepository = lavadoraRepository;
        this.planAlquilerService = planAlquilerService;
    }

    public List<Lavadora> listarTodas() {
        return lavadoraRepository.findAllByOrderByCodigoAsc();
    }

    public Lavadora obtenerPorId(Long id) {
        return lavadoraRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la lavadora con id: " + id));
    }

    /** Lavadoras disponibles de un plan concreto, para elegir al cobrar un alquiler. */
    public List<Lavadora> disponiblesDe(Long planId) {
        return lavadoraRepository.findByPlanIdAndEstadoOrderByCodigoAsc(planId, EstadoLavadora.DISPONIBLE);
    }

    @Transactional
    public Lavadora alta(String codigo, Long planId) {
        if (codigo == null || codigo.isBlank()) {
            throw new IllegalArgumentException("La lavadora necesita un codigo.");
        }
        String limpio = codigo.trim();
        if (lavadoraRepository.existsByCodigo(limpio)) {
            throw new IllegalArgumentException("Ya existe una lavadora con el codigo \"" + limpio + "\".");
        }
        PlanAlquiler plan = planAlquilerService.obtenerPorId(planId);
        return lavadoraRepository.save(new Lavadora(limpio, plan));
    }

    /** Cambio de estado manual (ej. mandarla a "fuera de servicio" por una averia). */
    @Transactional
    public void cambiarEstado(Long id, EstadoLavadora estado) {
        Lavadora lavadora = obtenerPorId(id);
        if (lavadora.getEstado() == EstadoLavadora.PRESTADA && estado != EstadoLavadora.PRESTADA) {
            throw new IllegalArgumentException(
                    "La lavadora \"" + lavadora.getCodigo() + "\" esta prestada; hay que marcarla devuelta primero.");
        }
        lavadora.setEstado(estado);
    }
}
