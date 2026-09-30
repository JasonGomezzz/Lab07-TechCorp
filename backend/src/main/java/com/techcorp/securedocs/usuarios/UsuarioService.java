package com.techcorp.securedocs.usuarios;

import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import com.techcorp.securedocs.autorizacion.Accion;
import com.techcorp.securedocs.autorizacion.Autorizador;
import com.techcorp.securedocs.autorizacion.Entorno;
import com.techcorp.securedocs.autorizacion.Recurso;
import com.techcorp.securedocs.autorizacion.Sujeto;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UsuarioService {
    private static final Set<String> ESTADOS = Set.of("ACTIVO", "INACTIVO", "SUSPENDIDO");
    private static final Set<String> PAISES = Set.of("PERU", "MEXICO", "COLOMBIA");
    private static final Set<String> CONTRATOS = Set.of("INTERNO", "EXTERNO");

    private final UsuarioRepository usuarios;
    private final RolRepository roles;
    private final DepartamentoRepository departamentos;
    private final PasswordEncoder codificador;
    private final Autorizador autorizador;

    public UsuarioService(UsuarioRepository usuarios, RolRepository roles, DepartamentoRepository departamentos,
                          PasswordEncoder codificador, Autorizador autorizador) {
        this.usuarios = usuarios;
        this.roles = roles;
        this.departamentos = departamentos;
        this.codificador = codificador;
        this.autorizador = autorizador;
    }

    @Transactional(readOnly = true)
    public List<UsuarioRespuesta> listar(Usuario actor, Entorno entorno) {
        Sujeto sujeto = Sujeto.de(actor);
        autorizador.exigir(sujeto, Accion.USER_LIST,
            Recurso.noDocumental("usuarios", sujeto.departamento()), entorno);
        return usuarios.findAll().stream().map(UsuarioRespuesta::de).toList();
    }

    @Transactional
    public UsuarioRespuesta crear(UsuarioCrear datos, Usuario actor, Entorno entorno) {
        Sujeto sujeto = Sujeto.de(actor);
        autorizador.exigir(sujeto, Accion.USER_CREATE,
            Recurso.noDocumental("usuario-nuevo", sujeto.departamento()), entorno);
        autorizador.exigir(sujeto, Accion.USER_ROLE_ASSIGN,
            Recurso.noDocumental("usuario-nuevo", sujeto.departamento()), entorno);
        validar(datos.nivelSeguridad(), datos.pais(), datos.tipoContrato(), datos.estado(), datos.rol(),
            datos.accesoHasta());
        Usuario usuario = new Usuario();
        usuario.setUsername(obligatorio(datos.username(), "username"));
        usuario.setNombre(obligatorio(datos.nombre(), "nombre"));
        usuario.setCorreo(obligatorio(datos.correo(), "correo"));
        usuario.setPasswordHash(codificador.encode(obligatorio(datos.password(), "password")));
        usuario.setRol(roles.findByCodigo(datos.rol()).orElseThrow(() -> new IllegalArgumentException("Rol inválido")));
        usuario.setDepartamento(departamentos.findByCodigo(datos.departamento())
            .orElseThrow(() -> new IllegalArgumentException("Departamento inválido")));
        usuario.setNivelSeguridad(datos.nivelSeguridad().byteValue());
        usuario.setPais(datos.pais());
        usuario.setTipoContrato(datos.tipoContrato());
        usuario.setEstado(datos.estado());
        usuario.setAccesoHasta(datos.accesoHasta());
        return UsuarioRespuesta.de(usuarios.saveAndFlush(usuario));
    }

    @Transactional
    public UsuarioRespuesta modificar(long id, UsuarioActualizar datos, Usuario actor, Entorno entorno) {
        Sujeto sujeto = Sujeto.de(actor);
        autorizador.exigir(sujeto, Accion.USER_UPDATE,
            Recurso.noDocumental("usuario-" + id, sujeto.departamento()), entorno);
        Usuario objetivo = usuarios.findWithRolAndDepartamentoById(id)
            .orElseThrow(() -> new NoSuchElementException("Usuario no encontrado"));
        String rolFinal = datos.rol() == null ? objetivo.getRol().getCodigo() : datos.rol();
        String estadoFinal = datos.estado() == null ? objetivo.getEstado() : datos.estado();
        autorizador.validarCambioPropio(sujeto, id, rolFinal, estadoFinal);
        if (!rolFinal.equals(objetivo.getRol().getCodigo())) {
            autorizador.exigir(sujeto, Accion.USER_ROLE_ASSIGN,
                Recurso.noDocumental("usuario-" + id, sujeto.departamento()), entorno);
            objetivo.setRol(roles.findByCodigo(rolFinal)
                .orElseThrow(() -> new IllegalArgumentException("Rol inválido")));
        }
        int nivelFinal = datos.nivelSeguridad() == null ? objetivo.getNivelSeguridad() : datos.nivelSeguridad();
        String paisFinal = datos.pais() == null ? objetivo.getPais() : datos.pais();
        String contratoFinal = datos.tipoContrato() == null ? objetivo.getTipoContrato() : datos.tipoContrato();
        LocalDate accesoFinal = datos.accesoHasta() == null ? objetivo.getAccesoHasta() : datos.accesoHasta();
        validar(nivelFinal, paisFinal, contratoFinal, estadoFinal, rolFinal, accesoFinal);
        if (datos.nombre() != null) objetivo.setNombre(obligatorio(datos.nombre(), "nombre"));
        if (datos.correo() != null) objetivo.setCorreo(obligatorio(datos.correo(), "correo"));
        if (datos.password() != null) objetivo.setPasswordHash(codificador.encode(
            obligatorio(datos.password(), "password")));
        if (datos.departamento() != null) objetivo.setDepartamento(departamentos.findByCodigo(datos.departamento())
            .orElseThrow(() -> new IllegalArgumentException("Departamento inválido")));
        objetivo.setNivelSeguridad((byte) nivelFinal);
        objetivo.setPais(paisFinal);
        objetivo.setTipoContrato(contratoFinal);
        objetivo.setEstado(estadoFinal);
        objetivo.setAccesoHasta(accesoFinal);
        return UsuarioRespuesta.de(usuarios.saveAndFlush(objetivo));
    }

    private void validar(Integer nivel, String pais, String contrato, String estado,
                         String rol, LocalDate accesoHasta) {
        if (nivel == null || nivel < 0 || nivel > 5 || !PAISES.contains(pais)
            || !CONTRATOS.contains(contrato) || !ESTADOS.contains(estado)) {
            throw new IllegalArgumentException("Atributos de usuario inválidos");
        }
        if ("INVITADO".equals(rol) && accesoHasta == null) {
            throw new IllegalArgumentException("Un invitado requiere accesoHasta");
        }
    }

    private String obligatorio(String valor, String campo) {
        if (valor == null || valor.isBlank()) throw new IllegalArgumentException("Campo obligatorio: " + campo);
        return valor.trim();
    }

    public record UsuarioCrear(String username, String nombre, String correo, String password,
                               String rol, String departamento, Integer nivelSeguridad,
                               String pais, String tipoContrato, String estado, LocalDate accesoHasta) {
    }

    public record UsuarioActualizar(String nombre, String correo, String password, String rol,
                                    String departamento, Integer nivelSeguridad, String pais,
                                    String tipoContrato, String estado, LocalDate accesoHasta) {
    }

    public record UsuarioRespuesta(Long id, String username, String nombre, String correo,
                                   String rol, String departamento, int nivelSeguridad,
                                   String pais, String tipoContrato, String estado, LocalDate accesoHasta) {
        static UsuarioRespuesta de(Usuario u) {
            return new UsuarioRespuesta(u.getId(), u.getUsername(), u.getNombre(), u.getCorreo(),
                u.getRol().getCodigo(), u.getDepartamento().getCodigo(), u.getNivelSeguridad(),
                u.getPais(), u.getTipoContrato(), u.getEstado(), u.getAccesoHasta());
        }
    }
}
