-- ============================================================================
--  Migracion manual para bases de datos creadas ANTES de las lineas de venta
--  "libres" (alquiler / mantenimiento), que no tienen un Producto detras.
--
--  Por que hace falta:
--  Hibernate creo la columna "producto_id" de lineas_venta como NOT NULL
--  (toda venta era de un producto). Ahora una linea puede ser de tipo
--  ALQUILER o MANTENIMIENTO, sin producto, asi que "producto_id" tiene que
--  admitir NULL. Con spring.jpa.hibernate.ddl-auto=update Hibernate agrega
--  columnas y tablas que faltan, pero NO relaja un NOT NULL ya existente, y
--  SQLite no permite cambiar la nulabilidad de una columna con ALTER TABLE.
--  Hay que reconstruir la tabla.
--
--  En una base de datos NUEVA no hace falta ejecutar nada: Hibernate ya crea
--  la columna admitiendo NULL.
--
--  Como ejecutarlo (desde la raiz del proyecto, con la app PARADA):
--      sqlite3 inventario.db < db/migraciones/2026-09-12_lineas_venta_producto_opcional.sql
--  Haz copia de inventario.db antes, por si acaso.
-- ============================================================================

PRAGMA foreign_keys = OFF;

BEGIN TRANSACTION;

ALTER TABLE lineas_venta RENAME TO lineas_venta_old;

CREATE TABLE lineas_venta (
    id integer,
    cantidad integer not null,
    codigo_qr varchar(64),
    descripcion varchar(150) not null,
    importe numeric(12,2) not null,
    porcentaje_iva integer not null,
    precio_unitario numeric(12,2) not null,
    valor_iva numeric(12,2) not null,
    producto_id bigint,
    venta_id bigint not null,
    primary key (id)
);

INSERT INTO lineas_venta
    (id, cantidad, codigo_qr, descripcion, importe, porcentaje_iva, precio_unitario, valor_iva, producto_id, venta_id)
SELECT
    id, cantidad, codigo_qr, descripcion, importe, porcentaje_iva, precio_unitario, valor_iva, producto_id, venta_id
FROM lineas_venta_old;

DROP TABLE lineas_venta_old;

COMMIT;

PRAGMA foreign_keys = ON;
