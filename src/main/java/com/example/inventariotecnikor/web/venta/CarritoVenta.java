package com.example.inventariotecnikor.web.venta;

import com.example.inventariotecnikor.model.Producto;
import com.example.inventariotecnikor.service.VentaService;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Carrito de la venta en curso. Vive en la sesion del navegador (un carrito
 * por cajero/pestana), no en la base de datos: solo al pulsar "Cobrar" se
 * convierte en una Venta.
 *
 * El precio que guarda cada linea es para pintar el total en pantalla; el
 * precio que se cobra de verdad lo vuelve a leer VentaService del producto
 * al registrar la venta.
 */
@Component
@SessionScope
public class CarritoVenta {

    /** Una linea del carrito. Mutable solo en la cantidad. */
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

    // clave = productoId, para agrupar escaneos repetidos del mismo producto.
    // LinkedHashMap conserva el orden en que se fueron agregando.
    private final Map<Long, Item> items = new LinkedHashMap<>();

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

    public void vaciar() {
        items.clear();
    }

    public boolean isVacio() {
        return items.isEmpty();
    }

    public Collection<Item> getItems() {
        return new ArrayList<>(items.values());
    }

    public int getTotalUnidades() {
        return items.values().stream().mapToInt(Item::getCantidad).sum();
    }

    public BigDecimal getTotal() {
        return items.values().stream()
                .map(Item::getImporte)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Traduce el carrito a lo que espera VentaService.registrar(...). */
    public List<VentaService.LineaSolicitada> aLineasSolicitadas() {
        return items.values().stream()
                .map(i -> new VentaService.LineaSolicitada(i.productoId, i.cantidad))
                .toList();
    }
}
