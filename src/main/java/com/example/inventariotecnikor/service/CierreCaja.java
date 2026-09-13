package com.example.inventariotecnikor.service;

import com.example.inventariotecnikor.model.FormaPago;
import com.example.inventariotecnikor.model.MovimientoCaja;
import com.example.inventariotecnikor.model.TipoLinea;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Resumen de caja de un rango de fechas (un solo dia si desde == hasta):
 * ventas por forma de pago y por linea de negocio, mas los movimientos de
 * caja (ingresos/egresos en efectivo que no son venta).
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
        /** Cuanto dejo cada linea de negocio (repuestos / alquiler / mantenimiento) en el rango. */
        List<PorTipoLinea> desglosePorTipoLinea,
        BigDecimal ingresosCaja,
        BigDecimal egresosCaja,
        BigDecimal efectivoEsperado,
        /** Neto de los movimientos generados por un arqueo dentro del rango (positivo = sobro, negativo = falto). */
        BigDecimal ajustesArqueo,
        List<MovimientoCaja> movimientosCaja) {

    /** Total y numero de ventas de una forma de pago concreta. */
    public record PorFormaPago(FormaPago formaPago, int cantidad, BigDecimal total) {
    }

    /** Total facturado en una linea de negocio concreta (repuestos, alquiler, mantenimiento). */
    public record PorTipoLinea(TipoLinea tipo, BigDecimal total) {
    }

    /** Total vendido en una forma de pago concreta (0 si no hubo ventas de ese tipo). */
    public BigDecimal totalDe(FormaPago formaPago) {
        return desglosePorFormaPago.stream()
                .filter(d -> d.formaPago() == formaPago)
                .map(PorFormaPago::total)
                .findFirst()
                .orElse(BigDecimal.ZERO.setScale(2));
    }

    /** Total de una linea de negocio (0 si no hubo movimiento de ese tipo). */
    public BigDecimal totalDe(TipoLinea tipo) {
        return desglosePorTipoLinea.stream()
                .filter(d -> d.tipo() == tipo)
                .map(PorTipoLinea::total)
                .findFirst()
                .orElse(BigDecimal.ZERO.setScale(2));
    }
}
