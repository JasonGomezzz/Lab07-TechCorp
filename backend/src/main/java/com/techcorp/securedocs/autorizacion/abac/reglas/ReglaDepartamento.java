package com.techcorp.securedocs.autorizacion.abac.reglas;

import com.techcorp.securedocs.autorizacion.ContextoAutorizacion;
import com.techcorp.securedocs.autorizacion.ResultadoPolitica;
import com.techcorp.securedocs.autorizacion.abac.ConfigPolitica;
import com.techcorp.securedocs.autorizacion.abac.ReglaAbac;
import org.springframework.stereotype.Component;

@Component
public class ReglaDepartamento implements ReglaAbac {
    public String codigo() { return "P1_DEPARTAMENTO"; }

    public ResultadoPolitica evaluar(ContextoAutorizacion c, ConfigPolitica p) {
        boolean cumple = c.sujeto().departamento().equals(c.recurso().departamento());
        return resultado(cumple, cumple ? "Departamento coincidente" : "Departamento distinto");
    }
}
