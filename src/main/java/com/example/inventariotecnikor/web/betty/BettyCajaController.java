package com.example.inventariotecnikor.web.betty;

import com.example.inventariotecnikor.service.CajaBettyService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;

@Controller
@RequestMapping("/betty/caja")
public class BettyCajaController {

    private final CajaBettyService cajaService;

    public BettyCajaController(CajaBettyService cajaService) {
        this.cajaService = cajaService;
    }

    @GetMapping
    public String cierre(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
                          Model model) {
        if (desde == null) desde = LocalDate.now();
        if (hasta == null) hasta = LocalDate.now();

        model.addAttribute("cierre", cajaService.cierre(desde, hasta));
        model.addAttribute("desde", desde);
        model.addAttribute("hasta", hasta);
        return "betty/caja/cierre";
    }

    @PostMapping("/movimientos")
    public String registrarMovimiento(@RequestParam String tipo,
                                       @RequestParam BigDecimal monto,
                                       @RequestParam String motivo,
                                       @RequestParam(required = false) String responsable,
                                       RedirectAttributes redirect) {
        try {
            if ("INGRESO".equals(tipo)) {
                cajaService.registrarIngreso(monto, motivo, responsable);
            } else {
                cajaService.registrarEgreso(monto, motivo, responsable);
            }
            redirect.addFlashAttribute("mensajeExito", "Movimiento registrado correctamente.");
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("mensajeError", e.getMessage());
        }
        return "redirect:/betty/caja";
    }

    @GetMapping("/movimientos")
    public String movimientos(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                               @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
                               Model model) {
        if (desde == null) desde = LocalDate.now();
        if (hasta == null) hasta = LocalDate.now();

        model.addAttribute("cierre", cajaService.cierre(desde, hasta));
        model.addAttribute("desde", desde);
        model.addAttribute("hasta", hasta);
        return "betty/caja/movimientos";
    }
}
