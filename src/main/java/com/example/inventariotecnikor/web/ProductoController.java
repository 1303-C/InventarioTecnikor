package com.example.inventariotecnikor.web;

import com.example.inventariotecnikor.exception.CodigoQrDuplicadoException;
import com.example.inventariotecnikor.model.Categoria;
import com.example.inventariotecnikor.model.Producto;
import com.example.inventariotecnikor.service.MovimientoService;
import com.example.inventariotecnikor.service.ProductoService;
import com.example.inventariotecnikor.web.form.ProductoForm;
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
 * Listado, busqueda, alta y edicion de productos.
 *
 * @RequestMapping("/productos") a nivel de clase = prefijo comun de todas
 * las rutas de abajo.
 *
 * Patron PRG (Post-Redirect-Get): tras un POST que modifica datos hacemos
 * "redirect:" en vez de devolver HTML directamente. Asi, si el usuario
 * recarga la pagina, no reenvia el formulario. El mensaje de exito viaja
 * en un "flash attribute" (sobrevive justo a ese redirect y desaparece).
 */
@Controller
@RequestMapping("/productos")
public class ProductoController {

    private final ProductoService productoService;
    private final MovimientoService movimientoService;
    private final CorrectorTeclado correctorTeclado;

    public ProductoController(ProductoService productoService,
                             MovimientoService movimientoService,
                             CorrectorTeclado correctorTeclado) {
        this.productoService = productoService;
        this.movimientoService = movimientoService;
        this.correctorTeclado = correctorTeclado;
    }

    // ------------------------------------------------------------------
    //  Listado + buscador
    // ------------------------------------------------------------------

    @GetMapping
    public String lista(@RequestParam(required = false) String q, Model model) {
        var resultados = productoService.buscar(q);

        // Se puede escanear directamente en el buscador. Si el lector esta
        // en distribucion US y Windows en espanol, el codigo llega con los
        // simbolos cambiados (- por /, ' por -, ...). Si la busqueda tal
        // cual no da nada, se reintenta corrigiendo esa distribucion.
        if (resultados.isEmpty() && q != null && !q.isBlank()) {
            String corregido = correctorTeclado.comoUs(q.trim());
            if (!corregido.equals(q.trim())) {
                var reintento = productoService.buscar(corregido);
                if (!reintento.isEmpty()) {
                    resultados = reintento;
                    q = corregido; // el buscador muestra el codigo ya corregido
                }
            }
        }

        model.addAttribute("productos", resultados);
        model.addAttribute("q", q);
        return "productos/lista";
    }

    // ------------------------------------------------------------------
    //  Ficha / detalle de un producto (con su historial de movimientos)
    // ------------------------------------------------------------------

    @GetMapping("/{id}")
    public String detalle(@PathVariable Long id, Model model) {
        Producto p = productoService.obtenerPorId(id); // 404 si no existe
        model.addAttribute("producto", p);
        model.addAttribute("movimientos", movimientoService.historialDe(id));
        return "productos/detalle";
    }

    // ------------------------------------------------------------------
    //  Alta
    // ------------------------------------------------------------------

    @GetMapping("/nuevo")
    public String formNuevo(@RequestParam(required = false) String codigoQr, Model model) {
        // Si venimos de un POST con errores, el "form" ya esta en el model;
        // solo lo creamos vacio la primera vez.
        if (!model.containsAttribute("form")) {
            ProductoForm form = new ProductoForm();
            form.setCodigoQr(codigoQr); // pre-rellena si venimos de un escaneo sin resultado
            model.addAttribute("form", form);
        }
        prepararCombosYModo(model, false, null);
        return "productos/form";
    }

    @PostMapping
    public String crear(@Valid @ModelAttribute("form") ProductoForm form,
                        BindingResult errores,
                        Model model,
                        RedirectAttributes flash) {

        if (errores.hasErrors()) {
            prepararCombosYModo(model, false, null);
            return "productos/form"; // vuelve al formulario mostrando los errores
        }

        try {
            Producto creado = productoService.alta(form.toProducto());
            flash.addFlashAttribute("mensajeExito",
                    "Producto \"" + creado.getNombre() + "\" creado correctamente.");
            return "redirect:/productos";
        } catch (CodigoQrDuplicadoException ex) {
            // Error de negocio que corresponde a un campo concreto:
            // lo colgamos de ese campo para que Thymeleaf lo pinte al lado.
            errores.rejectValue("codigoQr", "duplicado", ex.getMessage());
            prepararCombosYModo(model, false, null);
            return "productos/form";
        }
    }

    // ------------------------------------------------------------------
    //  Edicion
    // ------------------------------------------------------------------

    @GetMapping("/{id}/editar")
    public String formEditar(@PathVariable Long id, Model model) {
        Producto p = productoService.obtenerPorId(id); // lanza 404 si no existe
        model.addAttribute("form", ProductoForm.desde(p));
        prepararCombosYModo(model, true, p);
        return "productos/form";
    }

    @PostMapping("/{id}")
    public String actualizar(@PathVariable Long id,
                             @Valid @ModelAttribute("form") ProductoForm form,
                             BindingResult errores,
                             Model model,
                             RedirectAttributes flash) {

        if (errores.hasErrors()) {
            prepararCombosYModo(model, true, productoService.obtenerPorId(id));
            return "productos/form";
        }

        Producto actualizado = productoService.actualizarDatos(id, form.toProducto());
        flash.addFlashAttribute("mensajeExito",
                "Producto \"" + actualizado.getNombre() + "\" actualizado.");
        return "redirect:/productos";
    }

    @PostMapping("/{id}/desactivar")
    public String desactivar(@PathVariable Long id, RedirectAttributes flash) {
        productoService.desactivar(id);
        flash.addFlashAttribute("mensajeExito", "Producto desactivado.");
        return "redirect:/productos";
    }

    // ------------------------------------------------------------------
    //  Helper: datos que la plantilla del formulario necesita siempre
    // ------------------------------------------------------------------

    private void prepararCombosYModo(Model model, boolean esEdicion, Producto producto) {
        model.addAttribute("categorias", Categoria.values());
        model.addAttribute("esEdicion", esEdicion);
        if (producto != null) {
            model.addAttribute("producto", producto); // para mostrar el stock actual (solo lectura)
        }
    }
}
