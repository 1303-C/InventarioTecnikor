package com.example.inventariotecnikor.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "ventas_betty")
public class VentaBetty {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private Integer numero;

    private LocalDateTime fecha;

    /** "EFECTIVO" o "TRANSFERENCIA" */
    private String formaPago;

    @Column(precision = 12, scale = 2)
    private BigDecimal total;

    /** "COMPLETADA" o "ANULADA" */
    private String estado;

    private String motivoAnulacion;

    private LocalDateTime fechaAnulacion;

    @OneToMany(mappedBy = "venta", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LineaVentaBetty> lineas = new ArrayList<>();

    public VentaBetty() {}

    public VentaBetty(int numero, String formaPago) {
        this.numero = numero;
        this.formaPago = formaPago;
        this.fecha = LocalDateTime.now();
        this.estado = "COMPLETADA";
        this.total = BigDecimal.ZERO;
    }

    public void addLinea(LineaVentaBetty linea) {
        linea.setVenta(this);
        lineas.add(linea);
    }

    public void recalcularTotal() {
        this.total = lineas.stream()
                .map(LineaVentaBetty::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public Long getId() { return id; }

    public Integer getNumero() { return numero; }
    public void setNumero(Integer numero) { this.numero = numero; }

    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }

    public String getFormaPago() { return formaPago; }
    public void setFormaPago(String formaPago) { this.formaPago = formaPago; }

    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal total) { this.total = total; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public String getMotivoAnulacion() { return motivoAnulacion; }
    public void setMotivoAnulacion(String motivoAnulacion) { this.motivoAnulacion = motivoAnulacion; }

    public LocalDateTime getFechaAnulacion() { return fechaAnulacion; }
    public void setFechaAnulacion(LocalDateTime fechaAnulacion) { this.fechaAnulacion = fechaAnulacion; }

    public List<LineaVentaBetty> getLineas() { return lineas; }
}
