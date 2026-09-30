package com.techcorp.securedocs.autorizacion.abac.reglas;

import com.techcorp.securedocs.autorizacion.ContextoAutorizacion;
import com.techcorp.securedocs.autorizacion.ResultadoPolitica;
import com.techcorp.securedocs.autorizacion.abac.ConfigPolitica;
import com.techcorp.securedocs.autorizacion.abac.ReglaAbac;
import org.springframework.stereotype.Component;

@Component
public class ReglaPropiedad implements ReglaAbac {
    public String codigo() { return "P3_PROPIEDAD"; }

    public ResultadoPolitica evaluar(ContextoAutorizacion c, ConfigPolitica p) {
        boolean cumple = c.sujeto().id() == c.recurso().propietarioId();
        return resultado(cumple, cumple ? "Es propietario" : "No es propietario");
    }
}
