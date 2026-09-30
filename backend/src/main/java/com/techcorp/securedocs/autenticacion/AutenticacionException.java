package com.techcorp.securedocs.autenticacion;

import org.springframework.http.HttpStatus;

public class AutenticacionException extends RuntimeException {
    private final HttpStatus estado;
    private final String codigo;

    public AutenticacionException(HttpStatus estado, String codigo, String mensaje) {
        super(mensaje);
        this.estado = estado;
        this.codigo = codigo;
    }

    public HttpStatus getEstado() {
        return estado;
    }

    public String getCodigo() {
        return codigo;
    }
}
