package com.techcorp.securedocs.autorizacion.abac.reglas;

import com.techcorp.securedocs.autorizacion.ContextoAutorizacion;
import com.techcorp.securedocs.autorizacion.ResultadoPolitica;
import com.techcorp.securedocs.autorizacion.abac.ConfigPolitica;
import com.techcorp.securedocs.autorizacion.abac.ReglaAbac;
import org.springframework.stereotype.Component;

@Component
public class ReglaInvitado implements ReglaAbac {
    public String codigo() { return "P8_INVITADO"; }

    public ResultadoPolitica evaluar(ContextoAutorizacion c, ConfigPolitica p) {
        if (!"INVITADO".equals(c.sujeto().rol())) {
            return resultado(true, "No es invitado");
        }
        int maximo = ((Number) p.parametros().get("nivelMaximo")).intValue();
        boolean cumple = "EXTERNO".equals(c.sujeto().tipoContrato())
            && c.recurso().nivelConfidencialidad() <= maximo
            && "PUBLICADO".equals(c.recurso().estado());
        return resultado(cumple, cumple ? "Acceso de invitado permitido" : "Invitado sin condiciones de acceso");
    }
}
