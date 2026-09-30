package com.techcorp.securedocs.usuarios;

import java.util.List;
import com.techcorp.securedocs.entorno.EntornoResolver;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/usuarios")
public class UsuarioController {
    private final UsuarioService usuarios;
    private final EntornoResolver entorno;

    public UsuarioController(UsuarioService usuarios, EntornoResolver entorno) {
        this.usuarios = usuarios;
        this.entorno = entorno;
    }

    @GetMapping
    public List<UsuarioService.UsuarioRespuesta> listar(@AuthenticationPrincipal Usuario actor,
                                                         HttpServletRequest request) {
        return usuarios.listar(actor, entorno.resolver(request));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioService.UsuarioRespuesta crear(@RequestBody UsuarioService.UsuarioCrear datos,
                                                 @AuthenticationPrincipal Usuario actor,
                                                 HttpServletRequest request) {
        return usuarios.crear(datos, actor, entorno.resolver(request));
    }

    @PutMapping("/{id}")
    public UsuarioService.UsuarioRespuesta modificar(@PathVariable long id,
                                                     @RequestBody UsuarioService.UsuarioActualizar datos,
                                                     @AuthenticationPrincipal Usuario actor,
                                                     HttpServletRequest request) {
        return usuarios.modificar(id, datos, actor, entorno.resolver(request));
    }
}
