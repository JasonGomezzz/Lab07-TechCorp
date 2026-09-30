package com.techcorp.securedocs.auditoria;

import java.time.LocalDateTime;
import java.util.List;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConsultaAuditoriaService {
    private final AuditoriaRepository repositorio;
    private final ObjectMapper json;

    public ConsultaAuditoriaService(AuditoriaRepository repositorio, ObjectMapper json) {
        this.repositorio = repositorio;
        this.json = json;
    }

    @Transactional(readOnly = true)
    public Page<Auditoria> consultar(Filtros filtros, String departamentoVisible, int pagina, int tamano) {
        return repositorio.findAll(especificacion(filtros, departamentoVisible),
            PageRequest.of(pagina, tamano, Sort.by(Sort.Direction.DESC, "fechaHora")));
    }

    @Transactional(readOnly = true)
    public String exportarCsv(Filtros filtros, String departamentoVisible) {
        List<Auditoria> registros = repositorio.findAll(especificacion(filtros, departamentoVisible),
            Sort.by(Sort.Direction.DESC, "fechaHora"));
        StringBuilder csv = new StringBuilder("id,fecha_hora,usuario,usuario_rol,usuario_departamento,recurso,accion,resultado,capa,motivo,politicas_evaluadas,ip,ubicacion,dispositivo,departamento_recurso\n");
        for (Auditoria r : registros) {
            String politicas;
            try {
                politicas = json.writeValueAsString(r.getPoliticasEvaluadas());
            } catch (JsonProcessingException ex) {
                throw new IllegalStateException("No se pudo exportar la auditoría", ex);
            }
            csv.append(fila(r.getId(), r.getFechaHora(), r.getUsuario(), r.getUsuarioRol(),
                r.getUsuarioDepartamento(), r.getRecurso(), r.getAccion(), r.getResultado(), r.getCapa(),
                r.getMotivo(), politicas, r.getIp(), r.getUbicacion(), r.getDispositivo(),
                r.getDepartamentoRecurso()));
        }
        return csv.toString();
    }

    private Specification<Auditoria> especificacion(Filtros f, String departamentoVisible) {
        Specification<Auditoria> s = (raiz, consulta, cb) -> cb.conjunction();
        if (f.usuario() != null) s = s.and((r, q, cb) -> cb.equal(r.get("usuario"), f.usuario()));
        if (f.recurso() != null) s = s.and((r, q, cb) -> cb.equal(r.get("recurso"), f.recurso()));
        if (f.accion() != null) s = s.and((r, q, cb) -> cb.equal(r.get("accion"), f.accion()));
        if (f.resultado() != null) s = s.and((r, q, cb) -> cb.equal(r.get("resultado"), f.resultado()));
        if (f.desde() != null) s = s.and((r, q, cb) -> cb.greaterThanOrEqualTo(r.get("fechaHora"), f.desde()));
        if (f.hasta() != null) s = s.and((r, q, cb) -> cb.lessThanOrEqualTo(r.get("fechaHora"), f.hasta()));
        if (departamentoVisible != null) {
            s = s.and((r, q, cb) -> cb.equal(r.get("departamentoRecurso"), departamentoVisible));
        }
        return s;
    }

    private String fila(Object... columnas) {
        StringBuilder fila = new StringBuilder();
        for (int i = 0; i < columnas.length; i++) {
            if (i > 0) fila.append(',');
            fila.append(escapar(columnas[i]));
        }
        return fila.append('\n').toString();
    }

    private String escapar(Object valor) {
        if (valor == null) return "";
        String texto = valor.toString();
        // La comilla inicial evita que hojas de cálculo ejecuten fórmulas del registro.
        if (!texto.isEmpty() && "=+-@".indexOf(texto.charAt(0)) >= 0) texto = "'" + texto;
        return '"' + texto.replace("\"", "\"\"") + '"';
    }

    public record Filtros(String usuario, String recurso, String accion, String resultado,
                          LocalDateTime desde, LocalDateTime hasta) {
    }
}
