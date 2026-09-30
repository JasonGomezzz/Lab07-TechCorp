package com.techcorp.securedocs.documentos;

import java.util.List;
import com.fasterxml.jackson.databind.JsonNode;
import com.techcorp.securedocs.entorno.EntornoResolver;
import com.techcorp.securedocs.usuarios.Usuario;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/documentos")
public class DocumentoController {
    private final DocumentoService documentos;
    private final EntornoResolver entorno;

    public DocumentoController(DocumentoService documentos, EntornoResolver entorno) {
        this.documentos = documentos;
        this.entorno = entorno;
    }

    @GetMapping
    public List<DocumentoService.DocumentoRespuesta> listar(@AuthenticationPrincipal Usuario usuario,
                                                             HttpServletRequest request) {
        return documentos.listar(usuario, entorno.resolver(request));
    }

    @GetMapping("/{id}")
    public DocumentoService.DocumentoRespuesta consultar(@PathVariable long id,
                                                          @AuthenticationPrincipal Usuario usuario,
                                                          HttpServletRequest request) {
        return documentos.consultar(id, usuario, entorno.resolver(request));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DocumentoService.DocumentoRespuesta crear(@RequestBody JsonNode datos,
                                                     @AuthenticationPrincipal Usuario usuario,
                                                     HttpServletRequest request) {
        return documentos.crear(datos, usuario, entorno.resolver(request));
    }

    @PutMapping("/{id}")
    public DocumentoService.DocumentoRespuesta modificar(@PathVariable long id, @RequestBody JsonNode datos,
                                                          @AuthenticationPrincipal Usuario usuario,
                                                          HttpServletRequest request) {
        return documentos.modificar(id, datos, usuario, entorno.resolver(request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable long id, @AuthenticationPrincipal Usuario usuario,
                         HttpServletRequest request) {
        documentos.eliminar(id, usuario, entorno.resolver(request));
    }

    @PostMapping("/{id}/aprobar")
    public DocumentoService.DocumentoRespuesta aprobar(@PathVariable long id,
                                                        @AuthenticationPrincipal Usuario usuario,
                                                        HttpServletRequest request) {
        return documentos.aprobar(id, usuario, entorno.resolver(request));
    }
}
