package com.example.inventariotecnikor.service;

import com.example.inventariotecnikor.exception.RecursoNoEncontradoException;
import com.example.inventariotecnikor.model.Alquiler;
import com.example.inventariotecnikor.model.EstadoAlquiler;
import com.example.inventariotecnikor.model.EstadoLavadora;
import com.example.inventariotecnikor.model.Lavadora;
import com.example.inventariotecnikor.model.Venta;
import com.example.inventariotecnikor.repository.AlquilerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Lado "operativo" del alquiler de lavadoras: que lavadora esta prestada,
 * desde cuando y a quien. El cobro se hace aparte, como una linea de una
 * Venta (VentaService.registrar), en el mismo momento en que se inicia el
 * prestamo (se cobra ANTES de entregar la lavadora).
 */
@Service
@Transactional(readOnly = true)
public class AlquilerService {

    private final AlquilerRepository alquilerRepository;
    private final LavadoraService lavadoraService;

    public AlquilerService(AlquilerRepository alquilerRepository, LavadoraService lavadoraService) {
        this.alquilerRepository = alquilerRepository;
        this.lavadoraService = lavadoraService;
    }

    /** Alquileres que todavia no han vuelto, del que deberia volver antes primero. */
    public List<Alquiler> activos() {
        return alquilerRepository.findByEstadoOrderByFechaFinEstimadaAsc(EstadoAlquiler.ACTIVO);
    }

    public Alquiler obtenerPorId(Long id) {
        return alquilerRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el alquiler con id: " + id));
    }

    /** Lanza si la lavadora no esta disponible; la usa VentaService antes de cobrar. */
    public Lavadora lavadoraDisponible(Long lavadoraId) {
        Lavadora lavadora = lavadoraService.obtenerPorId(lavadoraId);
        if (!lavadora.estaDisponible()) {
            throw new IllegalArgumentException(
                    "La lavadora \"" + lavadora.getCodigo() + "\" no esta disponible ahora mismo.");
        }
        return lavadora;
    }

    /**
     * Arranca el prestamo justo despues de cobrarlo: crea el registro y deja
     * la lavadora como PRESTADA. Lo llama VentaService dentro de la misma
     * transaccion que registra la venta.
     */
    @Transactional
    public Alquiler iniciar(Lavadora lavadora, Venta venta, String cliente, int horas) {
        lavadora.setEstado(EstadoLavadora.PRESTADA); // dirty checking
        return alquilerRepository.save(new Alquiler(lavadora, venta, cliente, horas));
    }

    /** La lavadora vuelve: se cierra el prestamo y queda disponible de nuevo. */
    @Transactional
    public Alquiler marcarDevuelto(Long id) {
        Alquiler alquiler = obtenerPorId(id);
        if (alquiler.getEstado() == EstadoAlquiler.DEVUELTO) {
            throw new IllegalArgumentException("Este alquiler ya estaba marcado como devuelto.");
        }
        alquiler.marcarDevuelto();
        alquiler.getLavadora().setEstado(EstadoLavadora.DISPONIBLE);
        return alquiler;
    }
}
