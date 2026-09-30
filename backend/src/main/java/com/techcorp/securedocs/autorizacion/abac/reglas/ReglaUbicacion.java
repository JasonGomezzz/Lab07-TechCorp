package com.techcorp.securedocs.autorizacion.abac.reglas;

import com.techcorp.securedocs.autorizacion.ContextoAutorizacion;
import com.techcorp.securedocs.autorizacion.ResultadoPolitica;
import com.techcorp.securedocs.autorizacion.abac.ConfigPolitica;
import com.techcorp.securedocs.autorizacion.abac.ReglaAbac;
import org.springframework.stereotype.Component;

@Component
public class ReglaUbicacion implements ReglaAbac {
    public String codigo() { return "P5B_UBICACION"; }

    public ResultadoPolitica evaluar(ContextoAutorizacion c, ConfigPolitica p) {
        String paisRestringido = (String) p.parametros().get("pais");
        boolean cumple = !paisRestringido.equals(c.recurso().pais())
            || paisRestringido.equals(c.entorno().ubicacion());
        return resultado(cumple, cumple ? "Ubicación permitida" : "Ubicación no autorizada");
    }
}
