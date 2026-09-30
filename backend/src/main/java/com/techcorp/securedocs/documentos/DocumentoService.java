package com.techcorp.securedocs.documentos;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import com.fasterxml.jackson.databind.JsonNode;
import com.techcorp.securedocs.auditoria.AuditoriaService;
import com.techcorp.securedocs.auditoria.EventoAuditoria;
import com.techcorp.securedocs.autorizacion.Accion;
import com.techcorp.securedocs.autorizacion.Autorizador;
import com.techcorp.securedocs.autorizacion.Entorno;
import com.techcorp.securedocs.autorizacion.Recurso;
import com.techcorp.securedocs.autorizacion.Sujeto;
import com.techcorp.securedocs.comun.ConflictoEstadoException;
import com.techcorp.securedocs.usuarios.Usuario;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DocumentoService {
    private final DocumentoRepository documentos;
    private final Autorizador autorizador;
    private final AuditoriaService auditoria;
    private final Clock reloj;

    public DocumentoService(DocumentoRepository documentos, Autorizador autorizador,
                            AuditoriaService auditoria, Clock reloj) {
        this.documentos = documentos;
        this.autorizador = autorizador;
        this.auditoria = auditoria;
        this.reloj = reloj;
    }

    @Transactional(readOnly = true)
    public List<DocumentoRespuesta> listar(Usuario usuario, Entorno entorno) {
        Sujeto sujeto = Sujeto.de(usuario);
        List<Documento> disponibles = documentos.findByEstadoNot("ELIMINADO");
        List<DocumentoRespuesta> incluidos = new ArrayList<>();
        for (Documento documento : disponibles) {
            if (autorizador.permiteLecturaEnListado(sujeto, Recurso.de(documento), entorno)) {
                incluidos.add(DocumentoRespuesta.de(documento));
            }
        }
        int omitidos = disponibles.size() - incluidos.size();
        autorizador.exigir(sujeto, Accion.LIST,
            Recurso.noDocumental("documentos:incluidos=" + incluidos.size() + ";omitidos=" + omitidos,
                sujeto.departamento()), entorno);
        return List.copyOf(incluidos);
    }

    @Transactional(readOnly = true)
    public DocumentoRespuesta consultar(long id, Usuario usuario, Entorno entorno) {
        Documento documento = existente(id, usuario, Accion.READ, entorno);
        autorizador.exigir(Sujeto.de(usuario), Accion.READ, Recurso.de(documento), entorno);
        return DocumentoRespuesta.de(documento);
    }

    @Transactional
    public DocumentoRespuesta crear(JsonNode datos, Usuario usuario, Entorno entorno) {
        validarCampos(datos, Set.of("titulo", "descripcion", "nivelConfidencialidad"));
        String titulo = texto(datos, "titulo");
        String descripcion = texto(datos, "descripcion");
        int nivel = entero(datos, "nivelConfidencialidad");
        Documento documento = new Documento();
        documento.setTitulo(titulo);
        documento.setDescripcion(descripcion);
        documento.setNivelConfidencialidad((byte) nivel);
        documento.setPropietario(usuario);
        documento.setDepartamento(usuario.getDepartamento());
        documento.setPais(usuario.getPais());
        documento.setEstado("PENDIENTE");
        documento.setFechaCreacion(LocalDateTime.now(reloj));
        Recurso propuesta = new Recurso("documento-nuevo", usuario.getDepartamento().getCodigo(), nivel,
            usuario.getId(), "PENDIENTE", usuario.getPais());
        autorizador.exigir(Sujeto.de(usuario), Accion.CREATE, propuesta, entorno);
        return DocumentoRespuesta.de(documentos.saveAndFlush(documento));
    }

    @Transactional
    public DocumentoRespuesta modificar(long id, JsonNode datos, Usuario usuario, Entorno entorno) {
        Documento documento = existente(id, usuario, Accion.UPDATE, entorno);
        autorizador.exigir(Sujeto.de(usuario), Accion.UPDATE, Recurso.de(documento), entorno);
        validarCampos(datos, Set.of("titulo", "descripcion"));
        if (datos.has("titulo")) documento.setTitulo(texto(datos, "titulo"));
        if (datos.has("descripcion")) documento.setDescripcion(texto(datos, "descripcion"));
        return DocumentoRespuesta.de(documentos.saveAndFlush(documento));
    }

    @Transactional
    public void eliminar(long id, Usuario usuario, Entorno entorno) {
        Documento documento = existente(id, usuario, Accion.DELETE, entorno);
        autorizador.exigir(Sujeto.de(usuario), Accion.DELETE, Recurso.de(documento), entorno);
        documento.setEstado("ELIMINADO");
        documento.setEliminadoEn(LocalDateTime.now(reloj));
        documentos.saveAndFlush(documento);
    }

    @Transactional
    public DocumentoRespuesta aprobar(long id, Usuario usuario, Entorno entorno) {
        Documento documento = existente(id, usuario, Accion.APPROVE, entorno);
        autorizador.exigir(Sujeto.de(usuario), Accion.APPROVE, Recurso.de(documento), entorno);
        if (!"PENDIENTE".equals(documento.getEstado())) {
            throw new ConflictoEstadoException("Solo se puede aprobar un documento pendiente");
        }
        documento.setEstado("PUBLICADO");
        return DocumentoRespuesta.de(documentos.saveAndFlush(documento));
    }

    private Documento existente(long id, Usuario usuario, Accion accion, Entorno entorno) {
        Documento documento = documentos.findWithPropietarioAndDepartamentoById(id).orElse(null);
        if (documento == null || "ELIMINADO".equals(documento.getEstado())) {
            Sujeto sujeto = Sujeto.de(usuario);
            auditoria.registrarEvento(new EventoAuditoria(sujeto.username(), sujeto.rol(),
                sujeto.departamento(), "documento-" + id, accion.name(), "DENEGADO",
                "RECURSO", "Documento no encontrado", List.of(), null, entorno));
            throw new NoSuchElementException("Documento no encontrado");
        }
        return documento;
    }

    private void validarCampos(JsonNode datos, Set<String> permitidos) {
        if (datos == null || !datos.isObject() || !datos.fieldNames().hasNext()) {
            throw new IllegalArgumentException("Se requiere un objeto con campos válidos");
        }
        datos.fieldNames().forEachRemaining(campo -> {
            if (!permitidos.contains(campo)) {
                throw new IllegalArgumentException("Campo no editable: " + campo);
            }
        });
    }

    private String texto(JsonNode datos, String campo) {
        JsonNode valor = datos.get(campo);
        if (valor == null || !valor.isTextual() || valor.asText().isBlank()) {
            throw new IllegalArgumentException("El campo " + campo + " es obligatorio y no puede estar vacío");
        }
        return valor.asText().trim();
    }

    private int entero(JsonNode datos, String campo) {
        JsonNode valor = datos.get(campo);
        if (valor == null || !valor.isIntegralNumber() || valor.asInt() < 0 || valor.asInt() > 5) {
            throw new IllegalArgumentException("El campo " + campo + " debe estar entre 0 y 5");
        }
        return valor.asInt();
    }

    public record DocumentoRespuesta(Long id, String titulo, String descripcion, Long propietarioId,
                                     String departamento, int nivelConfidencialidad, String estado,
                                     String pais, LocalDateTime fechaCreacion) {
        static DocumentoRespuesta de(Documento d) {
            return new DocumentoRespuesta(d.getId(), d.getTitulo(), d.getDescripcion(),
                d.getPropietario().getId(), d.getDepartamento().getCodigo(), d.getNivelConfidencialidad(),
                d.getEstado(), d.getPais(), d.getFechaCreacion());
        }
    }
}
