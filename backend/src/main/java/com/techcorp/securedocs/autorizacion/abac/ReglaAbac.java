package com.techcorp.securedocs.autorizacion.abac;

import com.techcorp.securedocs.autorizacion.ContextoAutorizacion;
import com.techcorp.securedocs.autorizacion.ResultadoPolitica;

public interface ReglaAbac {
    String codigo();
    ResultadoPolitica evaluar(ContextoAutorizacion contexto, ConfigPolitica configuracion);

    default ResultadoPolitica resultado(boolean cumple, String detalle) {
        return new ResultadoPolitica(codigo(), cumple, detalle);
    }
}
