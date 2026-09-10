package com.example.inventariotecnikor.web.form;

import com.example.inventariotecnikor.model.FormaPago;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Respalda el panel de cobro de la pantalla de venta.
 *
 * "montoRecibido" solo aplica cuando la forma de pago es EFECTIVO; que
 * cubra el total lo comprueba el controlador (que conoce el total del
 * carrito) y, como red, Venta.registrarPago(...).
 */
public class CobroForm {

    @NotNull
    private FormaPago formaPago = FormaPago.EFECTIVO;

    @PositiveOrZero
    private BigDecimal montoRecibido;

    @Size(max = 80)
    private String responsable;

    @Size(max = 150)
    private String clienteNombre;

    @Size(max = 40)
    private String clienteDocumento;

    public FormaPago getFormaPago() {
        return formaPago;
    }

    public void setFormaPago(FormaPago formaPago) {
        this.formaPago = formaPago;
    }

    public BigDecimal getMontoRecibido() {
        return montoRecibido;
    }

    public void setMontoRecibido(BigDecimal montoRecibido) {
        this.montoRecibido = montoRecibido;
    }

    public String getResponsable() {
        return responsable;
    }

    public void setResponsable(String responsable) {
        this.responsable = responsable;
    }

    public String getClienteNombre() {
        return clienteNombre;
    }

    public void setClienteNombre(String clienteNombre) {
        this.clienteNombre = clienteNombre;
    }

    public String getClienteDocumento() {
        return clienteDocumento;
    }

    public void setClienteDocumento(String clienteDocumento) {
        this.clienteDocumento = clienteDocumento;
    }
}
