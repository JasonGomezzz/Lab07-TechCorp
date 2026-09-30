package com.techcorp.securedocs.autorizacion.abac.reglas;

import com.techcorp.securedocs.autorizacion.ContextoAutorizacion;
import com.techcorp.securedocs.autorizacion.ResultadoPolitica;
import com.techcorp.securedocs.autorizacion.abac.ConfigPolitica;
import com.techcorp.securedocs.autorizacion.abac.ReglaAbac;
import org.springframework.stereotype.Component;

@Component
public class ReglaDispositivo implements ReglaAbac {
    public String codigo() { return "P6_DISPOSITIVO"; }

    public ResultadoPolitica evaluar(ContextoAutorizacion c, ConfigPolitica p) {
        int minimo = ((Number) p.parametros().get("nivelMinimo")).intValue();
        boolean cumple = c.recurso().nivelConfidencialidad() < minimo
            || "CORPORATIVO".equals(c.entorno().dispositivo());
        return resultado(cumple, cumple ? "Dispositivo permitido" : "Se requiere dispositivo corporativo");
    }
}
