package com.example.inventariotecnikor.service;

import com.example.inventariotecnikor.model.FormaPago;
import com.example.inventariotecnikor.model.MovimientoCaja;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Resumen de caja de un rango de fechas (un solo dia si desde == hasta):
 * ventas por forma de pago mas los movimientos de caja (ingresos/egresos en
 * efectivo que no son venta).
 *
 * "efectivoEsperado" es lo que deberia haber en el cajon por ese rango:
 * ventas en efectivo + ingresos - egresos. El cambio de cada venta ya salio
 * del cajon, asi que lo que queda neto de cada venta es su TOTAL. No incluye
 * ninguna base anterior a "desde" que no se haya registrado como ingreso.
 */
public record CierreCaja(
        LocalDate desde,
        LocalDate hasta,
        int cantidadVentas,
        BigDecimal totalVentas,
        List<PorFormaPago> desglosePorFormaPago,
        BigDecimal ingresosCaja,
        BigDecimal egresosCaja,
        BigDecimal efectivoEsperado,
        List<MovimientoCaja> movimientosCaja) {

    /** Total y numero de ventas de una forma de pago concreta. */
    public record PorFormaPago(FormaPago formaPago, int cantidad, BigDecimal total) {
    }

    /** Total vendido en una forma de pago concreta (0 si no hubo ventas de ese tipo). */
    public BigDecimal totalDe(FormaPago formaPago) {
        return desglosePorFormaPago.stream()
                .filter(d -> d.formaPago() == formaPago)
                .map(PorFormaPago::total)
                .findFirst()
                .orElse(BigDecimal.ZERO.setScale(2));
    }
}
