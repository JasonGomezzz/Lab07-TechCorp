package com.techcorp.securedocs.auditoria;

import java.time.LocalDateTime;
import java.util.List;
import com.techcorp.securedocs.autorizacion.Accion;
import com.techcorp.securedocs.autorizacion.Autorizador;
import com.techcorp.securedocs.autorizacion.Recurso;
import com.techcorp.securedocs.autorizacion.Sujeto;
import com.techcorp.securedocs.entorno.EntornoResolver;
import com.techcorp.securedocs.usuarios.Usuario;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuditoriaController {
    private final Autorizador autorizador;
    private final EntornoResolver entorno;
    private final ConsultaAuditoriaService consulta;

    public AuditoriaController(Autorizador autorizador, EntornoResolver entorno,
                               ConsultaAuditoriaService consulta) {
        this.autorizador = autorizador;
        this.entorno = entorno;
        this.consulta = consulta;
    }

    @GetMapping("/auditoria")
    public ResponseEntity<?> listar(@AuthenticationPrincipal Usuario usuario, HttpServletRequest request,
                                    @RequestParam(name = "usuario", required = false) String usuarioFiltro,
                                    @RequestParam(required = false) String recurso,
                                    @RequestParam(required = false) String accion,
                                    @RequestParam(required = false) String resultado,
                                    @RequestParam(required = false)
                                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime desde,
                                    @RequestParam(required = false)
                                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime hasta,
                                    @RequestParam(defaultValue = "0") int pagina,
                                    @RequestParam(defaultValue = "20") int tamano,
                                    @RequestParam(required = false) String formato) {
        Sujeto sujeto = Sujeto.de(usuario);
        autorizador.exigir(sujeto, Accion.AUDIT_READ, Recurso.noDocumental("auditoria", sujeto.departamento()),
            entorno.resolver(request));
        String alcance = autorizador.departamentoVisibleEnAuditoria(sujeto);
        ConsultaAuditoriaService.Filtros filtros =
            new ConsultaAuditoriaService.Filtros(usuarioFiltro, recurso, accion, resultado, desde, hasta);
        if ("csv".equalsIgnoreCase(formato)) {
            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=auditoria.csv")
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .body(consulta.exportarCsv(filtros, alcance));
        }
        if (pagina < 0 || tamano < 1 || tamano > 100) {
            throw new IllegalArgumentException("La paginación debe usar pagina >= 0 y tamano entre 1 y 100");
        }
        Page<Auditoria> datos = consulta.consultar(filtros, alcance, pagina, tamano);
        return ResponseEntity.ok(new PaginaAuditoria(datos.getContent(), datos.getTotalElements(),
            datos.getNumber(), datos.getSize(), datos.getTotalPages()));
    }

    public record PaginaAuditoria(List<Auditoria> content, long totalElements,
                                  int number, int size, int totalPages) {
    }
}
