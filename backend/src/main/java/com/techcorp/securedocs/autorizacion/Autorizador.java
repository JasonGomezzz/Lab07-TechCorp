package com.techcorp.securedocs.autorizacion;

import java.util.List;
import com.techcorp.securedocs.auditoria.AuditoriaService;
import com.techcorp.securedocs.autorizacion.abac.MotorAbac;
import com.techcorp.securedocs.autorizacion.rbac.ServicioRbac;
import com.techcorp.securedocs.politicas.ConfiguracionPoliticas;
import com.techcorp.securedocs.comun.ConflictoEstadoException;
import org.springframework.stereotype.Service;

@Service
public class Autorizador {
    private final ServicioRbac rbac;
    private final MotorAbac abac;
    private final ConfiguracionPoliticas politicas;
    private final AuditoriaService auditoria;

    public Autorizador(ServicioRbac rbac, MotorAbac abac, ConfiguracionPoliticas politicas,
                       AuditoriaService auditoria) {
        this.rbac = rbac;
        this.abac = abac;
        this.politicas = politicas;
        this.auditoria = auditoria;
    }

    public Decision exigir(Sujeto sujeto, Accion accion, Recurso recurso, Entorno entorno) {
        ContextoAutorizacion contexto = new ContextoAutorizacion(sujeto, accion, recurso, entorno);
        if (!"ACTIVO".equals(sujeto.estado())) {
            return denegar(contexto, Decision.denegada("AUTH",
                List.of(new ResultadoPolitica("P7_ESTADO_USUARIO", false, "Usuario no activo")),
                List.of("Usuario no activo")));
        }
        if (!rbac.permite(sujeto.rol(), accion.permiso())) {
            return denegar(contexto, Decision.denegada("RBAC", List.of(),
                List.of("El rol no permite " + accion.name())));
        }
        List<ResultadoPolitica> resultados = abac.evaluar(contexto, politicas.activas());
        List<String> fallos = resultados.stream().filter(p -> !p.cumple())
            .map(p -> p.codigo() + ": " + p.detalle()).toList();
        if (!fallos.isEmpty()) {
            return denegar(contexto, Decision.denegada("ABAC", resultados, fallos));
        }
        Decision decision = Decision.permitida(resultados);
        auditoria.registrar(contexto, decision);
        return decision;
    }

    private Decision denegar(ContextoAutorizacion contexto, Decision decision) {
        auditoria.registrar(contexto, decision);
        throw new AccesoDenegadoException(decision);
    }

    public String departamentoVisibleEnAuditoria(Sujeto sujeto) {
        return "GERENTE".equals(sujeto.rol()) ? sujeto.departamento() : null;
    }

    public boolean permiteLecturaEnListado(Sujeto sujeto, Recurso recurso, Entorno entorno) {
        if (!"ACTIVO".equals(sujeto.estado()) || !rbac.permite(sujeto.rol(), Accion.READ.permiso())) {
            return false;
        }
        return abac.evaluar(new ContextoAutorizacion(sujeto, Accion.READ, recurso, entorno),
            politicas.activas()).stream().allMatch(ResultadoPolitica::cumple);
    }

    public void validarCambioPropio(Sujeto actor, long objetivoId, String nuevoRol, String nuevoEstado) {
        if (actor.id() == objetivoId && "ADMINISTRADOR".equals(actor.rol())
            && (!"ADMINISTRADOR".equals(nuevoRol) || !"ACTIVO".equals(nuevoEstado))) {
            throw new ConflictoEstadoException("Un administrador no puede desactivarse ni quitarse su rol");
        }
    }
}
