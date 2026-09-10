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
 *
 * Ademas, si el codigo no aparece tal cual, se reintenta la busqueda
 * corrigiendo el desajuste de distribucion de teclado del lector
 * (ver {@link CorrectorTeclado}).
 */
@Controller
public class EscaneoController {

    private final ProductoService productoService;
    private final CorrectorTeclado correctorTeclado;

    public EscaneoController(ProductoService productoService,
                            CorrectorTeclado correctorTeclado) {
        this.productoService = productoService;
        this.correctorTeclado = correctorTeclado;
    }

    @GetMapping("/escanear")
    public String escanear(@RequestParam(required = false) String codigo, Model model) {
        if (codigo == null || codigo.isBlank()) {
            return "escanear"; // primera visita: solo el input
        }

        String limpio = codigo.trim();

        // 1) Busqueda normal, con el codigo tal cual llego.
        Optional<Producto> encontrado = productoService.buscarPorQrOpcional(limpio);

        // 2) Fallback: lector en distribucion US con Windows en espanol.
        //    Se reintenta deshaciendo ese cambio (' -> -, - -> /, ...).
        //    Solo entra si la correccion cambia algo y solo se usa si
        //    encuentra producto, asi nunca devuelve uno equivocado.
        if (encontrado.isEmpty()) {
            String reinterpretado = correctorTeclado.comoUs(limpio);
            if (!reinterpretado.equals(limpio)) {
                encontrado = productoService.buscarPorQrOpcional(reinterpretado);
            }
        }

        if (encontrado.isPresent()) {
            return "redirect:/productos/" + encontrado.get().getId();
        }

        // No hay ningun producto con ese codigo (ni corrigiendo la distribucion).
        // Se muestra el codigo tal cual se escaneo, que es lo que el usuario ve.
        model.addAttribute("codigoNoEncontrado", limpio);
        return "escanear";
    }
}
