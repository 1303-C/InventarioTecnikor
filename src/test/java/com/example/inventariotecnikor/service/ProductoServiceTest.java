package com.example.inventariotecnikor.service;

import com.example.inventariotecnikor.exception.CodigoQrDuplicadoException;
import com.example.inventariotecnikor.exception.RecursoNoEncontradoException;
import com.example.inventariotecnikor.model.Categoria;
import com.example.inventariotecnikor.model.Producto;
import com.example.inventariotecnikor.repository.ProductoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Reglas de negocio sobre productos: busquedas, alta con QR unico y
 * edicion de datos sin tocar el stock. Repositorio mockeado; no levanta
 * Spring ni base de datos.
 */
@ExtendWith(MockitoExtension.class)
class ProductoServiceTest {

    @Mock
    ProductoRepository productoRepository;

    @InjectMocks
    ProductoService productoService;

    private Producto producto;

    @BeforeEach
    void setUp() {
        producto = new Producto("NEV-001", "Termostato", Categoria.NEVERA, 3);
        producto.setStockActual(7);
    }

    // ------------------------------------------------------------------
    //  Busqueda por codigo QR (la del escaner)
    // ------------------------------------------------------------------

    @Test
    void buscarPorQr_devuelve_el_producto_si_existe() {
        when(productoRepository.findByCodigoQr("NEV-001")).thenReturn(Optional.of(producto));

        assertThat(productoService.buscarPorQr("NEV-001")).isSameAs(producto);
    }

    @Test
    void buscarPorQr_lanza_RecursoNoEncontrado_si_no_existe() {
        when(productoRepository.findByCodigoQr("XXX")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productoService.buscarPorQr("XXX"))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining("XXX");
    }

    @Test
    void buscarPorQrOpcional_traslada_tal_cual_el_resultado_del_repositorio() {
        when(productoRepository.findByCodigoQr("NEV-001")).thenReturn(Optional.of(producto));
        when(productoRepository.findByCodigoQr("NADA")).thenReturn(Optional.empty());

        assertThat(productoService.buscarPorQrOpcional("NEV-001")).containsSame(producto);
        assertThat(productoService.buscarPorQrOpcional("NADA")).isEmpty();
    }

    // ------------------------------------------------------------------
    //  obtenerPorId
    // ------------------------------------------------------------------

    @Test
    void obtenerPorId_devuelve_el_producto_si_existe() {
        when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));

        assertThat(productoService.obtenerPorId(1L)).isSameAs(producto);
    }

    @Test
    void obtenerPorId_lanza_RecursoNoEncontrado_si_no_existe() {
        when(productoRepository.findById(2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productoService.obtenerPorId(2L))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    // ------------------------------------------------------------------
    //  Alta con codigo QR unico
    // ------------------------------------------------------------------

    @Test
    void alta_guarda_el_producto_cuando_el_codigo_no_existe() {
        when(productoRepository.existsByCodigoQr("NEV-001")).thenReturn(false);
        when(productoRepository.save(producto)).thenReturn(producto);

        Producto creado = productoService.alta(producto);

        assertThat(creado).isSameAs(producto);
        verify(productoRepository).save(producto);
    }

    @Test
    void alta_lanza_CodigoQrDuplicado_y_no_guarda_si_el_codigo_ya_existe() {
        when(productoRepository.existsByCodigoQr("NEV-001")).thenReturn(true);

        assertThatThrownBy(() -> productoService.alta(producto))
                .isInstanceOf(CodigoQrDuplicadoException.class)
                .hasMessageContaining("NEV-001");

        verify(productoRepository, never()).save(any());
    }

    // ------------------------------------------------------------------
    //  Edicion de datos: NO toca stock ni codigo QR, y NO llama a save()
    //  (confia en el dirty checking dentro de la transaccion)
    // ------------------------------------------------------------------

    @Test
    void actualizarDatos_copia_los_campos_descriptivos() {
        when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));

        Producto datos = new Producto("IGNORADO", "Termostato nuevo", Categoria.ESTUFA, 5);
        datos.setDescripcion("descripcion nueva");
        datos.setNumeroParte("NP-1");
        datos.setMarca("Mabe");
        datos.setUbicacion("A1");
        datos.setPrecioVenta(new BigDecimal("12.50"));

        Producto resultado = productoService.actualizarDatos(1L, datos);

        assertThat(resultado).isSameAs(producto);
        assertThat(resultado.getNombre()).isEqualTo("Termostato nuevo");
        assertThat(resultado.getCategoria()).isEqualTo(Categoria.ESTUFA);
        assertThat(resultado.getStockMinimo()).isEqualTo(5);
        assertThat(resultado.getDescripcion()).isEqualTo("descripcion nueva");
        assertThat(resultado.getNumeroParte()).isEqualTo("NP-1");
        assertThat(resultado.getMarca()).isEqualTo("Mabe");
        assertThat(resultado.getUbicacion()).isEqualTo("A1");
        assertThat(resultado.getPrecioVenta()).isEqualByComparingTo("12.50");
    }

    @Test
    void actualizarDatos_no_modifica_el_stock_ni_el_codigo_qr_ni_llama_a_save() {
        when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));

        Producto datos = new Producto("OTRO-QR", "Otro nombre", Categoria.OTRO, 1);
        datos.setStockActual(999); // no debe copiarse

        Producto resultado = productoService.actualizarDatos(1L, datos);

        assertThat(resultado.getStockActual()).isEqualTo(7);      // stock intacto
        assertThat(resultado.getCodigoQr()).isEqualTo("NEV-001"); // QR intacto
        verify(productoRepository, never()).save(any());
    }

    @Test
    void actualizarDatos_lanza_RecursoNoEncontrado_si_el_id_no_existe() {
        when(productoRepository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productoService.actualizarDatos(9L, producto))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    // ------------------------------------------------------------------
    //  Baja / alta logica
    // ------------------------------------------------------------------

    @Test
    void desactivar_marca_el_producto_como_inactivo() {
        producto.setActivo(true);
        when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));

        productoService.desactivar(1L);

        assertThat(producto.isActivo()).isFalse();
        verify(productoRepository, never()).save(any());
    }

    @Test
    void reactivar_marca_el_producto_como_activo() {
        producto.setActivo(false);
        when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));

        productoService.reactivar(1L);

        assertThat(producto.isActivo()).isTrue();
    }

    // ------------------------------------------------------------------
    //  Buscador de texto libre
    // ------------------------------------------------------------------

    @Test
    void buscar_sin_texto_devuelve_los_activos_y_no_consulta_por_texto() {
        when(productoRepository.findByActivoTrueOrderByNombreAsc()).thenReturn(List.of(producto));

        assertThat(productoService.buscar("   ")).containsExactly(producto);
        assertThat(productoService.buscar(null)).containsExactly(producto);

        verify(productoRepository, never()).buscarPorTexto(any());
    }

    @Test
    void buscar_con_texto_consulta_por_texto_recortando_espacios() {
        when(productoRepository.buscarPorTexto("bomba")).thenReturn(List.of(producto));

        assertThat(productoService.buscar("  bomba  ")).containsExactly(producto);

        verify(productoRepository).buscarPorTexto("bomba");
    }
}
