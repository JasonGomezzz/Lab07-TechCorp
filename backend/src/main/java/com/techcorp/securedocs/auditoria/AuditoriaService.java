package com.techcorp.securedocs.auditoria;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Map;
import com.techcorp.securedocs.autorizacion.ContextoAutorizacion;
import com.techcorp.securedocs.autorizacion.Decision;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditoriaService {
    private final AuditoriaRepository repositorio;
    private final Clock reloj;

    public AuditoriaService(AuditoriaRepository repositorio, Clock reloj) {
        this.repositorio = repositorio;
        this.reloj = reloj;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrar(ContextoAutorizacion contexto, Decision decision) {
        guardar(new EventoAuditoria(contexto.sujeto().username(), contexto.sujeto().rol(),
            contexto.sujeto().departamento(), contexto.recurso().identificador(),
            contexto.accion().name(), decision.permitido() ? "PERMITIDO" : "DENEGADO",
            decision.capa(), String.join("; ", decision.motivos()),
            decision.politicas().stream().map(p -> Map.<String, Object>of(
                "codigo", p.codigo(), "cumple", p.cumple(), "detalle", p.detalle())).toList(),
            contexto.recurso().departamento(), contexto.entorno()));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrarEvento(EventoAuditoria evento) {
        guardar(evento);
    }

    private void guardar(EventoAuditoria evento) {
        Auditoria registro = new Auditoria();
        registro.setFechaHora(LocalDateTime.now(reloj));
        registro.setUsuario(evento.usuario());
        registro.setUsuarioRol(evento.usuarioRol());
        registro.setUsuarioDepartamento(evento.usuarioDepartamento());
        registro.setRecurso(evento.recurso());
        registro.setAccion(evento.accion());
        registro.setResultado(evento.resultado());
        registro.setCapa(evento.capa());
        registro.setMotivo(evento.motivo());
        registro.setPoliticasEvaluadas(evento.politicasEvaluadas());
        registro.setIp(evento.entorno().direccionIp());
        registro.setUbicacion(evento.entorno().ubicacion());
        registro.setDispositivo(evento.entorno().dispositivo());
        registro.setDepartamentoRecurso(evento.departamentoRecurso());
        repositorio.saveAndFlush(registro);
    }
}
