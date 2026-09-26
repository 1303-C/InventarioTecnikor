package com.example.inventariotecnikor.service;

import com.example.inventariotecnikor.exception.RecursoNoEncontradoException;
import com.example.inventariotecnikor.model.LineaVentaBetty;
import com.example.inventariotecnikor.model.MovimientoCajaBetty;
import com.example.inventariotecnikor.model.VentaBetty;
import com.example.inventariotecnikor.repository.MovimientoCajaBettyRepository;
import com.example.inventariotecnikor.repository.VentaBettyRepository;
import com.example.inventariotecnikor.web.betty.ItemCarritoBetty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class VentaBettyService {

    private final VentaBettyRepository ventaRepo;
    private final MovimientoCajaBettyRepository cajaRepo;

    public VentaBettyService(VentaBettyRepository ventaRepo,
                              MovimientoCajaBettyRepository cajaRepo) {
        this.ventaRepo = ventaRepo;
        this.cajaRepo = cajaRepo;
    }

    @Transactional
    public VentaBetty registrar(List<ItemCarritoBetty> items, String formaPago) {
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("El carrito esta vacio.");
        }

        int numero = ventaRepo.findTopByOrderByNumeroDesc()
                .map(v -> v.getNumero() + 1)
                .orElse(1);

        VentaBetty venta = new VentaBetty(numero, formaPago);

        for (ItemCarritoBetty item : items) {
            LineaVentaBetty linea = new LineaVentaBetty(
                    item.getDescripcion(), item.getPrecioUnit(), item.getCantidad());
            venta.addLinea(linea);
        }
        venta.recalcularTotal();

        VentaBetty guardada = ventaRepo.save(venta);

        // Registrar movimiento de caja
        cajaRepo.save(new MovimientoCajaBetty(
                "INGRESO", guardada.getTotal(),
                "Venta #" + guardada.getNumero(),
                null));

        return guardada;
    }

    @Transactional
    public VentaBetty anular(Long id, String motivo) {
        VentaBetty venta = obtenerPorId(id);
        if ("ANULADA".equals(venta.getEstado())) {
            throw new IllegalStateException("La venta #" + venta.getNumero() + " ya esta anulada.");
        }
        venta.setEstado("ANULADA");
        venta.setMotivoAnulacion(motivo);
        venta.setFechaAnulacion(LocalDateTime.now());

        // Registrar egreso de caja por anulacion
        cajaRepo.save(new MovimientoCajaBetty(
                "EGRESO", venta.getTotal(),
                "Anulacion venta #" + venta.getNumero(),
                null));

        return venta;
    }

    public VentaBetty obtenerPorId(Long id) {
        return ventaRepo.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe la venta Betty con id: " + id));
    }
}
