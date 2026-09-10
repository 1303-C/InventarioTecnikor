package com.example.inventariotecnikor.repository;

import com.example.inventariotecnikor.model.Venta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Acceso a datos de ventas. Las ventas no se editan ni se borran: se
 * insertan y se consultan (una anulacion, cuando exista, sera un cambio de
 * estado, no un DELETE).
 */
public interface VentaRepository extends JpaRepository<Venta, Long> {

    /**
     * Ultimo consecutivo usado, o 0 si aun no hay ventas. VentaService le
     * suma 1 para el numero de la venta nueva. Con el pool de conexiones en 1
     * (ver application.properties) las ventas se serializan, asi que no hay
     * carrera por el consecutivo.
     */
    @Query("select coalesce(max(v.numero), 0) from Venta v")
    long ultimoNumero();

    Optional<Venta> findByNumero(long numero);

    /** Ventas en un rango de fechas (para el historial y el cierre de caja). */
    List<Venta> findByFechaBetweenOrderByFechaDesc(LocalDateTime desde, LocalDateTime hasta);
}
