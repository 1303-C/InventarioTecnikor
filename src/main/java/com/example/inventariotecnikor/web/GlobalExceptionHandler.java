package com.example.inventariotecnikor.web;

import com.example.inventariotecnikor.exception.CodigoQrDuplicadoException;
import com.example.inventariotecnikor.exception.RecursoNoEncontradoException;
import com.example.inventariotecnikor.exception.StockInsuficienteException;
import com.example.inventariotecnikor.exception.TicketException;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Manejo centralizado de errores para TODOS los controladores.
 *
 * @ControllerAdvice = una clase cuyos @ExceptionHandler aplican a toda la
 * app. Cuando un controlador (o un servicio que este llama) lanza una de
 * estas excepciones, en vez de reventar con una pagina fea de error, se
 * ejecuta el metodo de aqui y se muestra una vista con un mensaje claro.
 *
 * Es el equivalente a un middleware de manejo de excepciones en ASP.NET.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    /** Producto/recurso inexistente -> 404 con mensaje. */
    @ExceptionHandler(RecursoNoEncontradoException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String noEncontrado(RecursoNoEncontradoException ex, Model model) {
        model.addAttribute("titulo", "No encontrado");
        model.addAttribute("mensaje", ex.getMessage());
        return "error/generico";
    }

    /**
     * Reglas de negocio incumplidas (stock insuficiente, QR duplicado que
     * no se atrapo antes, cantidad invalida...) -> 400 con mensaje.
     */
    @ExceptionHandler({
            StockInsuficienteException.class,
            CodigoQrDuplicadoException.class,
            TicketException.class,
            IllegalArgumentException.class
    })
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String reglaDeNegocio(RuntimeException ex, Model model) {
        model.addAttribute("titulo", "No se pudo completar la operacion");
        model.addAttribute("mensaje", ex.getMessage());
        return "error/generico";
    }
}
