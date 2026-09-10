package com.example.inventariotecnikor.service;

import com.example.inventariotecnikor.exception.RecursoNoEncontradoException;
import com.example.inventariotecnikor.exception.StockInsuficienteException;
import com.example.inventariotecnikor.model.Categoria;
import com.example.inventariotecnikor.model.MovimientoInventario;
import com.example.inventariotecnikor.model.Producto;
import com.example.inventariotecnikor.model.TipoMovimiento;
import com.example.inventariotecnikor.repository.MovimientoInventarioRepository;
import com.example.inventariotecnikor.repository.ProductoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Reglas de negocio del registro de entradas/salidas.
 *
 * Son tests "de servicio": los repositorios van mockeados (Mockito), asi
 * que no se levanta Spring ni se toca la base de datos. Solo se comprueba
 * la logica de {@link MovimientoService}.
 */
@ExtendWith(MockitoExtension.class)
class MovimientoServiceTest {

    @Mock
    ProductoRepository productoRepository;

    @Mock
    MovimientoInventarioRepository movimientoRepository;

    @InjectMocks
    MovimientoService movimientoService;

    private Producto producto;

    @BeforeEach
    void setUp() {
        producto = new Producto("LAV-001", "Bomba de agua", Categoria.LAVADORA, 2);
        producto.setStockActual(10);
    }

    /** save(...) devuelve el mismo movimiento que se le pasa (como hace JPA). */
    private void elRepoDevuelveLoQueSeGuarda() {
        when(movimientoRepository.save(any(MovimientoInventario.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));
    }

    // ------------------------------------------------------------------
    //  Camino feliz
    // ------------------------------------------------------------------

    @Test
    void entrada_suma_stock_y_registra_el_movimiento() {
        when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));
        elRepoDevuelveLoQueSeGuarda();

        MovimientoInventario mov = movimientoService.registrar(
                1L, TipoMovimiento.ENTRADA, 5, "compra proveedor", "Carlos");

        assertThat(producto.getStockActual()).isEqualTo(15);
        assertThat(mov.getStockResultante()).isEqualTo(15);
        assertThat(mov.getCantidad()).isEqualTo(5);
        assertThat(mov.getTipo()).isEqualTo(TipoMovimiento.ENTRADA);
        assertThat(mov.getMotivo()).isEqualTo("compra proveedor");
        assertThat(mov.getResponsable()).isEqualTo("Carlos");
        assertThat(mov.getProducto()).isSameAs(producto);
    }

    @Test
    void salida_resta_stock() {
        when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));
        elRepoDevuelveLoQueSeGuarda();

        MovimientoInventario mov = movimientoService.registrar(
                1L, TipoMovimiento.SALIDA, 4, "ajuste conteo", "Ana");

        assertThat(producto.getStockActual()).isEqualTo(6);
        assertThat(mov.getStockResultante()).isEqualTo(6);
    }

    @Test
    void salida_que_deja_el_stock_en_cero_esta_permitida() {
        when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));
        elRepoDevuelveLoQueSeGuarda();

        movimientoService.registrar(1L, TipoMovimiento.SALIDA, 10, "vaciado", "Ana");

        assertThat(producto.getStockActual()).isZero();
        verify(movimientoRepository).save(any(MovimientoInventario.class));
    }

    // ------------------------------------------------------------------
    //  Errores: no debe quedar rastro ni cambiar el stock
    // ------------------------------------------------------------------

    @Test
    void salida_mayor_que_el_stock_lanza_StockInsuficiente_y_no_guarda_nada() {
        when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));

        assertThatThrownBy(() ->
                movimientoService.registrar(1L, TipoMovimiento.SALIDA, 11, "venta", "Ana"))
                .isInstanceOf(StockInsuficienteException.class)
                .hasMessageContaining("Bomba de agua");

        assertThat(producto.getStockActual()).isEqualTo(10); // intacto
        verify(movimientoRepository, never()).save(any());
    }

    @Test
    void cantidad_cero_lanza_IllegalArgument_sin_tocar_repositorios() {
        assertThatThrownBy(() ->
                movimientoService.registrar(1L, TipoMovimiento.ENTRADA, 0, "x", "y"))
                .isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(productoRepository, movimientoRepository);
    }

    @Test
    void cantidad_negativa_lanza_IllegalArgument_sin_tocar_repositorios() {
        assertThatThrownBy(() ->
                movimientoService.registrar(1L, TipoMovimiento.SALIDA, -3, "x", "y"))
                .isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(productoRepository, movimientoRepository);
    }

    @Test
    void producto_inexistente_lanza_RecursoNoEncontrado_y_no_guarda_nada() {
        when(productoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                movimientoService.registrar(99L, TipoMovimiento.ENTRADA, 1, "x", "y"))
                .isInstanceOf(RecursoNoEncontradoException.class);

        verify(movimientoRepository, never()).save(any());
    }

    // ------------------------------------------------------------------
    //  Atajos registrarEntrada / registrarSalida
    // ------------------------------------------------------------------

    @Test
    void registrarEntrada_es_un_atajo_de_registrar_con_tipo_ENTRADA() {
        when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));
        elRepoDevuelveLoQueSeGuarda();

        movimientoService.registrarEntrada(1L, 3, "compra", "Carlos");

        assertThat(producto.getStockActual()).isEqualTo(13);
        ArgumentCaptor<MovimientoInventario> capturado =
                ArgumentCaptor.forClass(MovimientoInventario.class);
        verify(movimientoRepository).save(capturado.capture());
        assertThat(capturado.getValue().getTipo()).isEqualTo(TipoMovimiento.ENTRADA);
    }

    @Test
    void registrarSalida_es_un_atajo_de_registrar_con_tipo_SALIDA() {
        when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));
        elRepoDevuelveLoQueSeGuarda();

        movimientoService.registrarSalida(1L, 3, "ajuste", "Ana");

        assertThat(producto.getStockActual()).isEqualTo(7);
        ArgumentCaptor<MovimientoInventario> capturado =
                ArgumentCaptor.forClass(MovimientoInventario.class);
        verify(movimientoRepository).save(capturado.capture());
        assertThat(capturado.getValue().getTipo()).isEqualTo(TipoMovimiento.SALIDA);
    }

    // ------------------------------------------------------------------
    //  Ajuste por conteo fisico (ajustarA)
    // ------------------------------------------------------------------

    @Test
    void registrar_no_acepta_el_tipo_AJUSTE() {
        assertThatThrownBy(() ->
                movimientoService.registrar(1L, TipoMovimiento.AJUSTE, 5, "x", "y"))
                .isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(productoRepository, movimientoRepository);
    }

    @Test
    void ajustarA_a_la_baja_corrige_el_stock_y_registra_un_AJUSTE() {
        when(productoRepository.findById(1L)).thenReturn(Optional.of(producto)); // stock 10
        elRepoDevuelveLoQueSeGuarda();

        Optional<MovimientoInventario> resultado =
                movimientoService.ajustarA(1L, 7, null, "Ana");

        assertThat(producto.getStockActual()).isEqualTo(7);
        assertThat(resultado).isPresent();
        MovimientoInventario mov = resultado.get();
        assertThat(mov.getTipo()).isEqualTo(TipoMovimiento.AJUSTE);
        assertThat(mov.getCantidad()).isEqualTo(3);          // |10 - 7|
        assertThat(mov.getStockResultante()).isEqualTo(7);
        assertThat(mov.getMotivo()).isEqualTo("Conteo: 10 -> 7 (-3)");
    }

    @Test
    void ajustarA_al_alza_deja_el_signo_positivo_en_el_motivo() {
        when(productoRepository.findById(1L)).thenReturn(Optional.of(producto)); // stock 10
        elRepoDevuelveLoQueSeGuarda();

        MovimientoInventario mov = movimientoService.ajustarA(1L, 15, "  ", "Ana").orElseThrow();

        assertThat(producto.getStockActual()).isEqualTo(15);
        assertThat(mov.getCantidad()).isEqualTo(5);
        assertThat(mov.getMotivo()).isEqualTo("Conteo: 10 -> 15 (+5)");
    }

    @Test
    void ajustarA_a_cero_no_lanza_StockInsuficiente() {
        when(productoRepository.findById(1L)).thenReturn(Optional.of(producto)); // stock 10
        elRepoDevuelveLoQueSeGuarda();

        MovimientoInventario mov = movimientoService.ajustarA(1L, 0, "todo roto", "Ana").orElseThrow();

        assertThat(producto.getStockActual()).isZero();
        assertThat(mov.getStockResultante()).isZero();
        assertThat(mov.getCantidad()).isEqualTo(10);
    }

    @Test
    void ajustarA_incluye_el_motivo_del_operario_despues_del_detalle_del_conteo() {
        when(productoRepository.findById(1L)).thenReturn(Optional.of(producto)); // stock 10
        elRepoDevuelveLoQueSeGuarda();

        MovimientoInventario mov = movimientoService.ajustarA(1L, 8, "roturas", "Ana").orElseThrow();

        assertThat(mov.getMotivo()).isEqualTo("Conteo: 10 -> 8 (-2) - roturas");
    }

    @Test
    void ajustarA_cuando_el_conteo_coincide_con_el_stock_no_registra_nada() {
        when(productoRepository.findById(1L)).thenReturn(Optional.of(producto)); // stock 10

        Optional<MovimientoInventario> resultado =
                movimientoService.ajustarA(1L, 10, "conteo mensual", "Ana");

        assertThat(resultado).isEmpty();
        assertThat(producto.getStockActual()).isEqualTo(10);
        verify(movimientoRepository, never()).save(any());
    }

    @Test
    void ajustarA_con_cantidad_negativa_lanza_IllegalArgument() {
        assertThatThrownBy(() -> movimientoService.ajustarA(1L, -1, "x", "y"))
                .isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(productoRepository, movimientoRepository);
    }

    @Test
    void ajustarA_producto_inexistente_lanza_RecursoNoEncontrado_y_no_guarda_nada() {
        when(productoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> movimientoService.ajustarA(99L, 5, "x", "y"))
                .isInstanceOf(RecursoNoEncontradoException.class);

        verify(movimientoRepository, never()).save(any());
    }
}
