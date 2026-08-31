package com.example.inventariotecnikor.web;

import com.example.inventariotecnikor.model.Producto;
import com.example.inventariotecnikor.service.ProductoService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Optional;

/**
 * Pantalla de escaneo de codigo QR.
 *
 * Como funciona un lector USB de mano: Windows lo ve como un teclado. Al
 * escanear, "teclea" el contenido del codigo y al final manda un Enter, que
 * envia el formulario. Por eso la pantalla es solo un <input> con foco
 * automatico dentro de un <form method="get">. Tambien sirve para teclear
 * el codigo a mano.
 *
 * Flujo:
 *   GET /escanear                -> muestra el input vacio
 *   GET /escanear?codigo=XXXX    -> busca el producto:
 *        - existe   -> redirect a su ficha /productos/{id}
 *        - no existe -> vuelve al input con un aviso y un enlace para
 *                       crear el producto con ese codigo ya puesto
 */
@Controller
public class EscaneoController {

    private final ProductoService productoService;

    public EscaneoController(ProductoService productoService) {
        this.productoService = productoService;
    }

    @GetMapping("/escanear")
    public String escanear(@RequestParam(required = false) String codigo, Model model) {
        if (codigo == null || codigo.isBlank()) {
            return "escanear"; // primera visita: solo el input
        }

        String limpio = codigo.trim();
        Optional<Producto> encontrado = productoService.buscarPorQrOpcional(limpio);

        if (encontrado.isPresent()) {
            return "redirect:/productos/" + encontrado.get().getId();
        }

        // No hay ningun producto con ese codigo
        model.addAttribute("codigoNoEncontrado", limpio);
        return "escanear";
    }
}
