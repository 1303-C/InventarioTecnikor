package com.example.inventariotecnikor.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuracion de la impresora de tickets (POS 80mm, ESC/POS).
 * Prefijo "tecnikor.ticket" en application.properties.
 */
@Component
@ConfigurationProperties(prefix = "tecnikor.ticket")
public class TicketProperties {

    /** Como se le mandan los bytes a la impresora. */
    public enum Transporte {
        /** No imprime; solo deja constancia en el log. Util en desarrollo. */
        NONE,
        /** Por red, a host:puerto (puerto 9100 es el habitual en impresoras POS). */
        TCP,
        /** Impresora instalada en Windows; se busca por su nombre. */
        RAW
    }

    private Transporte transporte = Transporte.NONE;

    /** Solo para TCP. */
    private String host;
    private int puerto = 9100;
    private int timeoutMs = 4000;

    /** Solo para RAW: nombre exacto de la impresora en Windows. */
    private String nombreImpresora;

    /** Corta el papel al final del ticket. */
    private boolean cortar = true;

    /** Manda el pulso para abrir el cajon monedero cuando el pago es en efectivo. */
    private boolean abrirCajonEnEfectivo = true;

    /** Ancho del papel en caracteres (80mm en fuente A ~ 42-48). */
    private int anchoCaracteres = 42;

    public Transporte getTransporte() {
        return transporte;
    }

    public void setTransporte(Transporte transporte) {
        this.transporte = transporte;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPuerto() {
        return puerto;
    }

    public void setPuerto(int puerto) {
        this.puerto = puerto;
    }

    public int getTimeoutMs() {
        return timeoutMs;
    }

    public void setTimeoutMs(int timeoutMs) {
        this.timeoutMs = timeoutMs;
    }

    public String getNombreImpresora() {
        return nombreImpresora;
    }

    public void setNombreImpresora(String nombreImpresora) {
        this.nombreImpresora = nombreImpresora;
    }

    public boolean isCortar() {
        return cortar;
    }

    public void setCortar(boolean cortar) {
        this.cortar = cortar;
    }

    public boolean isAbrirCajonEnEfectivo() {
        return abrirCajonEnEfectivo;
    }

    public void setAbrirCajonEnEfectivo(boolean abrirCajonEnEfectivo) {
        this.abrirCajonEnEfectivo = abrirCajonEnEfectivo;
    }

    public int getAnchoCaracteres() {
        return anchoCaracteres;
    }

    public void setAnchoCaracteres(int anchoCaracteres) {
        this.anchoCaracteres = anchoCaracteres;
    }
}
