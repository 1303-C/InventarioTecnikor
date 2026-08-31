package com.example.inventariotecnikor.web.form;

import com.example.inventariotecnikor.model.TipoMovimiento;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Respalda el formulario de "registrar entrada / salida".
 *
 * El "tipo" (ENTRADA o SALIDA) no lo elige el usuario en un desplegable:
 * llega decidido desde el boton que pulso en la ficha del producto, y viaja
 * en un campo oculto. Aun asi lo validamos (@NotNull) por si acaso.
 */
public class MovimientoForm {

    @NotNull
    private TipoMovimiento tipo;

    @NotNull
    @Positive(message = "La cantidad debe ser mayor que cero")
    private Integer cantidad;

    @Size(max = 200)
    private String motivo;

    @Size(max = 80)
    private String responsable;

    public TipoMovimiento getTipo() {
        return tipo;
    }

    public void setTipo(TipoMovimiento tipo) {
        this.tipo = tipo;
    }

    public Integer getCantidad() {
        return cantidad;
    }

    public void setCantidad(Integer cantidad) {
        this.cantidad = cantidad;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }

    public String getResponsable() {
        return responsable;
    }

    public void setResponsable(String responsable) {
        this.responsable = responsable;
    }
}
