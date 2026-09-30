package com.techcorp.securedocs.autenticacion;

import java.util.Set;
import java.util.List;
import com.techcorp.securedocs.auditoria.AuditoriaService;
import com.techcorp.securedocs.auditoria.EventoAuditoria;
import com.techcorp.securedocs.autorizacion.Entorno;
import com.techcorp.securedocs.autorizacion.rbac.ServicioRbac;
import com.techcorp.securedocs.entorno.EntornoResolver;
import com.techcorp.securedocs.usuarios.Usuario;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final ServicioAutenticacion autenticacion;
    private final ServicioJwt jwt;
    private final ServicioRbac rbac;
    private final AuditoriaService auditoria;
    private final EntornoResolver entornoResolver;

    public AuthController(ServicioAutenticacion autenticacion, ServicioJwt jwt, ServicioRbac rbac,
                          AuditoriaService auditoria, EntornoResolver entornoResolver) {
        this.autenticacion = autenticacion;
        this.jwt = jwt;
        this.rbac = rbac;
        this.auditoria = auditoria;
        this.entornoResolver = entornoResolver;
    }

    @PostMapping("/login")
    public TokenRespuesta login(@Valid @RequestBody Credenciales datos, HttpServletRequest request) {
        Entorno entorno = entornoResolver.resolver(request);
        return new TokenRespuesta(autenticacion.ingresar(datos.username(), datos.password(), entorno),
            "Bearer", 3600);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader("Authorization") String cabecera,
                                       @AuthenticationPrincipal Usuario usuario, HttpServletRequest request) {
        jwt.revocar(cabecera.substring(7));
        registrarSesion(usuario, "LOGOUT", entornoResolver.resolver(request));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public UsuarioRespuesta me(@AuthenticationPrincipal Usuario usuario, HttpServletRequest request) {
        registrarSesion(usuario, "AUTH_ME", entornoResolver.resolver(request));
        return new UsuarioRespuesta(usuario.getId(), usuario.getUsername(), usuario.getNombre(),
            usuario.getCorreo(), usuario.getRol().getCodigo(), usuario.getDepartamento().getCodigo(),
            usuario.getNivelSeguridad(), usuario.getPais(), usuario.getTipoContrato(), usuario.getEstado(),
            rbac.permisosDe(usuario.getRol().getCodigo()));
    }

    private void registrarSesion(Usuario usuario, String accion, Entorno entorno) {
        auditoria.registrarEvento(new EventoAuditoria(usuario.getUsername(), usuario.getRol().getCodigo(),
            usuario.getDepartamento().getCodigo(), "sesion", accion, "PERMITIDO", "AUTH",
            "Usuario autenticado y activo", List.of(), null, entorno));
    }

    @ExceptionHandler(AutenticacionException.class)
    public ResponseEntity<ProblemDetail> errorAutenticacion(AutenticacionException ex) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(ex.getEstado(), ex.getMessage());
        problema.setTitle(ex.getEstado() == HttpStatus.UNAUTHORIZED ? "No autenticado" : "Acceso denegado");
        problema.setProperty("codigo", ex.getCodigo());
        return ResponseEntity.status(ex.getEstado()).body(problema);
    }

    public record Credenciales(@NotBlank String username, @NotBlank String password) {
    }

    public record TokenRespuesta(String token, String tipo, long expiraEnSegundos) {
    }

    public record UsuarioRespuesta(Long id, String username, String nombre, String correo,
                                   String rol, String departamento, int nivelSeguridad, String pais,
                                   String tipoContrato, String estado, Set<String> permisos) {
    }
}
