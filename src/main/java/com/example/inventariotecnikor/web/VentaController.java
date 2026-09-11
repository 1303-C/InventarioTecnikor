package com.example.inventariotecnikor.web;

import com.example.inventariotecnikor.exception.StockInsuficienteException;
import com.example.inventariotecnikor.exception.TicketException;
import com.example.inventariotecnikor.model.FormaPago;
import com.example.inventariotecnikor.model.Producto;
import com.example.inventariotecnikor.model.Venta;
import com.example.inventariotecnikor.service.ProductoService;
import com.example.inventariotecnikor.service.VentaService;
import com.example.inventariotecnikor.service.ticket.TicketPrinter;
import com.example.inventariotecnikor.web.form.CobroForm;
import com.example.inventariotecnikor.web.venta.CarritoVenta;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Punto de venta.
 *
 *   GET  /ventas/nueva                 -> pantalla de venta (carrito en sesion)
 *   POST /ventas/nueva/lineas          -> agrega un producto (codigo escaneado)
 *   POST /ventas/nueva/lineas/{id}     -> cambia la cantidad de una linea
 *   POST /ventas/nueva/vaciar          -> vacia el carrito
 *   POST /ventas                       -> cobra: registra la Venta e imprime el ticket
 *   GET  /ventas/{id}                  -> detalle de una venta
 *   POST /ventas/{id}/reimprimir       -> vuelve a mandar el ticket
 *   GET  /ventas                       -> historial de un dia
 *
 * El carrito vive en {@link CarritoVenta} (sesion). La impresion del ticket
 * NO tumba la venta: si el papel no sale, la venta queda guardada y se
 * reimprime desde el detalle.
 */
@Controller
public class VentaController {

    private final ProductoService productoService;
    private final VentaService ventaService;
    private final CarritoVenta carrito;
    private final CorrectorTeclado correctorTeclado;
    private final TicketPrinter ticketPrinter;

    public VentaController(ProductoService productoService,
                           VentaService ventaService,
                           CarritoVenta carrito,
                           CorrectorTeclado correctorTeclado,
                           TicketPrinter ticketPrinter) {
        this.productoService = productoService;
        this.ventaService = ventaService;
        this.carrito = carrito;
        this.correctorTeclado = correctorTeclado;
        this.ticketPrinter = ticketPrinter;
    }

    // ------------------------------------------------------------------
    //  Pantalla de venta
    // ------------------------------------------------------------------

    @GetMapping("/ventas/nueva")
    public String nueva(Model model) {
        model.addAttribute("carrito", carrito);
        model.addAttribute("formasPago", FormaPago.values());
        if (!model.containsAttribute("cobro")) {
            model.addAttribute("cobro", new CobroForm());
        }
        return "ventas/nueva";
    }

    @PostMapping("/ventas/nueva/lineas")
    public String agregarLinea(@RequestParam String codigo, RedirectAttributes flash) {
        String limpio = codigo == null ? "" : codigo.trim();
        if (limpio.isEmpty()) {
            return "redirect:/ventas/nueva";
        }

        Producto producto = resolver(limpio);
        if (producto == null) {
            flash.addFlashAttribute("mensajeError",
                    "No hay ningun producto con el codigo \"" + limpio + "\".");
        } else if (!producto.isActivo()) {
            flash.addFlashAttribute("mensajeError",
                    "El producto \"" + producto.getNombre() + "\" esta inactivo.");
        } else if (producto.getPrecioVenta() == null) {
            flash.addFlashAttribute("mensajeError",
                    "El producto \"" + producto.getNombre() + "\" no tiene precio de venta.");
        } else {
            carrito.agregar(producto, 1);
            flash.addFlashAttribute("mensajeExito", "Agregado: " + producto.getNombre());
        }
        return "redirect:/ventas/nueva";
    }

    @PostMapping("/ventas/nueva/lineas/{productoId}")
    public String cambiarCantidad(@PathVariable Long productoId,
                                  @RequestParam int cantidad) {
        carrito.cambiarCantidad(productoId, cantidad);
        return "redirect:/ventas/nueva";
    }

    @PostMapping("/ventas/nueva/vaciar")
    public String vaciar() {
        carrito.vaciar();
        return "redirect:/ventas/nueva";
    }

    // ------------------------------------------------------------------
    //  Cobro
    // ------------------------------------------------------------------

    @PostMapping("/ventas")
    public String cobrar(@Valid @ModelAttribute("cobro") CobroForm cobro,
                         BindingResult errores,
                         RedirectAttributes flash) {

        if (carrito.isVacio()) {
            flash.addFlashAttribute("mensajeError", "El carrito esta vacio.");
            return "redirect:/ventas/nueva";
        }

        if (cobro.getFormaPago() == FormaPago.EFECTIVO
                && cobro.getMontoRecibido() == null) {
            errores.rejectValue("montoRecibido", "requerido",
                    "Indica con cuanto paga el cliente.");
        }
        if (cobro.getFormaPago() == FormaPago.EFECTIVO
                && cobro.getMontoRecibido() != null
                && cobro.getMontoRecibido().compareTo(carrito.getTotal()) < 0) {
            errores.rejectValue("montoRecibido", "insuficiente",
                    "El monto recibido no cubre el total.");
        }

        if (errores.hasErrors()) {
            flash.addFlashAttribute("org.springframework.validation.BindingResult.cobro", errores);
            flash.addFlashAttribute("cobro", cobro);
            flash.addFlashAttribute("mensajeError", "Revisa los datos del cobro.");
            return "redirect:/ventas/nueva";
        }

        Venta venta;
        try {
            venta = ventaService.registrar(
                    carrito.aLineasSolicitadas(),
                    cobro.getFormaPago(),
                    cobro.getMontoRecibido(),
                    cobro.getResponsable(),
                    cobro.getClienteNombre(),
                    cobro.getClienteDocumento());
        } catch (StockInsuficienteException | IllegalArgumentException ex) {
            flash.addFlashAttribute("mensajeError", ex.getMessage());
            return "redirect:/ventas/nueva";
        }

        carrito.vaciar();

        try {
            ticketPrinter.imprimir(venta);
            flash.addFlashAttribute("mensajeExito",
                    "Venta #" + venta.getNumero() + " registrada. Ticket enviado a la impresora.");
        } catch (TicketException ex) {
            flash.addFlashAttribute("mensajeError",
                    "Venta #" + venta.getNumero() + " registrada, pero el ticket no salio. "
                            + "Reimprimelo desde el detalle.");
        }
        return "redirect:/ventas/" + venta.getId();
    }

    // ------------------------------------------------------------------
    //  Detalle e historial
    // ------------------------------------------------------------------

    @GetMapping("/ventas/{id}")
    public String detalle(@PathVariable Long id, Model model) {
        model.addAttribute("venta", ventaService.obtenerPorId(id));
        return "ventas/detalle";
    }

    @PostMapping("/ventas/{id}/reimprimir")
    public String reimprimir(@PathVariable Long id, RedirectAttributes flash) {
        Venta venta = ventaService.obtenerPorId(id);
        try {
            ticketPrinter.imprimir(venta);
            flash.addFlashAttribute("mensajeExito", "Ticket de la venta #" + venta.getNumero() + " reenviado.");
        } catch (TicketException ex) {
            flash.addFlashAttribute("mensajeError", ex.getMessage());
        }
        return "redirect:/ventas/" + id;
    }

    @GetMapping("/ventas")
    public String historial(@RequestParam(required = false)
                            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
                            Model model) {
        LocalDate dia = fecha != null ? fecha : LocalDate.now();
        List<Venta> ventas = ventaService.ventasDelDia(dia);

        BigDecimal totalDia = ventas.stream()
                .map(Venta::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        model.addAttribute("ventas", ventas);
        model.addAttribute("fecha", dia);
        model.addAttribute("totalDia", totalDia);
        model.addAttribute("cantidadVentas", ventas.size());
        return "ventas/historial";
    }

    @GetMapping("/ventas/cierre")
    public String cierre(@RequestParam(required = false)
                         @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
                         Model model) {
        LocalDate dia = fecha != null ? fecha : LocalDate.now();
        model.addAttribute("cierre", ventaService.cierreDelDia(dia));
        return "ventas/cierre";
    }

    // ------------------------------------------------------------------

    /** Busca por codigo QR; si no aparece, reintenta corrigiendo la
     *  distribucion del teclado del lector (ver {@link CorrectorTeclado}). */
    private Producto resolver(String codigo) {
        return productoService.buscarPorQrOpcional(codigo)
                .or(() -> {
                    String corregido = correctorTeclado.comoUs(codigo);
                    return corregido.equals(codigo)
                            ? java.util.Optional.empty()
                            : productoService.buscarPorQrOpcional(corregido);
                })
                .orElse(null);
    }
}
