package com.example.inventariotecnikor.web;

import com.example.inventariotecnikor.service.MovimientoService;
import com.example.inventariotecnikor.service.ProductoService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Pantalla de inicio: resumen del inventario.
 *
 * @Controller (no @RestController): cada metodo devuelve el NOMBRE de una
 * plantilla Thymeleaf (un String), no JSON. Spring busca
 * src/main/resources/templates/<nombre>.html y lo renderiza.
 *
 * El "Model" es un mapa de datos que viaja del controlador a la vista;
 * en la plantilla se leen con ${nombreAtributo}. Es parecido al ViewBag /
 * ViewData de ASP.NET MVC.
 */
@Controller
public class DashboardController {

    private final ProductoService productoService;
    private final MovimientoService movimientoService;

    public DashboardController(ProductoService productoService,
                              MovimientoService movimientoService) {
        this.productoService = productoService;
        this.movimientoService = movimientoService;
    }

    @GetMapping("/")
    public String dashboard(Model model) {
        var stockBajo = productoService.conStockBajo();

        model.addAttribute("stockBajo", stockBajo);
        model.addAttribute("cantidadStockBajo", stockBajo.size());
        model.addAttribute("totalActivos", productoService.listarActivos().size());
        model.addAttribute("ultimosMovimientos", movimientoService.ultimos(0, 10).getContent());

        return "dashboard"; // -> templates/dashboard.html
    }
}
