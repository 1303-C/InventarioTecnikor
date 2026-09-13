package com.example.inventariotecnikor.web;

import com.example.inventariotecnikor.model.EstadoLavadora;
import com.example.inventariotecnikor.service.AlquilerService;
import com.example.inventariotecnikor.service.LavadoraService;
import com.example.inventariotecnikor.service.PlanAlquilerService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;

/**
 * Disponibilidad de la flota de lavadoras de alquiler, y el catalogo de
 * planes (tarifas por hora). El cobro y el arranque del prestamo pasan por
 * la pantalla de venta (VentaController); aqui solo se ve el estado y se
 * marca la devolucion.
 *
 *   GET  /alquiler                          disponibilidad
 *   POST /alquiler/{id}/devolver            marca una lavadora como devuelta
 *   GET  /alquiler/lavadoras/nueva          formulario de alta
 *   POST /alquiler/lavadoras                da de alta una lavadora
 *   POST /alquiler/lavadoras/{id}/estado    cambia el estado a mano (ej. averia)
 *   GET  /alquiler/planes                   planes y tarifas
 *   POST /alquiler/planes                   nuevo plan
 *   POST /alquiler/planes/{id}              actualiza la tarifa de un plan
 *   POST /alquiler/planes/{id}/desactivar
 *   POST /alquiler/planes/{id}/reactivar
 */
@Controller
@RequestMapping("/alquiler")
public class AlquilerController {

    private final AlquilerService alquilerService;
    private final LavadoraService lavadoraService;
    private final PlanAlquilerService planAlquilerService;

    public AlquilerController(AlquilerService alquilerService,
                              LavadoraService lavadoraService,
                              PlanAlquilerService planAlquilerService) {
        this.alquilerService = alquilerService;
        this.lavadoraService = lavadoraService;
        this.planAlquilerService = planAlquilerService;
    }

    @GetMapping
    public String disponibilidad(Model model) {
        model.addAttribute("lavadoras", lavadoraService.listarTodas());
        model.addAttribute("alquileresActivos", alquilerService.activos());
        return "alquiler/disponibilidad";
    }

    @PostMapping("/{id}/devolver")
    public String devolver(@PathVariable Long id, RedirectAttributes flash) {
        try {
            var alquiler = alquilerService.marcarDevuelto(id);
            flash.addFlashAttribute("mensajeExito",
                    "Lavadora " + alquiler.getLavadora().getCodigo() + " marcada como devuelta.");
        } catch (IllegalArgumentException ex) {
            flash.addFlashAttribute("mensajeError", ex.getMessage());
        }
        return "redirect:/alquiler";
    }

    // ------------------------------------------------------------------
    //  Lavadoras
    // ------------------------------------------------------------------

    @GetMapping("/lavadoras/nueva")
    public String formNuevaLavadora(Model model) {
        model.addAttribute("planes", planAlquilerService.listarActivos());
        return "alquiler/lavadora-form";
    }

    @PostMapping("/lavadoras")
    public String crearLavadora(@RequestParam String codigo, @RequestParam Long planId, RedirectAttributes flash) {
        try {
            lavadoraService.alta(codigo, planId);
            flash.addFlashAttribute("mensajeExito", "Lavadora agregada.");
        } catch (IllegalArgumentException ex) {
            flash.addFlashAttribute("mensajeError", ex.getMessage());
        }
        return "redirect:/alquiler";
    }

    @PostMapping("/lavadoras/{id}/estado")
    public String cambiarEstadoLavadora(@PathVariable Long id, @RequestParam EstadoLavadora estado,
                                        RedirectAttributes flash) {
        try {
            lavadoraService.cambiarEstado(id, estado);
            flash.addFlashAttribute("mensajeExito", "Estado de la lavadora actualizado.");
        } catch (IllegalArgumentException ex) {
            flash.addFlashAttribute("mensajeError", ex.getMessage());
        }
        return "redirect:/alquiler";
    }

    // ------------------------------------------------------------------
    //  Planes
    // ------------------------------------------------------------------

    @GetMapping("/planes")
    public String planes(Model model) {
        model.addAttribute("planes", planAlquilerService.listarTodos());
        return "alquiler/planes";
    }

    @PostMapping("/planes")
    public String crearPlan(@RequestParam String nombre, @RequestParam BigDecimal tarifaPorHora,
                            RedirectAttributes flash) {
        try {
            planAlquilerService.alta(nombre, tarifaPorHora);
            flash.addFlashAttribute("mensajeExito", "Plan agregado.");
        } catch (IllegalArgumentException ex) {
            flash.addFlashAttribute("mensajeError", ex.getMessage());
        }
        return "redirect:/alquiler/planes";
    }

    @PostMapping("/planes/{id}")
    public String actualizarPlan(@PathVariable Long id, @RequestParam BigDecimal tarifaPorHora,
                                 RedirectAttributes flash) {
        try {
            planAlquilerService.actualizarTarifa(id, tarifaPorHora);
            flash.addFlashAttribute("mensajeExito", "Tarifa actualizada.");
        } catch (IllegalArgumentException ex) {
            flash.addFlashAttribute("mensajeError", ex.getMessage());
        }
        return "redirect:/alquiler/planes";
    }

    @PostMapping("/planes/{id}/desactivar")
    public String desactivarPlan(@PathVariable Long id, RedirectAttributes flash) {
        planAlquilerService.desactivar(id);
        flash.addFlashAttribute("mensajeExito", "Plan desactivado.");
        return "redirect:/alquiler/planes";
    }

    @PostMapping("/planes/{id}/reactivar")
    public String reactivarPlan(@PathVariable Long id, RedirectAttributes flash) {
        planAlquilerService.reactivar(id);
        flash.addFlashAttribute("mensajeExito", "Plan reactivado.");
        return "redirect:/alquiler/planes";
    }
}
