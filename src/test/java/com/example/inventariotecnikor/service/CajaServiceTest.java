package com.example.inventariotecnikor.service;

import com.example.inventariotecnikor.model.Categoria;
import com.example.inventariotecnikor.model.EstadoVenta;
import com.example.inventariotecnikor.model.FormaPago;
import com.example.inventariotecnikor.model.LineaVenta;
import com.example.inventariotecnikor.model.MovimientoCaja;
import com.example.inventariotecnikor.model.Producto;
import com.example.inventariotecnikor.model.TipoMovimientoCaja;
import com.example.inventariotecnikor.model.Venta;
import com.example.inventariotecnikor.repository.MovimientoCajaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Cierre de caja (ventas + movimientos manuales) y registro de
 * ingresos/egresos/arqueo. VentaService y el repositorio de movimientos de
 * caja van mockeados.
 */
@ExtendWith(MockitoExtension.class)
class CajaServiceTest {

    @Mock
    VentaService ventaService;

    @Mock
    MovimientoCajaRepository movimientoCajaRepository;

    @InjectMocks
    CajaService cajaService;

    private Producto bomba;
    private final LocalDate desde = LocalDate.of(2026, 9, 1);
    private final LocalDate hasta = LocalDate.of(2026, 9, 10);

    @BeforeEach
    void setUp() {
        bomba = new Producto("LAV-001", "Bomba de agua", Categoria.LAVADORA, 1);
        bomba.setPrecioVenta(new BigDecimal("100.00"));
    }

    private Venta ventaCon(long numero, FormaPago formaPago, String precio) {
        Venta v = new Venta(numero, formaPago, "Caja");
        v.addLinea(new LineaVenta(bomba, 1, new BigDecimal(precio), 0));
        v.recalcularTotales();
        v.registrarPago(formaPago, formaPago.esEfectivo() ? new BigDecimal("999999") : null);
        return v;
    }

    private static void set(Object destino, String campo, Object valor) {
        try {
            var f = destino.getClass().getDeclaredField(campo);
            f.setAccessible(true);
            f.set(destino, valor);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    private void sinVentasNiMovimientos() {
        when(ventaService.ventasEnRango(desde, hasta)).thenReturn(List.of());
        when(movimientoCajaRepository.findByFechaBetweenOrderByFechaDesc(any(), any())).thenReturn(List.of());
    }

    // ------------------------------------------------------------------
    //  Registrar ingreso / egreso
    // ------------------------------------------------------------------

    @Test
    void registrarIngreso_guarda_el_movimiento() {
        when(movimientoCajaRepository.save(any(MovimientoCaja.class))).thenAnswer(inv -> inv.getArgument(0));

        MovimientoCaja m = cajaService.registrarIngreso(new BigDecimal("50000"), "  abono cliente  ", "  Ana  ");

        assertThat(m.getTipo()).isEqualTo(TipoMovimientoCaja.INGRESO);
        assertThat(m.getMonto()).isEqualByComparingTo("50000.00");
        assertThat(m.getMotivo()).isEqualTo("abono cliente");
        assertThat(m.getResponsable()).isEqualTo("Ana");
    }

    @Test
    void registrarEgreso_guarda_el_movimiento() {
        when(movimientoCajaRepository.save(any(MovimientoCaja.class))).thenAnswer(inv -> inv.getArgument(0));

        MovimientoCaja m = cajaService.registrarEgreso(new BigDecimal("20000"), "pago proveedor", null);

        assertThat(m.getTipo()).isEqualTo(TipoMovimientoCaja.EGRESO);
        assertThat(m.getMonto()).isEqualByComparingTo("20000.00");
        assertThat(m.getResponsable()).isNull();
    }

    @Test
    void registrar_con_monto_no_positivo_lanza_IllegalArgument() {
        assertThatThrownBy(() -> cajaService.registrarIngreso(BigDecimal.ZERO, "x", "y"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> cajaService.registrarEgreso(new BigDecimal("-5"), "x", "y"))
                .isInstanceOf(IllegalArgumentException.class);

        verify(movimientoCajaRepository, never()).save(any());
    }

    @Test
    void registrar_sin_motivo_lanza_IllegalArgument() {
        assertThatThrownBy(() -> cajaService.registrarIngreso(new BigDecimal("100"), "   ", "y"))
                .isInstanceOf(IllegalArgumentException.class);

        verify(movimientoCajaRepository, never()).save(any());
    }

    // ------------------------------------------------------------------
    //  Cierre
    // ------------------------------------------------------------------

    @Test
    void cierre_agrupa_ventas_y_suma_los_movimientos_de_caja() {
        when(ventaService.ventasEnRango(desde, hasta)).thenReturn(List.of(
                ventaCon(1, FormaPago.EFECTIVO, "100.00"),
                ventaCon(2, FormaPago.EFECTIVO, "50.00"),
                ventaCon(3, FormaPago.TARJETA, "200.00"),
                ventaCon(4, FormaPago.TRANSFERENCIA, "30.00")));
        when(movimientoCajaRepository.findByFechaBetweenOrderByFechaDesc(any(), any())).thenReturn(List.of(
                new MovimientoCaja(TipoMovimientoCaja.INGRESO, new BigDecimal("20.00"), "abono", null),
                new MovimientoCaja(TipoMovimientoCaja.EGRESO, new BigDecimal("10.00"), "gasto", null)));

        CierreCaja cierre = cajaService.cierre(desde, hasta);

        assertThat(cierre.cantidadVentas()).isEqualTo(4);
        assertThat(cierre.totalVentas()).isEqualByComparingTo("380.00");
        assertThat(cierre.ingresosCaja()).isEqualByComparingTo("20.00");
        assertThat(cierre.egresosCaja()).isEqualByComparingTo("10.00");
        // efectivo de ventas (150) + ingresos (20) - egresos (10) = 160
        assertThat(cierre.efectivoEsperado()).isEqualByComparingTo("160.00");
        assertThat(cierre.totalDe(FormaPago.TARJETA)).isEqualByComparingTo("200.00");
        assertThat(cierre.totalDe(FormaPago.TRANSFERENCIA)).isEqualByComparingTo("30.00");
    }

    @Test
    void cierre_no_cuenta_las_ventas_anuladas() {
        Venta anulada = ventaCon(9, FormaPago.TARJETA, "500.00");
        set(anulada, "estado", EstadoVenta.ANULADA);

        when(ventaService.ventasEnRango(desde, hasta)).thenReturn(List.of(
                ventaCon(1, FormaPago.TARJETA, "200.00"), anulada));
        when(movimientoCajaRepository.findByFechaBetweenOrderByFechaDesc(any(), any())).thenReturn(List.of());

        CierreCaja cierre = cajaService.cierre(desde, hasta);

        assertThat(cierre.cantidadVentas()).isEqualTo(1);
        assertThat(cierre.totalVentas()).isEqualByComparingTo("200.00");
    }

    @Test
    void cierre_sin_datos_da_ceros_y_lista_las_tres_formas_de_pago() {
        sinVentasNiMovimientos();

        CierreCaja cierre = cajaService.cierre(desde, hasta);

        assertThat(cierre.cantidadVentas()).isZero();
        assertThat(cierre.totalVentas()).isEqualByComparingTo("0.00");
        assertThat(cierre.efectivoEsperado()).isEqualByComparingTo("0.00");
        assertThat(cierre.desglosePorFormaPago()).hasSize(3);
    }

    @Test
    void cierre_con_hasta_anterior_a_desde_lanza_IllegalArgument() {
        assertThatThrownBy(() -> cajaService.cierre(hasta, desde))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ------------------------------------------------------------------
    //  Arqueo
    // ------------------------------------------------------------------

    @Test
    void arqueo_sin_diferencia_no_registra_nada() {
        sinVentasNiMovimientos();

        Optional<MovimientoCaja> resultado = cajaService.arquear(desde, hasta, BigDecimal.ZERO, "Ana");

        assertThat(resultado).isEmpty();
        verify(movimientoCajaRepository, never()).save(any());
    }

    @Test
    void arqueo_con_sobrante_registra_un_ingreso() {
        sinVentasNiMovimientos(); // esperado = 0
        when(movimientoCajaRepository.save(any(MovimientoCaja.class))).thenAnswer(inv -> inv.getArgument(0));

        MovimientoCaja m = cajaService.arquear(desde, hasta, new BigDecimal("15000"), "Ana").orElseThrow();

        assertThat(m.getTipo()).isEqualTo(TipoMovimientoCaja.INGRESO);
        assertThat(m.getMonto()).isEqualByComparingTo("15000.00");
        assertThat(m.getMotivo()).contains("Arqueo");
    }

    @Test
    void arqueo_con_faltante_registra_un_egreso() {
        when(ventaService.ventasEnRango(desde, hasta)).thenReturn(List.of(ventaCon(1, FormaPago.EFECTIVO, "100.00")));
        when(movimientoCajaRepository.findByFechaBetweenOrderByFechaDesc(any(), any())).thenReturn(List.of());
        when(movimientoCajaRepository.save(any(MovimientoCaja.class))).thenAnswer(inv -> inv.getArgument(0));

        // esperado = 100.00, contado = 90 -> faltan 10
        MovimientoCaja m = cajaService.arquear(desde, hasta, new BigDecimal("90"), "Ana").orElseThrow();

        assertThat(m.getTipo()).isEqualTo(TipoMovimientoCaja.EGRESO);
        assertThat(m.getMonto()).isEqualByComparingTo("10.00");
    }

    @Test
    void arqueo_con_contado_negativo_lanza_IllegalArgument() {
        assertThatThrownBy(() -> cajaService.arquear(desde, hasta, new BigDecimal("-1"), "Ana"))
                .isInstanceOf(IllegalArgumentException.class);

        verify(movimientoCajaRepository, never()).save(any());
    }
}
