package com.techcorp.securedocs.autorizacion.abac.reglas;

import com.techcorp.securedocs.autorizacion.ContextoAutorizacion;
import com.techcorp.securedocs.autorizacion.ResultadoPolitica;
import com.techcorp.securedocs.autorizacion.abac.ConfigPolitica;
import com.techcorp.securedocs.autorizacion.abac.ReglaAbac;
import org.springframework.stereotype.Component;

@Component
public class ReglaEstadoUsuario implements ReglaAbac {
    public String codigo() { return "P7_ESTADO_USUARIO"; }

    public ResultadoPolitica evaluar(ContextoAutorizacion c, ConfigPolitica p) {
        boolean cumple = "ACTIVO".equals(c.sujeto().estado());
        return resultado(cumple, cumple ? "Usuario activo" : "Usuario no activo");
    }
}
