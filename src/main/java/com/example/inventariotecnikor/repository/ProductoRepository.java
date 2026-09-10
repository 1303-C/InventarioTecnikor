package com.example.inventariotecnikor.repository;

import com.example.inventariotecnikor.model.Categoria;
import com.example.inventariotecnikor.model.Producto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

/**
 * Acceso a datos de Producto.
 *
 * Extender JpaRepository<Producto, Long> ya te da gratis save, findById,
 * findAll, deleteById, count, etc. (parecido a un DbSet<Producto> de EF).
 * No hay que implementar nada: Spring Data genera la clase en tiempo de
 * arranque.
 *
 * Los metodos de abajo son "derived queries": Spring lee el NOMBRE del
 * metodo y construye el SQL. findByCodigoQr -> WHERE codigo_qr = ?.
 * Cuando el nombre se vuelve ilegible, se usa @Query con JPQL (como el
 * ultimo metodo).
 */
public interface ProductoRepository extends JpaRepository<Producto, Long> {

    /** Buscar por el QR escaneado. Optional = puede no existir; te obliga a manejarlo. */
    Optional<Producto> findByCodigoQr(String codigoQr);

    /** Para validar en el alta que el QR no este repetido. */
    boolean existsByCodigoQr(String codigoQr);

    /** Listado normal: solo productos activos, ordenados por nombre. */
    List<Producto> findByActivoTrueOrderByNombreAsc();

    List<Producto> findByCategoriaAndActivoTrueOrderByNombreAsc(Categoria categoria);

    /**
     * Buscador de texto libre para la pantalla de busqueda: coincidencia
     * parcial e ignorando mayusculas en nombre, marca, numero de parte o
     * codigo QR (para poder escanear directamente en el buscador).
     * Con derived query el nombre del metodo seria kilometrico y con la
     * precedencia de AND/OR mal puesta, asi que se hace con JPQL.
     * "lower(...) like %:q%" es el equivalente a un LIKE case-insensitive.
     */
    @Query("""
            select p from Producto p
            where p.activo = true and (
                  lower(p.nombre)      like lower(concat('%', :texto, '%'))
               or lower(p.marca)       like lower(concat('%', :texto, '%'))
               or lower(p.numeroParte) like lower(concat('%', :texto, '%'))
               or lower(p.codigoQr)    like lower(concat('%', :texto, '%'))
            )
            order by p.nombre asc
            """)
    List<Producto> buscarPorTexto(String texto);

    /**
     * Dashboard de stock bajo. No se puede expresar como derived query
     * porque compara DOS columnas de la misma fila (stockActual <=
     * stockMinimo), asi que va con JPQL explicito.
     */
    @Query("""
            select p from Producto p
            where p.activo = true and p.stockActual <= p.stockMinimo
            order by (p.stockActual - p.stockMinimo) asc
            """)
    List<Producto> findConStockBajo();
}
