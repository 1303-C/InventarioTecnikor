package com.example.inventariotecnikor.web.form;

import com.example.inventariotecnikor.model.Categoria;
import com.example.inventariotecnikor.model.Producto;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Objeto que respalda el formulario de alta / edicion de productos
 * ("form-backing object" o DTO de entrada).
 *
 * Por que no bindear el formulario directamente contra la entidad Producto:
 *  - La entidad tiene campos que el formulario NO debe tocar (id, fechas,
 *    activo, y sobre todo stockActual, que solo cambia por movimientos).
 *  - Aqui las validaciones son las de la PANTALLA; en la entidad son las de
 *    la BD. Conviene separarlas.
 *  - Evita el riesgo de "mass assignment" (que alguien mande campos de mas).
 *
 * Los tipos son envoltorios (Integer, no int) para que un campo vacio llegue
 * como null y @NotNull lo detecte, en vez de convertirse en 0 sin avisar.
 */
public class ProductoForm {

    /** null cuando es un alta; con valor cuando es una edicion. */
    private Long id;

    @NotBlank
    @Size(max = 64)
    private String codigoQr;

    @NotBlank
    @Size(max = 150)
    private String nombre;

    @Size(max = 500)
    private String descripcion;

    @Size(max = 80)
    private String numeroParte;

    @NotNull(message = "Elige una categoria")
    private Categoria categoria;

    @Size(max = 80)
    private String marca;

    @Size(max = 50)
    private String ubicacion;

    /** Solo se usa al crear. En edicion se ignora (el stock cambia por movimientos). */
    @NotNull
    @PositiveOrZero
    private Integer stockInicial;

    @NotNull
    @PositiveOrZero
    private Integer stockMinimo;

    @PositiveOrZero
    @Digits(integer = 10, fraction = 2)
    private BigDecimal precioVenta;

    // ------------------------------------------------------------------
    //  Conversiones entre form y entidad
    // ------------------------------------------------------------------

    /** Construye una entidad Producto con los datos del formulario. */
    public Producto toProducto() {
        int minimo = stockMinimo != null ? stockMinimo : 0;
        Producto p = new Producto(codigoQr, nombre, categoria, minimo);
        p.setDescripcion(descripcion);
        p.setNumeroParte(numeroParte);
        p.setMarca(marca);
        p.setUbicacion(ubicacion);
        p.setPrecioVenta(precioVenta);
        if (stockInicial != null) {
            p.setStockActual(stockInicial);
        }
        return p;
    }

    /** Rellena el formulario a partir de un producto existente (para editar). */
    public static ProductoForm desde(Producto p) {
        ProductoForm f = new ProductoForm();
        f.id = p.getId();
        f.codigoQr = p.getCodigoQr();
        f.nombre = p.getNombre();
        f.descripcion = p.getDescripcion();
        f.numeroParte = p.getNumeroParte();
        f.categoria = p.getCategoria();
        f.marca = p.getMarca();
        f.ubicacion = p.getUbicacion();
        f.stockInicial = p.getStockActual(); // no editable; solo para pasar la validacion
        f.stockMinimo = p.getStockMinimo();
        f.precioVenta = p.getPrecioVenta();
        return f;
    }

    // ------------------------------------------------------------------
    //  Getters / setters (Thymeleaf y Spring MVC los necesitan)
    // ------------------------------------------------------------------

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCodigoQr() {
        return codigoQr;
    }

    public void setCodigoQr(String codigoQr) {
        this.codigoQr = codigoQr;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getNumeroParte() {
        return numeroParte;
    }

    public void setNumeroParte(String numeroParte) {
        this.numeroParte = numeroParte;
    }

    public Categoria getCategoria() {
        return categoria;
    }

    public void setCategoria(Categoria categoria) {
        this.categoria = categoria;
    }

    public String getMarca() {
        return marca;
    }

    public void setMarca(String marca) {
        this.marca = marca;
    }

    public String getUbicacion() {
        return ubicacion;
    }

    public void setUbicacion(String ubicacion) {
        this.ubicacion = ubicacion;
    }

    public Integer getStockInicial() {
        return stockInicial;
    }

    public void setStockInicial(Integer stockInicial) {
        this.stockInicial = stockInicial;
    }

    public Integer getStockMinimo() {
        return stockMinimo;
    }

    public void setStockMinimo(Integer stockMinimo) {
        this.stockMinimo = stockMinimo;
    }

    public BigDecimal getPrecioVenta() {
        return precioVenta;
    }

    public void setPrecioVenta(BigDecimal precioVenta) {
        this.precioVenta = precioVenta;
    }
}
