package com.example.inventariotecnikor.web.betty;

import com.example.inventariotecnikor.model.PrecioPerfumeBetty;
import com.example.inventariotecnikor.model.VentaBetty;
import com.example.inventariotecnikor.repository.FraganciaBettyRepository;
import com.example.inventariotecnikor.repository.PrecioPerfumeBettyRepository;
import com.example.inventariotecnikor.repository.ProductoBettyRepository;
import com.example.inventariotecnikor.repository.TripleABettyRepository;
import com.example.inventariotecnikor.repository.VentaBettyRepository;
import com.example.inventariotecnikor.service.VentaBettyService;
import com.example.inventariotecnikor.service.ticket.BettyTicketBuilder;
import com.example.inventariotecnikor.service.ticket.TicketOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/betty")
public class BettyPosController {

    private static final Logger log = LoggerFactory.getLogger(BettyPosController.class);

    private final ProductoBettyRepository productoRepo;
    private final FraganciaBettyRepository fragranciaRepo;
    private final PrecioPerfumeBettyRepository precioRepo;
    private final TripleABettyRepository tripleARepo;
    private final VentaBettyRepository ventaRepo;
    private final VentaBettyService ventaService;
    private final CarritoBetty carrito;
    private final BettyTicketBuilder ticketBuilder;
    private final TicketOutput ticketOutput;

    public BettyPosController(ProductoBettyRepository productoRepo,
                               FraganciaBettyRepository fragranciaRepo,
                               PrecioPerfumeBettyRepository precioRepo,
                               TripleABettyRepository tripleARepo,
                               VentaBettyRepository ventaRepo,
                               VentaBettyService ventaService,
                               CarritoBetty carrito,
                               BettyTicketBuilder ticketBuilder,
                               TicketOutput ticketOutput) {
        this.productoRepo = productoRepo;
        this.fragranciaRepo = fragranciaRepo;
        this.precioRepo = precioRepo;
        this.tripleARepo = tripleARepo;
        this.ventaRepo = ventaRepo;
        this.ventaService = ventaService;
        this.carrito = carrito;
        this.ticketBuilder = ticketBuilder;
        this.ticketOutput = ticketOutput;
    }

    // -------------------------------------------------------
    //  POS
    // -------------------------------------------------------

    @GetMapping("/pos")
    public String pos(Model model) {
        model.addAttribute("productos", productoRepo.findByActivoTrueOrderByNombreAsc());
        model.addAttribute("fragancias", fragranciaRepo.findByActivoTrueOrderByNombreAsc());
        model.addAttribute("tripleA", tripleARepo.findByActivoTrueOrderByNombreAsc());
        model.addAttribute("mlDisponibles", precioRepo.findDistinctMlByOrderByMlAsc());
        // Leer el carrito en el controller (sesión activa aquí) y pasar datos planos
        var items = carrito.getItems();
        model.addAttribute("carritoItems", items);
        model.addAttribute("carritoTotal", carrito.getTotal());
        model.addAttribute("carritoVacio", items.isEmpty());
        return "betty/pos";
    }

    @PostMapping("/pos/agregar")
    public String agregar(@RequestParam String descripcion,
                           @RequestParam String precioUnit,
                           @RequestParam(defaultValue = "1") int cantidad) {
        BigDecimal precio = new BigDecimal(precioUnit.replace(",", ".").trim());
        carrito.agregar(new ItemCarritoBetty(descripcion.trim(), precio, cantidad));
        return "redirect:/betty/pos";
    }

    @PostMapping("/pos/agregar-perfume")
    public String agregarPerfume(@RequestParam String fragancia,
                                  @RequestParam int ml,
                                  @RequestParam BigDecimal onzas,
                                  RedirectAttributes redirect) {
        var precioOpt = precioRepo.findByMlAndOnzas(ml, onzas);
        if (precioOpt.isEmpty()) {
            redirect.addFlashAttribute("mensajeError",
                    "No se encontro precio para " + ml + "ml / " + onzas + "oz");
            return "redirect:/betty/pos";
        }
        PrecioPerfumeBetty precio = precioOpt.get();
        String descripcion = "Fragancia " + fragancia + " " + ml + "ml/" + onzas + "oz";
        carrito.agregar(new ItemCarritoBetty(descripcion, precio.getPrecio(), 1));
        return "redirect:/betty/pos";
    }

    @PostMapping("/pos/quitar/{index}")
    public String quitar(@PathVariable int index) {
        carrito.quitar(index);
        return "redirect:/betty/pos";
    }

    @GetMapping("/pos/onzas")
    @ResponseBody
    public List<PrecioPerfumeBetty> onzasPorMl(@RequestParam int ml) {
        return precioRepo.findByMlOrderByOnzasAsc(ml);
    }

    // -------------------------------------------------------
    //  Cobro
    // -------------------------------------------------------

    @PostMapping("/cobrar")
    public String cobrar(@RequestParam String formaPago, RedirectAttributes redirect) {
        if (carrito.isEmpty()) {
            redirect.addFlashAttribute("mensajeError", "El carrito esta vacio.");
            return "redirect:/betty/pos";
        }
        VentaBetty venta = ventaService.registrar(carrito.getItems(), formaPago);

        // Ticket
        try {
            boolean abrirCajon = "EFECTIVO".equals(formaPago);
            byte[] datos = ticketBuilder.construir(venta, abrirCajon);
            ticketOutput.enviar(datos);
        } catch (Exception e) {
            log.warn("No se pudo imprimir ticket Betty venta #{}: {}", venta.getNumero(), e.getMessage());
            redirect.addFlashAttribute("mensajeError",
                    "Venta registrada, pero hubo un problema al imprimir el ticket.");
        }

        carrito.vaciar();
        return "redirect:/betty/ventas/" + venta.getId();
    }

    // -------------------------------------------------------
    //  Historial y detalle
    // -------------------------------------------------------

    @GetMapping("/ventas")
    public String historial(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                             @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
                             Model model) {
        if (desde == null) desde = LocalDate.now();
        if (hasta == null) hasta = LocalDate.now();

        LocalDateTime desdeDt = desde.atStartOfDay();
        LocalDateTime hastaDt = hasta.plusDays(1).atStartOfDay();

        model.addAttribute("ventas", ventaRepo.findByFechaBetweenOrderByFechaDesc(desdeDt, hastaDt));
        model.addAttribute("desde", desde);
        model.addAttribute("hasta", hasta);
        return "betty/historial";
    }

    @GetMapping("/ventas/{id}")
    public String detalle(@PathVariable Long id, Model model) {
        model.addAttribute("venta", ventaService.obtenerPorId(id));
        return "betty/detalle-venta";
    }

    @PostMapping("/ventas/{id}/anular")
    public String anular(@PathVariable Long id,
                          @RequestParam String motivo,
                          RedirectAttributes redirect) {
        ventaService.anular(id, motivo);
        redirect.addFlashAttribute("mensajeExito", "Venta anulada correctamente.");
        return "redirect:/betty/ventas";
    }
}
