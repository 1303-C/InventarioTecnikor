package com.example.inventariotecnikor.web.betty;

import com.example.inventariotecnikor.model.*;
import com.example.inventariotecnikor.repository.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;

@Controller
@RequestMapping("/betty/admin")
public class BettyAdminController {

    private final ProductoBettyRepository productoRepo;
    private final FraganciaBettyRepository fragranciaRepo;
    private final PrecioPerfumeBettyRepository precioRepo;
    private final TripleABettyRepository tripleARepo;

    public BettyAdminController(ProductoBettyRepository productoRepo,
                                 FraganciaBettyRepository fragranciaRepo,
                                 PrecioPerfumeBettyRepository precioRepo,
                                 TripleABettyRepository tripleARepo) {
        this.productoRepo = productoRepo;
        this.fragranciaRepo = fragranciaRepo;
        this.precioRepo = precioRepo;
        this.tripleARepo = tripleARepo;
    }

    // -------------------------------------------------------
    //  Productos
    // -------------------------------------------------------

    @GetMapping("/productos")
    public String listaProductos(Model model) {
        model.addAttribute("productos", productoRepo.findAll());
        return "betty/admin/productos";
    }

    @PostMapping("/productos")
    public String guardarProducto(@RequestParam(required = false) Long id,
                                   @RequestParam String nombre,
                                   @RequestParam BigDecimal precio,
                                   RedirectAttributes redirect) {
        ProductoBetty p = id != null ? productoRepo.findById(id).orElseGet(ProductoBetty::new)
                : new ProductoBetty();
        p.setNombre(nombre.trim());
        p.setPrecio(precio);
        if (id == null) p.setActivo(true);
        productoRepo.save(p);
        redirect.addFlashAttribute("mensajeExito", "Producto guardado.");
        return "redirect:/betty/admin/productos";
    }

    @PostMapping("/productos/{id}/toggle")
    public String toggleProducto(@PathVariable Long id, RedirectAttributes redirect) {
        productoRepo.findById(id).ifPresent(p -> {
            p.setActivo(!p.isActivo());
            productoRepo.save(p);
        });
        redirect.addFlashAttribute("mensajeExito", "Estado actualizado.");
        return "redirect:/betty/admin/productos";
    }

    // -------------------------------------------------------
    //  Fragancias
    // -------------------------------------------------------

    @GetMapping("/fragancias")
    public String listaFragancias(Model model) {
        model.addAttribute("fragancias", fragranciaRepo.findAll());
        return "betty/admin/fragancias";
    }

    @PostMapping("/fragancias")
    public String guardarFragancia(@RequestParam(required = false) Long id,
                                    @RequestParam String nombre,
                                    RedirectAttributes redirect) {
        FraganciaBetty f = id != null ? fragranciaRepo.findById(id).orElseGet(FraganciaBetty::new)
                : new FraganciaBetty();
        f.setNombre(nombre.trim());
        if (id == null) f.setActivo(true);
        fragranciaRepo.save(f);
        redirect.addFlashAttribute("mensajeExito", "Fragancia guardada.");
        return "redirect:/betty/admin/fragancias";
    }

    @PostMapping("/fragancias/{id}/toggle")
    public String toggleFragancia(@PathVariable Long id, RedirectAttributes redirect) {
        fragranciaRepo.findById(id).ifPresent(f -> {
            f.setActivo(!f.isActivo());
            fragranciaRepo.save(f);
        });
        redirect.addFlashAttribute("mensajeExito", "Estado actualizado.");
        return "redirect:/betty/admin/fragancias";
    }

    // -------------------------------------------------------
    //  Precios de perfume
    // -------------------------------------------------------

    @GetMapping("/precios-perfume")
    public String listaPrecios(Model model) {
        model.addAttribute("precios", precioRepo.findAllByOrderByMlAscOnzasAsc());
        return "betty/admin/precios-perfume";
    }

    @PostMapping("/precios-perfume")
    public String guardarPrecio(@RequestParam Integer ml,
                                 @RequestParam BigDecimal onzas,
                                 @RequestParam BigDecimal precio,
                                 RedirectAttributes redirect) {
        PrecioPerfumeBetty p = precioRepo.findByMlAndOnzas(ml, onzas)
                .orElseGet(PrecioPerfumeBetty::new);
        p.setMl(ml);
        p.setOnzas(onzas);
        p.setPrecio(precio);
        precioRepo.save(p);
        redirect.addFlashAttribute("mensajeExito", "Precio guardado.");
        return "redirect:/betty/admin/precios-perfume";
    }

    @PostMapping("/precios-perfume/eliminar/{id}")
    public String eliminarPrecio(@PathVariable Long id, RedirectAttributes redirect) {
        precioRepo.deleteById(id);
        redirect.addFlashAttribute("mensajeExito", "Precio eliminado.");
        return "redirect:/betty/admin/precios-perfume";
    }

    // -------------------------------------------------------
    //  Triple A
    // -------------------------------------------------------

    @GetMapping("/triple-a")
    public String listaTripleA(Model model) {
        model.addAttribute("tripleA", tripleARepo.findAll());
        return "betty/admin/triple-a";
    }

    @PostMapping("/triple-a")
    public String guardarTripleA(@RequestParam(required = false) Long id,
                                  @RequestParam String nombre,
                                  @RequestParam BigDecimal precio,
                                  RedirectAttributes redirect) {
        TripleABetty t = id != null ? tripleARepo.findById(id).orElseGet(TripleABetty::new)
                : new TripleABetty();
        t.setNombre(nombre.trim());
        t.setPrecio(precio);
        if (id == null) t.setActivo(true);
        tripleARepo.save(t);
        redirect.addFlashAttribute("mensajeExito", "Producto Triple A guardado.");
        return "redirect:/betty/admin/triple-a";
    }

    @PostMapping("/triple-a/{id}/toggle")
    public String toggleTripleA(@PathVariable Long id, RedirectAttributes redirect) {
        tripleARepo.findById(id).ifPresent(t -> {
            t.setActivo(!t.isActivo());
            tripleARepo.save(t);
        });
        redirect.addFlashAttribute("mensajeExito", "Estado actualizado.");
        return "redirect:/betty/admin/triple-a";
    }
}
