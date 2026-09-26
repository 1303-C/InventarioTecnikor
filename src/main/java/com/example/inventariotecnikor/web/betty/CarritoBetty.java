package com.example.inventariotecnikor.web.betty;

import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Carrito de venta de Betty. Vive en la sesion del navegador.
 * Cada item es descripcion libre + precio + cantidad.
 */
@Component
@SessionScope
public class CarritoBetty {

    private final List<ItemCarritoBetty> items = new ArrayList<>();

    public void agregar(ItemCarritoBetty item) {
        items.add(item);
    }

    public void quitar(int index) {
        if (index >= 0 && index < items.size()) {
            items.remove(index);
        }
    }

    public void vaciar() {
        items.clear();
    }

    public List<ItemCarritoBetty> getItems() {
        return new ArrayList<>(items);
    }

    public BigDecimal getTotal() {
        return items.stream()
                .map(ItemCarritoBetty::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public int size() {
        return items.size();
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }
}
