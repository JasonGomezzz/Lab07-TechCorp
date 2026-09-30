package com.techcorp.securedocs.autorizacion;

public record ContextoAutorizacion(Sujeto sujeto, Accion accion, Recurso recurso, Entorno entorno) {
}
