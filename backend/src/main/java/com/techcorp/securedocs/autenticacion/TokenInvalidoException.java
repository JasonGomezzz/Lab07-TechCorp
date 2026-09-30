package com.techcorp.securedocs.autenticacion;

public class TokenInvalidoException extends RuntimeException {
    public TokenInvalidoException(String motivo) {
        super(motivo);
    }
}
