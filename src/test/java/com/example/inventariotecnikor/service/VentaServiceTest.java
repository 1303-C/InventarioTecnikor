package com.example.inventariotecnikor.service;

import com.example.inventariotecnikor.exception.RecursoNoEncontradoException;
import com.example.inventariotecnikor.exception.StockInsuficienteException;
import com.example.inventariotecnikor.model.Categoria;
import com.example.inventariotecnikor.model.FormaPago;
import com.example.inventariotecnikor.model.Lavadora;
import com.example.inventariotecnikor.model.PlanAlquiler;
import com.example.inventariotecnikor.model.Producto;
import com.example.inventariotecnikor.model.TipoLinea;
import com.example.inventariotecnikor.model.Venta;
import com.example.inventariotecnikor.repository.ProductoRepository;
import com.example.inventariotecnikor.repository.VentaRepository;
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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Reglas de negocio del registro de ventas: repuestos, alquiler de
 * lavadoras y mantenimiento. Repositorios, MovimientoService y
 * AlquilerService van mockeados: no se levanta Spring ni se toca la BD.
 */
@ExtendWith(MockitoExtension.class)
class VentaServiceTest {

    @Mock
    VentaRepository ventaRepository;

    @Mock
    ProductoRepository productoRepository;

    @Mock
    MovimientoService movimientoService;

    @Mock
    AlquilerService alquilerService;

    @InjectMocks
    VentaService ventaService;

    private Producto bomba;   // id 1, precio 100.00
    private Producto correa;  // id 2, precio 25.50

    @BeforeEach
    void setUp() {
        bomba = new Producto("LAV-001", "Bomba de agua", Categoria.LAVADORA, 2);
        bomba.setPrecioVenta(new BigDecimal("100.00"));
        correa = new Producto("LAV-002", "Correa", Categoria.LAVADORA, 5);
        correa.setPrecioVenta(new BigDecimal("25.50"));
    }

    private void stubBomba() {
        when(productoRepository.findById(1L)).thenReturn(Optional.of(bomba));
    }

    private void stubCorrea() {
        when(productoRepository.findById(2L)).thenReturn(Optional.of(correa));
    }

    private void stubRepoVentas() {
        when(ventaRepository.ultimoNumero()).thenReturn(0L);
        when(ventaRepository.save(any(Venta.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private List<VentaService.LineaSolicitada> carrito(long id1, int c1, long id2, int c2) {
        return List.of(new VentaService.LineaSolicitada(id1, c1),
                new VentaService.LineaSolicitada(id2, c2));
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

    private static Lavadora lavadora(long id, String codigo) {
        PlanAlquiler plan = new PlanAlquiler("Pequena", new BigDecimal("5000.00"));
        Lavadora l = new Lavadora(codigo, plan);
        set(l, "id", id);
        return l;
    }

    // ------------------------------------------------------------------
    //  Camino feliz (repuestos)
    // ------------------------------------------------------------------

    @Test
    void registra_la_venta_con_lineas_y_totales() {
        stubBomba();
        stubCorrea();
        stubRepoVentas();

        Venta venta = ventaService.registrar(
                carrito(1L, 2, 2L, 3), List.of(), FormaPago.TARJETA, null, "Carlos", null, null);

        assertThat(venta.getNumero()).isEqualTo(1L);
        assertThat(venta.getLineas()).hasSize(2);
        // 2 * 100.00 + 3 * 25.50 = 276.50
        assertThat(venta.getSubtotal()).isEqualByComparingTo("276.50");
        assertThat(venta.getTotalIva()).isEqualByComparingTo("0.00");
        assertThat(venta.getTotal()).isEqualByComparingTo("276.50");
        assertThat(venta.getTotalUnidades()).isEqualTo(5);
    }

    @Test
    void las_lineas_congelan_descripcion_y_precio_del_producto() {
        stubBomba();
        stubRepoVentas();

        Venta venta = ventaService.registrar(
                List.of(new VentaService.LineaSolicitada(1L, 2)), List.of(),
                FormaPago.TARJETA, null, "Carlos", null, null);

        var linea = venta.getLineas().get(0);
        assertThat(linea.getTipo()).isEqualTo(TipoLinea.PRODUCTO);
        assertThat(linea.getDescripcion()).isEqualTo("Bomba de agua");
        assertThat(linea.getCodigoQr()).isEqualTo("LAV-001");
        assertThat(linea.getPrecioUnitario()).isEqualByComparingTo("100.00");
        assertThat(linea.getImporte()).isEqualByComparingTo("200.00");
    }

    @Test
    void descuenta_stock_con_una_salida_por_linea_y_motivo_Venta_N() {
        stubBomba();
        stubCorrea();
        stubRepoVentas();

        ventaService.registrar(carrito(1L, 2, 2L, 3), List.of(),
                FormaPago.TARJETA, null, "Carlos", null, null);

        verify(movimientoService).registrarSalida(1L, 2, "Venta #1", "Carlos");
        verify(movimientoService).registrarSalida(2L, 3, "Venta #1", "Carlos");
    }

    @Test
    void el_consecutivo_es_ultimoNumero_mas_uno() {
        stubBomba();
        when(ventaRepository.ultimoNumero()).thenReturn(41L);
        when(ventaRepository.save(any(Venta.class))).thenAnswer(inv -> inv.getArgument(0));

        Venta venta = ventaService.registrar(
                List.of(new VentaService.LineaSolicitada(1L, 1)), List.of(),
                FormaPago.TARJETA, null, "Ana", null, null);

        assertThat(venta.getNumero()).isEqualTo(42L);
        verify(movimientoService).registrarSalida(1L, 1, "Venta #42", "Ana");
    }

    // ------------------------------------------------------------------
    //  Pago en efectivo
    // ------------------------------------------------------------------

    @Test
    void efectivo_calcula_el_cambio() {
        stubBomba();
        stubRepoVentas();

        Venta venta = ventaService.registrar(
                List.of(new VentaService.LineaSolicitada(1L, 1)), List.of(), // total 100.00
                FormaPago.EFECTIVO, new BigDecimal("150.00"), "Carlos", null, null);

        assertThat(venta.getMontoRecibido()).isEqualByComparingTo("150.00");
        assertThat(venta.getCambio()).isEqualByComparingTo("50.00");
    }

    @Test
    void efectivo_con_monto_insuficiente_lanza_IllegalArgument_y_no_guarda() {
        stubBomba();

        assertThatThrownBy(() -> ventaService.registrar(
                List.of(new VentaService.LineaSolicitada(1L, 1)), List.of(), // total 100.00
                FormaPago.EFECTIVO, new BigDecimal("50.00"), "Carlos", null, null))
                .isInstanceOf(IllegalArgumentException.class);

        verify(ventaRepository, never()).save(any());
    }

    @Test
    void tarjeta_no_guarda_monto_recibido_ni_cambio() {
        stubBomba();
        stubRepoVentas();

        Venta venta = ventaService.registrar(
                List.of(new VentaService.LineaSolicitada(1L, 1)), List.of(),
                FormaPago.TARJETA, new BigDecimal("999.00"), "Carlos", null, null);

        assertThat(venta.getMontoRecibido()).isNull();
        assertThat(venta.getCambio()).isEqualByComparingTo("0.00");
    }

    // ------------------------------------------------------------------
    //  Errores (repuestos)
    // ------------------------------------------------------------------

    @Test
    void carrito_vacio_lanza_IllegalArgument() {
        assertThatThrownBy(() -> ventaService.registrar(
                List.of(), List.of(), FormaPago.TARJETA, null, "Carlos", null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void producto_sin_precio_lanza_IllegalArgument_y_no_guarda() {
        correa.setPrecioVenta(null);
        when(productoRepository.findById(2L)).thenReturn(Optional.of(correa));

        assertThatThrownBy(() -> ventaService.registrar(
                List.of(new VentaService.LineaSolicitada(2L, 1)), List.of(),
                FormaPago.TARJETA, null, "Carlos", null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Correa");

        verify(ventaRepository, never()).save(any());
        verify(movimientoService, never()).registrarSalida(anyLong(), anyInt(), anyString(), any());
    }

    @Test
    void producto_inexistente_lanza_RecursoNoEncontrado() {
        when(productoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ventaService.registrar(
                List.of(new VentaService.LineaSolicitada(99L, 1)), List.of(),
                FormaPago.TARJETA, null, "Carlos", null, null))
                .isInstanceOf(RecursoNoEncontradoException.class);

        verify(ventaRepository, never()).save(any());
    }

    @Test
    void cantidad_de_linea_no_positiva_lanza_IllegalArgument() {
        assertThatThrownBy(() -> ventaService.registrar(
                List.of(new VentaService.LineaSolicitada(1L, 0)), List.of(),
                FormaPago.TARJETA, null, "Carlos", null, null))
                .isInstanceOf(IllegalArgumentException.class);

        verify(ventaRepository, never()).save(any());
    }

    @Test
    void si_falta_stock_en_una_linea_no_se_guarda_la_venta() {
        stubBomba();
        stubCorrea();
        // La salida de la linea 1 (producto 1) se deja pasar; la del producto 2
        // revienta por stock. lenient() para que el stub de 2L no choque con la
        // llamada real de 1L bajo strict stubbing.
        lenient().doThrow(new StockInsuficienteException("No hay stock de \"Correa\"."))
                .when(movimientoService).registrarSalida(eq(2L), anyInt(), anyString(), any());

        assertThatThrownBy(() -> ventaService.registrar(
                carrito(1L, 1, 2L, 10), List.of(), FormaPago.TARJETA, null, "Carlos", null, null))
                .isInstanceOf(StockInsuficienteException.class);

        verify(ventaRepository, never()).save(any());
    }

    @Test
    void guarda_los_datos_de_cliente_recortando_espacios() {
        stubBomba();
        stubRepoVentas();

        Venta venta = ventaService.registrar(
                List.of(new VentaService.LineaSolicitada(1L, 1)), List.of(),
                FormaPago.TARJETA, null, "  Carlos  ", "  Juan Perez ", " 12345 ");

        assertThat(venta.getResponsable()).isEqualTo("Carlos");
        assertThat(venta.getClienteNombre()).isEqualTo("Juan Perez");
        assertThat(venta.getClienteDocumento()).isEqualTo("12345");
    }

    // ------------------------------------------------------------------
    //  Lineas libres: mantenimiento
    // ------------------------------------------------------------------

    @Test
    void registra_un_servicio_de_mantenimiento_sin_producto_ni_stock() {
        stubRepoVentas();

        var libre = new VentaService.LineaLibreSolicitada(
                TipoLinea.MANTENIMIENTO, "Mano de obra", 1, new BigDecimal("50000"), null, null);

        Venta venta = ventaService.registrar(List.of(), List.of(libre),
                FormaPago.EFECTIVO, new BigDecimal("50000"), "Carlos", null, null);

        assertThat(venta.getLineas()).hasSize(1);
        assertThat(venta.getLineas().get(0).getTipo()).isEqualTo(TipoLinea.MANTENIMIENTO);
        assertThat(venta.getLineas().get(0).getProducto()).isNull();
        assertThat(venta.getTotal()).isEqualByComparingTo("50000.00");
        verifyNoInteractions(movimientoService);
        verifyNoInteractions(alquilerService);
    }

    @Test
    void una_venta_puede_tener_varias_lineas_de_mantenimiento_desglosadas() {
        stubRepoVentas();

        var visita = new VentaService.LineaLibreSolicitada(
                TipoLinea.MANTENIMIENTO, "Visita / diagnostico", 1, new BigDecimal("30000"), null, null);
        var manoDeObra = new VentaService.LineaLibreSolicitada(
                TipoLinea.MANTENIMIENTO, "Mano de obra", 1, new BigDecimal("50000"), null, null);

        Venta venta = ventaService.registrar(List.of(), List.of(visita, manoDeObra),
                FormaPago.TARJETA, null, "Carlos", null, null);

        assertThat(venta.getLineas()).hasSize(2);
        assertThat(venta.getTotal()).isEqualByComparingTo("80000.00");
    }

    @Test
    void mezcla_producto_y_servicio_en_la_misma_venta() {
        stubBomba();
        stubRepoVentas();

        var servicio = new VentaService.LineaLibreSolicitada(
                TipoLinea.MANTENIMIENTO, "Visita", 1, new BigDecimal("30000"), null, null);

        Venta venta = ventaService.registrar(
                List.of(new VentaService.LineaSolicitada(1L, 1)), List.of(servicio),
                FormaPago.TARJETA, null, "Carlos", null, null);

        assertThat(venta.getLineas()).hasSize(2);
        // 100.00 (bomba) + 30000.00 (servicio)
        assertThat(venta.getTotal()).isEqualByComparingTo("30100.00");
        verify(movimientoService).registrarSalida(1L, 1, "Venta #1", "Carlos");
    }

    // ------------------------------------------------------------------
    //  Lineas libres: alquiler
    // ------------------------------------------------------------------

    @Test
    void registra_un_alquiler_valida_disponibilidad_e_inicia_el_prestamo_tras_guardar() {
        stubRepoVentas();
        Lavadora lavadora = lavadora(9, "PEQ-01");
        when(alquilerService.lavadoraDisponible(9L)).thenReturn(lavadora);

        var libre = new VentaService.LineaLibreSolicitada(
                TipoLinea.ALQUILER, "Alquiler Pequena (PEQ-01)", 3, new BigDecimal("5000"), 9L, "Juan");

        Venta venta = ventaService.registrar(List.of(), List.of(libre),
                FormaPago.EFECTIVO, new BigDecimal("15000"), "Carlos", null, null);

        assertThat(venta.getTotal()).isEqualByComparingTo("15000.00");
        assertThat(venta.getLineas().get(0).getTipo()).isEqualTo(TipoLinea.ALQUILER);
        verify(alquilerService).iniciar(lavadora, venta, "Juan", 3);
    }

    @Test
    void alquiler_con_lavadora_no_disponible_no_registra_la_venta() {
        when(alquilerService.lavadoraDisponible(9L)).thenThrow(
                new IllegalArgumentException("La lavadora \"PEQ-01\" no esta disponible ahora mismo."));

        var libre = new VentaService.LineaLibreSolicitada(
                TipoLinea.ALQUILER, "Alquiler Pequena (PEQ-01)", 3, new BigDecimal("5000"), 9L, "Juan");

        assertThatThrownBy(() -> ventaService.registrar(List.of(), List.of(libre),
                FormaPago.EFECTIVO, new BigDecimal("15000"), "Carlos", null, null))
                .isInstanceOf(IllegalArgumentException.class);

        verify(ventaRepository, never()).save(any());
        verify(alquilerService, never()).iniciar(any(), any(), any(), anyInt());
    }

    @Test
    void alquiler_sin_lavadora_elegida_lanza_IllegalArgument() {
        var libre = new VentaService.LineaLibreSolicitada(
                TipoLinea.ALQUILER, "Alquiler", 1, new BigDecimal("5000"), null, "Juan");

        assertThatThrownBy(() -> ventaService.registrar(List.of(), List.of(libre),
                FormaPago.TARJETA, null, "Carlos", null, null))
                .isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(alquilerService);
    }

    @Test
    void linea_libre_de_tipo_PRODUCTO_lanza_IllegalArgument() {
        var libre = new VentaService.LineaLibreSolicitada(
                TipoLinea.PRODUCTO, "x", 1, new BigDecimal("100"), null, null);

        assertThatThrownBy(() -> ventaService.registrar(List.of(), List.of(libre),
                FormaPago.TARJETA, null, "Carlos", null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ------------------------------------------------------------------
    //  ventasEnRango (la usa el historial y CajaService para el cierre)
    // ------------------------------------------------------------------

    @Test
    void ventasEnRango_consulta_desde_el_inicio_del_dia_hasta_el_dia_siguiente_de_hasta() {
        LocalDate desde = LocalDate.of(2026, 9, 1);
        LocalDate hasta = LocalDate.of(2026, 9, 10);
        when(ventaRepository.findByFechaBetweenOrderByFechaDesc(any(), any())).thenReturn(List.of());

        ventaService.ventasEnRango(desde, hasta);

        verify(ventaRepository).findByFechaBetweenOrderByFechaDesc(
                desde.atStartOfDay(), hasta.plusDays(1).atStartOfDay());
    }

    @Test
    void ventasEnRango_con_hasta_anterior_a_desde_lanza_IllegalArgument() {
        assertThatThrownBy(() -> ventaService.ventasEnRango(
                LocalDate.of(2026, 9, 10), LocalDate.of(2026, 9, 1)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void ventasEnRango_sin_fechas_lanza_IllegalArgument() {
        assertThatThrownBy(() -> ventaService.ventasEnRango(null, LocalDate.now()))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
