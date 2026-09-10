package com.example.inventariotecnikor.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Datos del negocio que salen en el encabezado del ticket de venta.
 * Se configuran en application.properties con el prefijo "tecnikor.negocio".
 */
@Component
@ConfigurationProperties(prefix = "tecnikor.negocio")
public class NegocioProperties {

    private String nombre = "Tecnikor";
    private String nit;
    private String direccion;
    private String telefono;

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getNit() {
        return nit;
    }

    public void setNit(String nit) {
        this.nit = nit;
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }
}
