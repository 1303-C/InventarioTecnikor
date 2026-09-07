package com.example.inventariotecnikor.web;

import com.example.inventariotecnikor.model.Producto;
import com.example.inventariotecnikor.service.ProductoService;
import com.example.inventariotecnikor.service.QrService;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

/**
 * Impresion de etiquetas de producto (QR + datos).
 *
 * Fase actual (A1 + A2): genera el QR como imagen y una pagina maquetada al
 * tamano real de la etiqueta que se manda a imprimir por el driver de
 * Windows de la Bixolon (o cualquier impresora).
 *
 * Fase siguiente (A3): enviar ZPL directo a la Bixolon por red (IP:9100),
 * sin dialogo de impresion.
 */
@Controller
public class EtiquetaController {

    private final ProductoService productoService;
    private final QrService qrService;

    public EtiquetaController(ProductoService productoService, QrService qrService) {
        this.productoService = productoService;
        this.qrService = qrService;
    }

    /**
     * Imagen PNG del codigo QR de un producto. Se puede usar en cualquier
     * <img>: la ficha del producto, la pagina de etiquetas, etc.
     */
    @GetMapping(value = "/productos/{id}/qr.png", produces = MediaType.IMAGE_PNG_VALUE)
    @ResponseBody
    public byte[] qrDeProducto(@PathVariable Long id) {
        Producto p = productoService.obtenerPorId(id);
        return qrService.pngDeTexto(p.getCodigoQr(), 240);
    }

    /** Pantalla para elegir productos y cuantas etiquetas de cada uno. */
    @GetMapping("/etiquetas")
    public String seleccion(Model model) {
        model.addAttribute("productos", productoService.listarActivos());
        return "etiquetas/seleccion";
    }

    /**
     * Pagina lista para imprimir: cada producto repetido "copias" veces,
     * cada etiqueta con el tamano en mm que se indique (por defecto 50x30).
     * Se abre en una pestana nueva y lanza el dialogo de impresion sola.
     */
    @GetMapping("/etiquetas/imprimir")
    public String imprimir(@RequestParam(required = false) List<Long> ids,
                           @RequestParam(defaultValue = "1") int copias,
                           @RequestParam(defaultValue = "50") int ancho,
                           @RequestParam(defaultValue = "30") int alto,
                           Model model) {

        if (ids == null || ids.isEmpty()) {
            return "redirect:/etiquetas";
        }

        List<Producto> productos = ids.stream()
                .map(productoService::obtenerPorId)
                .toList();

        model.addAttribute("productos", productos);
        model.addAttribute("copias", Math.max(1, copias));
        model.addAttribute("ancho", Math.max(10, ancho));
        model.addAttribute("alto", Math.max(10, alto));
        return "etiquetas/imprimir";
    }
}
