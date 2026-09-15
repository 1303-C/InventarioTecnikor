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

import java.util.ArrayList;
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
     * cada etiqueta con el tamano en mm que se indique (por defecto 25x18,
     * el tamano real del rollo de la Bixolon en el mostrador).
     * Se abre en una pestana nueva y lanza el dialogo de impresion sola.
     *
     * "porFila" es cuantas etiquetas trae el rollo una al lado de la otra
     * (1 = una sola columna, como antes; 3 = el rollo actual del mostrador,
     * con "margenMm" a cada borde y "espacioMm" entre etiquetas). El
     * cabezal de la impresora es una barra fija de ancho completo: si el
     * rollo trae varias columnas, hay que mandarle TODAS juntas en una
     * misma "pagina" del ancho total, o las de los costados quedan en
     * blanco (eso es justo lo que se ve si porFila=1 en un rollo de 3).
     */
    @GetMapping("/etiquetas/imprimir")
    public String imprimir(@RequestParam(required = false) List<Long> ids,
                           @RequestParam(defaultValue = "1") int copias,
                           @RequestParam(defaultValue = "25") int ancho,
                           @RequestParam(defaultValue = "18") int alto,
                           @RequestParam(defaultValue = "1") int porFila,
                           @RequestParam(defaultValue = "0") double margenMm,
                           @RequestParam(defaultValue = "0") double espacioMm,
                           Model model) {

        if (ids == null || ids.isEmpty()) {
            return "redirect:/etiquetas";
        }

        int anchoValido = Math.max(10, ancho);
        int altoValido = Math.max(10, alto);
        int filaValida = Math.max(1, porFila);
        int copiasValidas = Math.max(1, copias);

        // Expande a la lista plana de unidades (cada producto repetido
        // "copias" veces) y la agrupa de a "porFila" para armar las filas
        // fisicas del rollo. Si el total no es multiplo de porFila, la
        // ultima fila queda con menos celdas (las que sobran del rollo
        // quedan en blanco, no hay forma de evitarlo).
        List<Producto> unidades = new ArrayList<>();
        for (Long id : ids) {
            Producto producto = productoService.obtenerPorId(id);
            for (int i = 0; i < copiasValidas; i++) {
                unidades.add(producto);
            }
        }
        List<List<Producto>> filas = new ArrayList<>();
        for (int i = 0; i < unidades.size(); i += filaValida) {
            filas.add(unidades.subList(i, Math.min(i + filaValida, unidades.size())));
        }

        model.addAttribute("filas", filas);
        model.addAttribute("cantidadUnidades", unidades.size());
        model.addAttribute("copias", copiasValidas);
        model.addAttribute("ancho", anchoValido);
        model.addAttribute("alto", altoValido);
        model.addAttribute("porFila", filaValida);
        model.addAttribute("margenMm", margenMm);
        model.addAttribute("espacioMm", espacioMm);
        return "etiquetas/imprimir";
    }
}
