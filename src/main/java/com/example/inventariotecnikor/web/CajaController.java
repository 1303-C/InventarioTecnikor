package com.example.inventariotecnikor.web;

import com.example.inventariotecnikor.model.TipoMovimientoCaja;
import com.example.inventariotecnikor.service.CajaService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Cierre de caja de un rango de fechas, movimientos de efectivo que no son
 * venta (ingresos/egresos) y arqueo.
 *
 *   GET  /caja                 -> cierre del rango (un dia por defecto: hoy)
 *   POST /caja/movimientos     -> registra un ingreso o un egreso
 *   POST /caja/arqueo          -> compara el efectivo contado contra el
 *                                 esperado y registra la diferencia (si hay)
 */
@Controller
@RequestMapping("/caja")
public class CajaController {

    private final CajaService cajaService;

    public CajaController(CajaService cajaService) {
        this.cajaService = cajaService;
    }

    @GetMapping
    public String cierre(@RequestParam(required = false)
                         @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                         @RequestParam(required = false)
                         @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
                         Model model) {
        LocalDate hoy = LocalDate.now();
        LocalDate d = desde != null ? desde : hoy;
        LocalDate h = hasta != null ? hasta : (desde != null ? desde : hoy);
        if (h.isBefore(d)) {
            h = d;
        }
        model.addAttribute("cierre", cajaService.cierre(d, h));
        return "caja/cierre";
    }

    @PostMapping("/movimientos")
    public String registrarMovimiento(@RequestParam TipoMovimientoCaja tipo,
                                      @RequestParam BigDecimal monto,
                                      @RequestParam String motivo,
                                      @RequestParam(required = false) String responsable,
                                      @RequestParam LocalDate desde,
                                      @RequestParam LocalDate hasta,
                                      RedirectAttributes flash) {
        try {
            if (tipo == TipoMovimientoCaja.INGRESO) {
                cajaService.registrarIngreso(monto, motivo, responsable);
            } else {
                cajaService.registrarEgreso(monto, motivo, responsable);
            }
            flash.addFlashAttribute("mensajeExito", tipo.getEtiqueta() + " de caja registrado.");
        } catch (IllegalArgumentException ex) {
            flash.addFlashAttribute("mensajeError", ex.getMessage());
        }
        return "redirect:/caja?desde=" + desde + "&hasta=" + hasta;
    }

    @PostMapping("/arqueo")
    public String arquear(@RequestParam LocalDate desde,
                          @RequestParam LocalDate hasta,
                          @RequestParam BigDecimal contado,
                          @RequestParam(required = false) String responsable,
                          RedirectAttributes flash) {
        try {
            var diferencia = cajaService.arquear(desde, hasta, contado, responsable);
            flash.addFlashAttribute("mensajeExito", diferencia
                    .map(m -> "Arqueo registrado: " + m.getTipo().getEtiqueta() + " de " + m.getMonto() + ".")
                    .orElse("El arqueo cuadra: el efectivo contado coincide con el esperado."));
        } catch (IllegalArgumentException ex) {
            flash.addFlashAttribute("mensajeError", ex.getMessage());
        }
        return "redirect:/caja?desde=" + desde + "&hasta=" + hasta;
    }
}
