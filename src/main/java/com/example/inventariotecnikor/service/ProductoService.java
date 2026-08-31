package com.example.inventariotecnikor.service;

import com.example.inventariotecnikor.exception.CodigoQrDuplicadoException;
import com.example.inventariotecnikor.exception.RecursoNoEncontradoException;
import com.example.inventariotecnikor.model.Producto;
import com.example.inventariotecnikor.repository.ProductoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Reglas de negocio sobre productos: alta, edicion de datos, busquedas.
 *
 * Por que existe esta capa y no metemos esto en el controlador:
 *  - Reutilizable: el mismo metodo sirve a una vista y a una futura API.
 *  - Transacciones: aqui se marca donde empieza y termina una unidad de
 *    trabajo (@Transactional).
 *  - Testeable sin levantar la web.
 *
 * @Service registra la clase como bean de Spring. Como hay un solo
 * constructor, Spring inyecta el repositorio sin necesidad de @Autowired
 * (inyeccion por constructor, el estilo recomendado).
 *
 * OJO: el stock NO se toca desde aqui. El stock solo cambia registrando
 * movimientos (MovimientoService), para que siempre quede rastro.
 */
@Service
@Transactional(readOnly = true) // por defecto los metodos solo leen...
public class ProductoService {

    private final ProductoRepository productoRepository;

    public ProductoService(ProductoRepository productoRepository) {
        this.productoRepository = productoRepository;
    }

    // ------------------------------------------------------------------
    //  Lecturas
    // ------------------------------------------------------------------

    /** Para el escaner: devuelve el producto o lanza excepcion si no existe. */
    public Producto buscarPorQr(String codigoQr) {
        return productoRepository.findByCodigoQr(codigoQr)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No hay ningun producto con el codigo QR: " + codigoQr));
    }

    /** Igual que el anterior pero sin excepcion: util cuando "no encontrado" es un caso normal. */
    public Optional<Producto> buscarPorQrOpcional(String codigoQr) {
        return productoRepository.findByCodigoQr(codigoQr);
    }

    public Producto obtenerPorId(Long id) {
        return productoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe el producto con id: " + id));
    }

    public List<Producto> listarActivos() {
        return productoRepository.findByActivoTrueOrderByNombreAsc();
    }

    /** Buscador de texto libre de la pantalla de busqueda. */
    public List<Producto> buscar(String texto) {
        if (texto == null || texto.isBlank()) {
            return listarActivos();
        }
        return productoRepository.buscarPorTexto(texto.trim());
    }

    /** Alimenta el dashboard de stock bajo. */
    public List<Producto> conStockBajo() {
        return productoRepository.findConStockBajo();
    }

    // ------------------------------------------------------------------
    //  Escrituras (readOnly = false explicito para que quede claro)
    // ------------------------------------------------------------------

    /**
     * Da de alta un producto nuevo. Valida que el codigo QR no este repetido.
     * El stock inicial se pasa en el propio objeto; si quieres registrar de
     * donde salio ese stock, mejor crearlo en 0 y luego una ENTRADA.
     */
    @Transactional
    public Producto alta(Producto nuevo) {
        String qr = nuevo.getCodigoQr();
        if (productoRepository.existsByCodigoQr(qr)) {
            throw new CodigoQrDuplicadoException("Ya existe un producto con el codigo QR: " + qr);
        }
        return productoRepository.save(nuevo);
    }

    /**
     * Actualiza los datos descriptivos de un producto ya existente.
     * No modifica el stock (eso es via movimientos) ni el codigo QR.
     *
     * Fijate que no llamamos a save(): al estar dentro de @Transactional,
     * el producto que devuelve obtenerPorId(...) esta "managed" y Hibernate
     * detecta los cambios (dirty checking) y hace el UPDATE al confirmar.
     * Viniendo de EF Core: es como el change tracking, pero sin el
     * SaveChanges() explicito.
     */
    @Transactional
    public Producto actualizarDatos(Long id, Producto datos) {
        Producto p = obtenerPorId(id);
        p.setNombre(datos.getNombre());
        p.setDescripcion(datos.getDescripcion());
        p.setNumeroParte(datos.getNumeroParte());
        p.setCategoria(datos.getCategoria());
        p.setMarca(datos.getMarca());
        p.setUbicacion(datos.getUbicacion());
        p.setStockMinimo(datos.getStockMinimo());
        p.setPrecioVenta(datos.getPrecioVenta());
        return p;
    }

    /** Baja logica: no borra la fila, la marca inactiva para que deje de listarse. */
    @Transactional
    public void desactivar(Long id) {
        obtenerPorId(id).setActivo(false);
    }

    @Transactional
    public void reactivar(Long id) {
        obtenerPorId(id).setActivo(true);
    }
}
