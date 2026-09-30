package com.techcorp.securedocs.autorizacion;

import java.util.List;

public record Decision(boolean permitido, String capa, List<ResultadoPolitica> politicas,
                       List<String> motivos) {
    public static Decision permitida(List<ResultadoPolitica> politicas) {
        return new Decision(true, "ABAC", List.copyOf(politicas), List.of("RBAC y ABAC permitidos"));
    }

    public static Decision denegada(String capa, List<ResultadoPolitica> politicas, List<String> motivos) {
        return new Decision(false, capa, List.copyOf(politicas), List.copyOf(motivos));
    }
}
