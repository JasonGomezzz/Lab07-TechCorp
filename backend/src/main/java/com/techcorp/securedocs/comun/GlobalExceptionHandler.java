package com.techcorp.securedocs.comun;

import java.time.DateTimeException;
import java.util.List;
import java.util.NoSuchElementException;
import com.techcorp.securedocs.autorizacion.AccesoDenegadoException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.annotation.ResponseStatus;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private final boolean mostrarDetalle;

    public GlobalExceptionHandler(@Value("${securedocs.mostrar-detalle-denegacion:false}") boolean mostrarDetalle) {
        this.mostrarDetalle = mostrarDetalle;
    }

    @ExceptionHandler(AccesoDenegadoException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ProblemDetail accesoDenegado(AccesoDenegadoException ex) {
        ProblemDetail p = ProblemDetail.forStatus(HttpStatus.FORBIDDEN);
        p.setTitle("Acceso denegado");
        p.setDetail("La operación no está autorizada");
        p.setProperty("codigo", ex.getDecision().capa() + "_DENEGADO");
        if (mostrarDetalle) {
            List<String> fallos = ex.getDecision().politicas().stream().filter(r -> !r.cumple())
                .map(r -> r.codigo()).toList();
            p.setProperty("politicas", fallos);
            p.setProperty("motivos", ex.getDecision().motivos());
        }
        return p;
    }

    @ExceptionHandler({IllegalArgumentException.class, DateTimeException.class,
        MethodArgumentNotValidException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ProblemDetail datosInvalidos(Exception ex) {
        ProblemDetail p = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Solicitud inválida");
        p.setTitle("Datos inválidos");
        p.setProperty("codigo", "DATOS_INVALIDOS");
        return p;
    }

    @ExceptionHandler(NoSuchElementException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ProblemDetail noEncontrado(NoSuchElementException ex) {
        ProblemDetail p = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "Recurso no encontrado");
        p.setTitle("No encontrado");
        p.setProperty("codigo", "NO_ENCONTRADO");
        return p;
    }

    @ExceptionHandler({ConflictoEstadoException.class, DataIntegrityViolationException.class})
    @ResponseStatus(HttpStatus.CONFLICT)
    public ProblemDetail conflicto(Exception ex) {
        String detalle = ex instanceof ConflictoEstadoException ? ex.getMessage() : "Los datos ya existen o están en uso";
        ProblemDetail p = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, detalle);
        p.setTitle("Conflicto de estado");
        p.setProperty("codigo", "CONFLICTO_ESTADO");
        return p;
    }
}
