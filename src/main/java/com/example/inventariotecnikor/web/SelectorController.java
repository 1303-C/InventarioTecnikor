package com.example.inventariotecnikor.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Página de selección de negocio. Es la raíz del sistema.
 */
@Controller
public class SelectorController {

    @GetMapping("/")
    public String selector() {
        return "selector";
    }
}
