package com.techcorp.securedocs.autorizacion.abac.reglas;

import java.time.LocalTime;
import com.techcorp.securedocs.autorizacion.ContextoAutorizacion;
import com.techcorp.securedocs.autorizacion.ResultadoPolitica;
import com.techcorp.securedocs.autorizacion.abac.ConfigPolitica;
import com.techcorp.securedocs.autorizacion.abac.ReglaAbac;
import org.springframework.stereotype.Component;

@Component
public class ReglaHorario implements ReglaAbac {
    public String codigo() { return "P4_HORARIO"; }

    public ResultadoPolitica evaluar(ContextoAutorizacion c, ConfigPolitica p) {
        int minimo = ((Number) p.parametros().get("nivelMinimo")).intValue();
        if (c.recurso().nivelConfidencialidad() < minimo) {
            return resultado(true, "El documento no requiere horario restringido");
        }
        LocalTime inicio = LocalTime.parse((String) p.parametros().get("inicio"));
        LocalTime fin = LocalTime.parse((String) p.parametros().get("fin"));
        boolean cumple = !c.entorno().hora().isBefore(inicio) && c.entorno().hora().isBefore(fin);
        return resultado(cumple, cumple ? "Dentro del horario permitido" : "Fuera del horario permitido");
    }
}
