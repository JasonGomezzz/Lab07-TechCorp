package com.techcorp.securedocs.autorizacion.abac;

import java.util.List;
import java.util.Map;
import com.techcorp.securedocs.politicas.Politica;

public record ConfigPolitica(String codigo, List<String> acciones, List<String> rolesExentos,
                             Map<String, Object> parametros) {
    public static ConfigPolitica de(Politica politica) {
        return new ConfigPolitica(politica.getCodigo(), List.copyOf(politica.getAcciones()),
            List.copyOf(politica.getRolesExentos()), Map.copyOf(politica.getParametros()));
    }
}
