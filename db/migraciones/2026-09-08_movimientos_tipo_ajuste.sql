-- ============================================================================
--  Migracion manual para bases de datos creadas ANTES del tipo de movimiento
--  AJUSTE (ajuste por conteo fisico).
--
--  Por que hace falta:
--  Hibernate genera en la columna "tipo" un CHECK con la lista de valores
--  del enum en el momento de crear la tabla:
--        check (tipo in ('ENTRADA','SALIDA'))
--  Con spring.jpa.hibernate.ddl-auto=update ese CHECK NO se actualiza al
--  anadir un valor nuevo al enum, y SQLite no permite quitar un CHECK con
--  ALTER TABLE. Hay que reconstruir la tabla.
--
--  En una base de datos NUEVA no hace falta ejecutar nada: Hibernate ya crea
--  el CHECK con los tres valores.
--
--  Como ejecutarlo (desde la raiz del proyecto, con la app PARADA):
--      sqlite3 inventario.db < db/migraciones/2026-09-08_movimientos_tipo_ajuste.sql
--  Haz copia de inventario.db antes, por si acaso.
-- ============================================================================

PRAGMA foreign_keys = OFF;

BEGIN TRANSACTION;

ALTER TABLE movimientos_inventario RENAME TO movimientos_inventario_old;

CREATE TABLE movimientos_inventario (
    id integer,
    cantidad integer not null,
    fecha timestamp not null,
    motivo varchar(200),
    responsable varchar(80),
    stock_resultante integer not null,
    tipo varchar(10) not null check ((tipo in ('ENTRADA','SALIDA','AJUSTE'))),
    producto_id bigint not null,
    primary key (id)
);

INSERT INTO movimientos_inventario
    (id, cantidad, fecha, motivo, responsable, stock_resultante, tipo, producto_id)
SELECT
    id, cantidad, fecha, motivo, responsable, stock_resultante, tipo, producto_id
FROM movimientos_inventario_old;

DROP TABLE movimientos_inventario_old;

CREATE INDEX idx_movimientos_producto ON movimientos_inventario (producto_id);
CREATE INDEX idx_movimientos_fecha    ON movimientos_inventario (fecha);

COMMIT;

PRAGMA foreign_keys = ON;
