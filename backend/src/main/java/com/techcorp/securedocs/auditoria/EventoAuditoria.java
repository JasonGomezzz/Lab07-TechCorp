package com.techcorp.securedocs.auditoria;

import java.util.List;
import java.util.Map;
import com.techcorp.securedocs.autorizacion.Entorno;

public record EventoAuditoria(String usuario, String usuarioRol, String usuarioDepartamento,
                              String recurso, String accion, String resultado, String capa,
                              String motivo, List<Map<String, Object>> politicasEvaluadas,
                              String departamentoRecurso, Entorno entorno) {
}
