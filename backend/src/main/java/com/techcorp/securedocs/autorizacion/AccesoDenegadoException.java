package com.techcorp.securedocs.autorizacion;

public class AccesoDenegadoException extends RuntimeException {
    private final Decision decision;

    public AccesoDenegadoException(Decision decision) {
        super(String.join("; ", decision.motivos()));
        this.decision = decision;
    }

    public Decision getDecision() {
        return decision;
    }
}
