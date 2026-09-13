package com.example.inventariotecnikor.web.venta;

import com.example.inventariotecnikor.model.Lavadora;
import com.example.inventariotecnikor.model.Producto;
import com.example.inventariotecnikor.model.TipoLinea;
import com.example.inventariotecnikor.service.VentaService;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Carrito de la venta en curso. Vive en la sesion del navegador (un carrito
 * por cajero/pestana), no en la base de datos: solo al pulsar "Cobrar" se
 * convierte en una Venta.
 *
 * Dos tipos de contenido:
 *  - "items": repuestos escaneados (agrupados por producto: escanear el
 *    mismo dos veces suma cantidad en la misma linea).
 *  - "libres": alquiler de lavadora o mantenimiento, cada uno su propia
 *    linea (no se agrupan: "visita" y "mano de obra" son cosas distintas
 *    aunque sean del mismo tipo).
 *
 * El precio que guarda cada linea es para pintar el total en pantalla; el
 * precio que se cobra de verdad lo vuelve a leer VentaService (del producto,
 * o el que se escribio aqui para las libres) al registrar la venta.
 */
@Component
@SessionScope
public class CarritoVenta {

    /** Una linea de producto del carrito. Mutable solo en la cantidad. */
    public static class Item {
        private final Long productoId;
        private final String codigoQr;
        private final String descripcion;
        private final BigDecimal precioUnitario;
        private int cantidad;

        Item(Producto producto, int cantidad) {
            this.productoId = producto.getId();
            this.codigoQr = producto.getCodigoQr();
            this.descripcion = producto.getNombre();
            this.precioUnitario = producto.getPrecioVenta();
            this.cantidad = cantidad;
        }

        public Long getProductoId() {
            return productoId;
        }

        public String getCodigoQr() {
            return codigoQr;
        }

        public String getDescripcion() {
            return descripcion;
        }

        public BigDecimal getPrecioUnitario() {
            return precioUnitario;
        }

        public int getCantidad() {
            return cantidad;
        }

        public BigDecimal getImporte() {
            return precioUnitario.multiply(BigDecimal.valueOf(cantidad));
        }
    }

    /** Una linea libre: alquiler (por horas, atada a una lavadora) o mantenimiento (monto libre). */
    public static class LineaLibre {
        private final Long id; // local al carrito, solo para poder quitarla
        private final TipoLinea tipo;
        private final String descripcion;
        private final int cantidad;
        private final BigDecimal precioUnitario;
        private final Long lavadoraId;
        private final String clienteAlquiler;

        LineaLibre(Long id, TipoLinea tipo, String descripcion, int cantidad, BigDecimal precioUnitario,
                  Long lavadoraId, String clienteAlquiler) {
            this.id = id;
            this.tipo = tipo;
            this.descripcion = descripcion;
            this.cantidad = cantidad;
            this.precioUnitario = precioUnitario;
            this.lavadoraId = lavadoraId;
            this.clienteAlquiler = clienteAlquiler;
        }

        public Long getId() {
            return id;
        }

        public TipoLinea getTipo() {
            return tipo;
        }

        public String getDescripcion() {
            return descripcion;
        }

        public int getCantidad() {
            return cantidad;
        }

        public BigDecimal getPrecioUnitario() {
            return precioUnitario;
        }

        public BigDecimal getImporte() {
            return precioUnitario.multiply(BigDecimal.valueOf(cantidad));
        }
    }

    // clave = productoId, para agrupar escaneos repetidos del mismo producto.
    // LinkedHashMap conserva el orden en que se fueron agregando.
    private final Map<Long, Item> items = new LinkedHashMap<>();
    private final List<LineaLibre> libres = new ArrayList<>();
    private final AtomicLong siguienteIdLibre = new AtomicLong(1);

    // --- Repuestos ---

    /** Agrega el producto; si ya estaba, le suma la cantidad. */
    public void agregar(Producto producto, int cantidad) {
        if (cantidad <= 0) {
            return;
        }
        Item existente = items.get(producto.getId());
        if (existente != null) {
            existente.cantidad += cantidad;
        } else {
            items.put(producto.getId(), new Item(producto, cantidad));
        }
    }

    /** Fija la cantidad de una linea. Cantidad 0 o negativa = quitar la linea. */
    public void cambiarCantidad(Long productoId, int cantidad) {
        if (cantidad <= 0) {
            items.remove(productoId);
            return;
        }
        Item item = items.get(productoId);
        if (item != null) {
            item.cantidad = cantidad;
        }
    }

    public void quitar(Long productoId) {
        items.remove(productoId);
    }

    public Collection<Item> getItems() {
        return new ArrayList<>(items.values());
    }

    // --- Alquiler / mantenimiento ---

    /** Agrega una hora de alquiler de una lavadora concreta, a la tarifa de su plan. */
    public void agregarAlquiler(Lavadora lavadora, int horas, String cliente) {
        if (horas <= 0) {
            throw new IllegalArgumentException("Las horas deben ser mayores que cero.");
        }
        String clienteLimpio = limpiar(cliente);
        String descripcion = "Alquiler " + lavadora.getPlan().getNombre() + " (" + lavadora.getCodigo() + ")"
                + (clienteLimpio != null ? " - " + clienteLimpio : "");
        libres.add(new LineaLibre(siguienteIdLibre.getAndIncrement(), TipoLinea.ALQUILER,
                descripcion, horas, lavadora.getPlan().getTarifaPorHora(), lavadora.getId(), clienteLimpio));
    }

    /** Agrega un cargo de mantenimiento a monto libre (mano de obra, visita, etc.). */
    public void agregarServicio(String descripcion, BigDecimal monto) {
        if (descripcion == null || descripcion.isBlank()) {
            throw new IllegalArgumentException("El servicio necesita una descripcion.");
        }
        if (monto == null || monto.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El monto del servicio debe ser mayor que cero.");
        }
        libres.add(new LineaLibre(siguienteIdLibre.getAndIncrement(), TipoLinea.MANTENIMIENTO,
                descripcion.trim(), 1, monto, null, null));
    }

    public void quitarLibre(Long id) {
        libres.removeIf(l -> l.getId().equals(id));
    }

    public List<LineaLibre> getLibres() {
        return new ArrayList<>(libres);
    }

    // --- Comun ---

    public void vaciar() {
        items.clear();
        libres.clear();
    }

    public boolean isVacio() {
        return items.isEmpty() && libres.isEmpty();
    }

    public int getTotalUnidades() {
        return items.values().stream().mapToInt(Item::getCantidad).sum();
    }

    public BigDecimal getTotal() {
        BigDecimal total = items.values().stream()
                .map(Item::getImporte)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return libres.stream()
                .map(LineaLibre::getImporte)
                .reduce(total, BigDecimal::add);
    }

    /** Traduce las lineas de producto a lo que espera VentaService.registrar(...). */
    public List<VentaService.LineaSolicitada> aLineasSolicitadas() {
        return items.values().stream()
                .map(i -> new VentaService.LineaSolicitada(i.productoId, i.cantidad))
                .toList();
    }

    /** Traduce las lineas libres a lo que espera VentaService.registrar(...). */
    public List<VentaService.LineaLibreSolicitada> aLineasLibresSolicitadas() {
        return libres.stream()
                .map(l -> new VentaService.LineaLibreSolicitada(
                        l.tipo, l.descripcion, l.cantidad, l.precioUnitario, l.lavadoraId, l.clienteAlquiler))
                .toList();
    }

    private static String limpiar(String texto) {
        return (texto == null || texto.isBlank()) ? null : texto.trim();
    }
}
