package com.example.inventariotecnikor.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Una venta de mostrador (ticket / cuenta de cobro interna).
 *
 * No es todavia una factura electronica DIAN: "numero" es un consecutivo
 * propio que empieza en 1, no una numeracion autorizada. Los campos de
 * cliente y de IVA ya estan aqui para engancharla mas adelante sin migrar.
 *
 * El stock NO se toca desde esta entidad: VentaService registra una SALIDA
 * por cada linea a traves de MovimientoService, que es el unico sitio que
 * mueve stock.
 *
 * OJO (igual que en MovimientoInventario): Hibernate genera un CHECK con la
 * lista actual del enum en "forma_pago" y "estado". En BD nueva se crea bien;
 * si se anade un valor al enum sobre una BD ya creada, hay que migrar a mano.
 */
@Entity
@Table(
        name = "ventas",
        indexes = {
                @Index(name = "idx_ventas_fecha", columnList = "fecha"),
                @Index(name = "idx_ventas_numero", columnList = "numero", unique = true)
        }
)
public class Venta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Consecutivo propio de ventas (1, 2, 3...). Lo asigna VentaService. */
    @Column(nullable = false, unique = true)
    private long numero;

    @Column(nullable = false, updatable = false)
    private LocalDateTime fecha;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private EstadoVenta estado = EstadoVenta.COMPLETADA;

    @Enumerated(EnumType.STRING)
    @Column(name = "forma_pago", nullable = false, length = 15)
    private FormaPago formaPago;

    /** Suma de las lineas sin IVA. */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal = cero();

    @Column(name = "total_iva", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalIva = cero();

    /** subtotal + totalIva: lo que paga el cliente. */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal total = cero();

    /** Solo en efectivo: con cuanto pago el cliente. */
    @Column(name = "monto_recibido", precision = 12, scale = 2)
    private BigDecimal montoRecibido;

    /** Solo en efectivo: montoRecibido - total. */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal cambio = cero();

    @Column(length = 80)
    private String responsable;

    @Column(name = "cliente_nombre", length = 150)
    private String clienteNombre;

    @Column(name = "cliente_documento", length = 40)
    private String clienteDocumento;

    @Column(name = "motivo_anulacion", length = 200)
    private String motivoAnulacion;

    @Column(name = "fecha_anulacion")
    private LocalDateTime fechaAnulacion;

    @Column(name = "anulado_por", length = 80)
    private String anuladoPor;

    @OneToMany(mappedBy = "venta", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id asc")
    private List<LineaVenta> lineas = new ArrayList<>();

    protected Venta() {
    }

    public Venta(long numero, FormaPago formaPago, String responsable) {
        this.numero = numero;
        this.formaPago = formaPago;
        this.responsable = responsable;
    }

    @PrePersist
    void alCrear() {
        this.fecha = LocalDateTime.now();
    }

    // --- Construccion de la venta ---

    public void addLinea(LineaVenta linea) {
        linea.setVenta(this);
        this.lineas.add(linea);
    }

    public void setCliente(String nombre, String documento) {
        this.clienteNombre = nombre;
        this.clienteDocumento = documento;
    }

    /** Recalcula subtotal / totalIva / total a partir de las lineas. */
    public void recalcularTotales() {
        BigDecimal sumaBase = cero();
        BigDecimal sumaIva = cero();
        for (LineaVenta l : lineas) {
            sumaBase = sumaBase.add(l.getBase());
            sumaIva = sumaIva.add(l.getValorIva());
        }
        this.subtotal = sumaBase.setScale(2, RoundingMode.HALF_UP);
        this.totalIva = sumaIva.setScale(2, RoundingMode.HALF_UP);
        this.total = this.subtotal.add(this.totalIva).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Fija la forma de pago y, si es efectivo, valida el monto recibido y
     * calcula el cambio. Llamar despues de recalcularTotales().
     */
    public void registrarPago(FormaPago formaPago, BigDecimal montoRecibido) {
        this.formaPago = formaPago;
        if (formaPago.esEfectivo()) {
            if (montoRecibido == null || montoRecibido.compareTo(total) < 0) {
                throw new IllegalArgumentException(
                        "El monto recibido (" + montoRecibido + ") no cubre el total (" + total + ").");
            }
            this.montoRecibido = montoRecibido.setScale(2, RoundingMode.HALF_UP);
            this.cambio = this.montoRecibido.subtract(total).setScale(2, RoundingMode.HALF_UP);
        } else {
            this.montoRecibido = null;
            this.cambio = cero();
        }
    }

    public int getTotalUnidades() {
        return lineas.stream().mapToInt(LineaVenta::getCantidad).sum();
    }

    /**
     * Marca la venta como ANULADA. No revierte nada por si sola: quien
     * llama (VentaService.anular) es responsable de devolver el stock y
     * las lavadoras ANTES de llamar aqui.
     */
    public void anular(String motivo, String responsable) {
        if (estado == EstadoVenta.ANULADA) {
            throw new IllegalArgumentException("La venta #" + numero + " ya estaba anulada.");
        }
        this.estado = EstadoVenta.ANULADA;
        this.motivoAnulacion = motivo;
        this.fechaAnulacion = LocalDateTime.now();
        this.anuladoPor = responsable;
    }

    private static BigDecimal cero() {
        return BigDecimal.ZERO.setScale(2);
    }

    // --- Getters ---

    public Long getId() {
        return id;
    }

    public long getNumero() {
        return numero;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public EstadoVenta getEstado() {
        return estado;
    }

    public FormaPago getFormaPago() {
        return formaPago;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public BigDecimal getTotalIva() {
        return totalIva;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public BigDecimal getMontoRecibido() {
        return montoRecibido;
    }

    public BigDecimal getCambio() {
        return cambio;
    }

    public String getResponsable() {
        return responsable;
    }

    public String getClienteNombre() {
        return clienteNombre;
    }

    public String getClienteDocumento() {
        return clienteDocumento;
    }

    public String getMotivoAnulacion() {
        return motivoAnulacion;
    }

    public LocalDateTime getFechaAnulacion() {
        return fechaAnulacion;
    }

    public String getAnuladoPor() {
        return anuladoPor;
    }

    public List<LineaVenta> getLineas() {
        return lineas;
    }
}
