package com.techcorp.securedocs.autorizacion.abac;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import com.techcorp.securedocs.autorizacion.ContextoAutorizacion;
import com.techcorp.securedocs.autorizacion.ResultadoPolitica;
import org.springframework.stereotype.Component;

@Component
public class MotorAbac {
    private final Map<String, ReglaAbac> reglas;

    public MotorAbac(List<ReglaAbac> reglas) {
        this.reglas = reglas.stream().collect(Collectors.toUnmodifiableMap(ReglaAbac::codigo, Function.identity()));
    }

    public List<ResultadoPolitica> evaluar(ContextoAutorizacion contexto, List<ConfigPolitica> configuraciones) {
        List<ResultadoPolitica> resultados = new ArrayList<>();
        for (ConfigPolitica configuracion : configuraciones) {
            if (!aplica(contexto, configuracion)) {
                continue;
            }
            ReglaAbac regla = reglas.get(configuracion.codigo());
            if (regla == null) {
                resultados.add(new ResultadoPolitica(configuracion.codigo(), false,
                    "No existe evaluador para la política"));
                continue;
            }
            resultados.add(regla.evaluar(contexto, configuracion));
        }
        return List.copyOf(resultados);
    }

    private boolean aplica(ContextoAutorizacion contexto, ConfigPolitica config) {
        if (config.rolesExentos().contains(contexto.sujeto().rol())) {
            return false;
        }
        if ("P7_ESTADO_USUARIO".equals(config.codigo())) {
            return true;
        }
        if (!contexto.accion().documental() || contexto.accion().name().equals("LIST")) {
            return false;
        }
        return config.acciones().contains("*") || config.acciones().contains(contexto.accion().name());
    }
}
