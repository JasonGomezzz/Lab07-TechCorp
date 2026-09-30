package com.techcorp.securedocs.autorizacion.abac.reglas;

import com.techcorp.securedocs.autorizacion.ContextoAutorizacion;
import com.techcorp.securedocs.autorizacion.ResultadoPolitica;
import com.techcorp.securedocs.autorizacion.abac.ConfigPolitica;
import com.techcorp.securedocs.autorizacion.abac.ReglaAbac;
import org.springframework.stereotype.Component;

@Component
public class ReglaPaisUsuario implements ReglaAbac {
    public String codigo() { return "P5A_PAIS_USUARIO"; }

    public ResultadoPolitica evaluar(ContextoAutorizacion c, ConfigPolitica p) {
        boolean cumple = c.sujeto().pais().equals(c.recurso().pais());
        return resultado(cumple, cumple ? "País coincidente" : "País del usuario distinto");
    }
}
