package com.example.inventariotecnikor.service;

import com.example.inventariotecnikor.model.FormaPago;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Resumen de un dia de ventas para el cierre de caja.
 *
 * "efectivo" es lo que deberia haber de mas en el cajon por las ventas del
 * dia: la suma del TOTAL de las ventas pagadas en efectivo (el cambio ya
 * salio del cajon, asi que lo que queda neto es el total de cada venta).
 * No incluye la base con la que se abrio la caja, que la app no lleva.
 */
public record CierreCaja(
        LocalDate fecha,
        int cantidadVentas,
        BigDecimal total,
        BigDecimal efectivo,
        List<PorFormaPago> desglose) {

    /** Total y numero de ventas de una forma de pago concreta. */
    public record PorFormaPago(FormaPago formaPago, int cantidad, BigDecimal total) {
    }
}
