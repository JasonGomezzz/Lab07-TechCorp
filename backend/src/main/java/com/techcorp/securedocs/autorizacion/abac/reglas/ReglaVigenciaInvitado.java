package com.techcorp.securedocs.autorizacion.abac.reglas;

import com.techcorp.securedocs.autorizacion.ContextoAutorizacion;
import com.techcorp.securedocs.autorizacion.ResultadoPolitica;
import com.techcorp.securedocs.autorizacion.abac.ConfigPolitica;
import com.techcorp.securedocs.autorizacion.abac.ReglaAbac;
import org.springframework.stereotype.Component;

@Component
public class ReglaVigenciaInvitado implements ReglaAbac {
    public String codigo() { return "P9_VIGENCIA_INVITADO"; }

    public ResultadoPolitica evaluar(ContextoAutorizacion c, ConfigPolitica p) {
        if (!"INVITADO".equals(c.sujeto().rol())) {
            return resultado(true, "No es invitado");
        }
        boolean cumple = c.sujeto().accesoHasta() != null
            && !c.entorno().fecha().isAfter(c.sujeto().accesoHasta());
        return resultado(cumple, cumple ? "Acceso temporal vigente" : "Acceso temporal vencido");
    }
}
