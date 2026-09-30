package com.techcorp.securedocs.autorizacion;

import com.techcorp.securedocs.documentos.Documento;

public record Recurso(String identificador, String departamento, int nivelConfidencialidad,
                      long propietarioId, String estado, String pais) {
    public static Recurso de(Documento documento) {
        return new Recurso("documento-" + documento.getId(), documento.getDepartamento().getCodigo(),
            documento.getNivelConfidencialidad(), documento.getPropietario().getId(),
            documento.getEstado(), documento.getPais());
    }

    public static Recurso noDocumental(String identificador, String departamento) {
        return new Recurso(identificador, departamento, 0, 0, "", "");
    }
}
