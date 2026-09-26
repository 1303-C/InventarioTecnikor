package com.example.inventariotecnikor.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Datos del negocio Betty que salen en el encabezado del ticket.
 * Se configuran en application.properties con el prefijo "betty.negocio".
 */
@Component
@ConfigurationProperties(prefix = "betty.negocio")
public class BettyProperties {

    private String nombre = "Betty";
    private String eslogan = "Belleza y Cuidado";
    private String telefono;
    private String nit;

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getEslogan() { return eslogan; }
    public void setEslogan(String eslogan) { this.eslogan = eslogan; }

    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }

    public String getNit() { return nit; }
    public void setNit(String nit) { this.nit = nit; }
}
