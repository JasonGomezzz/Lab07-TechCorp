package com.techcorp.securedocs.autorizacion.abac.reglas;

import com.techcorp.securedocs.autorizacion.ContextoAutorizacion;
import com.techcorp.securedocs.autorizacion.ResultadoPolitica;
import com.techcorp.securedocs.autorizacion.abac.ConfigPolitica;
import com.techcorp.securedocs.autorizacion.abac.ReglaAbac;
import org.springframework.stereotype.Component;

@Component
public class ReglaNivelSeguridad implements ReglaAbac {
    public String codigo() { return "P2_NIVEL_SEGURIDAD"; }

    public ResultadoPolitica evaluar(ContextoAutorizacion c, ConfigPolitica p) {
        boolean cumple = c.sujeto().nivelSeguridad() >= c.recurso().nivelConfidencialidad();
        return resultado(cumple, cumple ? "Nivel suficiente" : "Nivel de seguridad insuficiente");
    }
}
