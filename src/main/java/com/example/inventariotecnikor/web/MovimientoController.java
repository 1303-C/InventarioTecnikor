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
 * Registro de entradas, salidas y ajustes por conteo de un producto concreto.
 *
 * Las rutas cuelgan de un producto:
 *   GET  /productos/{productoId}/movimientos/nuevo?tipo=ENTRADA  -> formulario
 *   POST /productos/{productoId}/movimientos                     -> lo guarda
 *
 * tipo puede ser ENTRADA, SALIDA o AJUSTE. Para AJUSTE, "cantidad" es el
 * total contado fisicamente y el servicio calcula solo la diferencia.
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
            // En un ajuste se parte del stock actual: el operario solo cambia
            // el numero si el conteo no cuadra.
            if (tipo == TipoMovimiento.AJUSTE) {
                form.setCantidad(producto.getStockActual());
            }
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

        // En un AJUSTE, cantidad = total contado y puede ser 0. En una
        // ENTRADA/SALIDA tiene que ser > 0 (el 0 lo deja pasar @PositiveOrZero).
        if (form.getTipo() != TipoMovimiento.AJUSTE
                && form.getCantidad() != null && form.getCantidad() == 0) {
            errores.rejectValue("cantidad", "positivo", "La cantidad debe ser mayor que cero");
        }

        if (errores.hasErrors()) {
            model.addAttribute("producto", producto);
            return "movimientos/form";
        }

        try {
            if (form.getTipo() == TipoMovimiento.AJUSTE) {
                var ajuste = movimientoService.ajustarA(
                        productoId, form.getCantidad(), form.getMotivo(), form.getResponsable());
                flash.addFlashAttribute("mensajeExito", ajuste
                        .map(m -> "Stock ajustado a " + m.getStockResultante() + " unidad(es).")
                        .orElse("El conteo coincide con el stock actual ("
                                + producto.getStockActual() + "). No se registro ningun ajuste."));
            } else {
                movimientoService.registrar(
                        productoId,
                        form.getTipo(),
                        form.getCantidad(),
                        form.getMotivo(),
                        form.getResponsable());
                String verbo = form.getTipo() == TipoMovimiento.ENTRADA ? "Entrada" : "Salida";
                flash.addFlashAttribute("mensajeExito",
                        verbo + " de " + form.getCantidad() + " unidad(es) registrada.");
            }
        } catch (StockInsuficienteException ex) {
            // Regla de negocio: la mostramos junto al campo cantidad en vez
            // de mandar al usuario a la pagina de error generica.
            errores.rejectValue("cantidad", "stockInsuficiente", ex.getMessage());
            model.addAttribute("producto", producto);
            return "movimientos/form";
        }

        return "redirect:/productos/" + productoId;
    }
}
