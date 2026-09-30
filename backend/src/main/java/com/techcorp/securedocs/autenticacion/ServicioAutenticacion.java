package com.techcorp.securedocs.autenticacion;

import java.util.List;
import com.techcorp.securedocs.auditoria.AuditoriaService;
import com.techcorp.securedocs.auditoria.EventoAuditoria;
import com.techcorp.securedocs.autorizacion.Entorno;
import com.techcorp.securedocs.usuarios.Usuario;
import com.techcorp.securedocs.usuarios.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class ServicioAutenticacion {
    private final UsuarioRepository usuarios;
    private final PasswordEncoder codificador;
    private final ServicioJwt jwt;
    private final AuditoriaService auditoria;

    public ServicioAutenticacion(UsuarioRepository usuarios, PasswordEncoder codificador, ServicioJwt jwt,
                                 AuditoriaService auditoria) {
        this.usuarios = usuarios;
        this.codificador = codificador;
        this.jwt = jwt;
        this.auditoria = auditoria;
    }

    public String ingresar(String username, String password, Entorno entorno) {
        Usuario usuario = usuarios.findByUsername(username).orElse(null);
        if (usuario == null || !codificador.matches(password, usuario.getPasswordHash())) {
            registrar(username, usuario, "DENEGADO", "Credenciales inválidas", List.of(), entorno);
            throw new AutenticacionException(HttpStatus.UNAUTHORIZED, "CREDENCIALES_INVALIDAS",
                "Credenciales inválidas");
        }
        if (!"ACTIVO".equals(usuario.getEstado())) {
            registrar(username, usuario, "DENEGADO", "P7_ESTADO_USUARIO: Usuario no activo",
                List.of(java.util.Map.of("codigo", "P7_ESTADO_USUARIO", "cumple", false,
                    "detalle", "Usuario no activo")), entorno);
            throw new AutenticacionException(HttpStatus.FORBIDDEN, "USUARIO_NO_ACTIVO",
                "El usuario no está activo");
        }
        registrar(username, usuario, "PERMITIDO", "Credenciales válidas y usuario activo", List.of(), entorno);
        return jwt.emitir(usuario.getId());
    }

    private void registrar(String username, Usuario usuario, String resultado, String motivo,
                           List<java.util.Map<String, Object>> politicas, Entorno entorno) {
        auditoria.registrarEvento(new EventoAuditoria(username,
            usuario == null ? null : usuario.getRol().getCodigo(),
            usuario == null ? null : usuario.getDepartamento().getCodigo(),
            "sesion", "LOGIN", resultado, "AUTH", motivo, politicas, null, entorno));
    }
}
