package com.example.inventariotecnikor.web;

import com.example.inventariotecnikor.exception.StockInsuficienteException;
import com.example.inventariotecnikor.model.Producto;
import com.example.inventariotecnikor.model.TipoMovimiento;
import com.example.inventariotecnikor.service.MovimientoService;
import com.example.inventariotecnikor.service.ProductoService;
import com.example.inventariotecnikor.web.form.MovimientoForm;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Registro de entradas y salidas de stock de un producto concreto.
 *
 * Las rutas cuelgan de un producto:
 *   GET  /productos/{productoId}/movimientos/nuevo?tipo=ENTRADA  -> formulario
 *   POST /productos/{productoId}/movimientos                     -> lo guarda
 *
 * El historial se ve en la ficha del producto (ProductoController#detalle).
 */
@Controller
@RequestMapping("/productos/{productoId}/movimientos")
public class MovimientoController {

    private final ProductoService productoService;
    private final MovimientoService movimientoService;

    public MovimientoController(ProductoService productoService,
                               MovimientoService movimientoService) {
        this.productoService = productoService;
        this.movimientoService = movimientoService;
    }

    @GetMapping("/nuevo")
    public String formNuevo(@PathVariable Long productoId,
                            @RequestParam TipoMovimiento tipo,
                            Model model) {

        Producto producto = productoService.obtenerPorId(productoId); // 404 si no existe

        if (!model.containsAttribute("form")) {
            MovimientoForm form = new MovimientoForm();
            form.setTipo(tipo);
            model.addAttribute("form", form);
        }
        model.addAttribute("producto", producto);
        return "movimientos/form";
    }

    @PostMapping
    public String registrar(@PathVariable Long productoId,
                            @Valid @ModelAttribute("form") MovimientoForm form,
                            BindingResult errores,
                            Model model,
                            RedirectAttributes flash) {

        Producto producto = productoService.obtenerPorId(productoId);

        if (errores.hasErrors()) {
            model.addAttribute("producto", producto);
            return "movimientos/form";
        }

        try {
            movimientoService.registrar(
                    productoId,
                    form.getTipo(),
                    form.getCantidad(),
                    form.getMotivo(),
                    form.getResponsable());
        } catch (StockInsuficienteException ex) {
            // Regla de negocio: la mostramos junto al campo cantidad en vez
            // de mandar al usuario a la pagina de error generica.
            errores.rejectValue("cantidad", "stockInsuficiente", ex.getMessage());
            model.addAttribute("producto", producto);
            return "movimientos/form";
        }

        String verbo = form.getTipo() == TipoMovimiento.ENTRADA ? "Entrada" : "Salida";
        flash.addFlashAttribute("mensajeExito",
                verbo + " de " + form.getCantidad() + " unidad(es) registrada.");
        return "redirect:/productos/" + productoId;
    }
}
