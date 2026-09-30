package com.techcorp.securedocs.politicas;

import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.Collectors;
import com.techcorp.securedocs.autorizacion.Accion;
import com.techcorp.securedocs.autorizacion.Autorizador;
import com.techcorp.securedocs.autorizacion.Entorno;
import com.techcorp.securedocs.autorizacion.Recurso;
import com.techcorp.securedocs.autorizacion.Sujeto;
import com.techcorp.securedocs.comun.ConflictoEstadoException;
import com.techcorp.securedocs.usuarios.Usuario;
import org.springframework.stereotype.Service;

@Service
public class PoliticaService {
    private static final Set<String> ROLES = Set.of("ADMINISTRADOR", "GERENTE", "SUPERVISOR",
        "EMPLEADO", "AUDITOR", "INVITADO");
    private static final Set<String> ACCIONES = Arrays.stream(Accion.values())
        .map(Enum::name).collect(Collectors.toUnmodifiableSet());

    private final PoliticaRepository repositorio;
    private final ConfiguracionPoliticas configuracion;
    private final Autorizador autorizador;

    public PoliticaService(PoliticaRepository repositorio, ConfiguracionPoliticas configuracion,
                           Autorizador autorizador) {
        this.repositorio = repositorio;
        this.configuracion = configuracion;
        this.autorizador = autorizador;
    }

    public List<PoliticaRespuesta> listar(Usuario actor, Entorno entorno) {
        Sujeto sujeto = Sujeto.de(actor);
        autorizador.exigir(sujeto, Accion.POLICY_LIST,
            Recurso.noDocumental("politicas", sujeto.departamento()), entorno);
        return repositorio.findAll().stream().map(PoliticaRespuesta::de).toList();
    }

    public PoliticaRespuesta actualizar(String codigo, PoliticaActualizar datos, Usuario actor, Entorno entorno) {
        Sujeto sujeto = Sujeto.de(actor);
        autorizador.exigir(sujeto, Accion.POLICY_UPDATE,
            Recurso.noDocumental("politica-" + codigo, sujeto.departamento()), entorno);
        Politica politica = repositorio.findById(codigo)
            .orElseThrow(() -> new NoSuchElementException("Política no encontrada"));
        if ("P7_ESTADO_USUARIO".equals(codigo)) {
            throw new ConflictoEstadoException("La política de estado de usuario no se puede desactivar ni modificar");
        }
        if (datos.activa() != null) politica.setActiva(datos.activa());
        if (datos.acciones() != null) {
            if (datos.acciones().isEmpty() || datos.acciones().stream()
                .anyMatch(a -> !"*".equals(a) && !ACCIONES.contains(a))) {
                throw new IllegalArgumentException("Acciones de política inválidas");
            }
            politica.setAcciones(List.copyOf(datos.acciones()));
        }
        if (datos.rolesExentos() != null) {
            if (datos.rolesExentos().stream().anyMatch(r -> !ROLES.contains(r))) {
                throw new IllegalArgumentException("Roles exentos inválidos");
            }
            politica.setRolesExentos(List.copyOf(datos.rolesExentos()));
        }
        if (datos.parametros() != null) {
            validarParametros(codigo, datos.parametros());
            politica.setParametros(Map.copyOf(datos.parametros()));
        }
        PoliticaRespuesta respuesta = PoliticaRespuesta.de(repositorio.saveAndFlush(politica));
        configuracion.invalidarCache();
        return respuesta;
    }

    private void validarParametros(String codigo, Map<String, Object> p) {
        try {
            switch (codigo) {
                case "P4_HORARIO" -> {
                    nivel(p, "nivelMinimo");
                    LocalTime inicio = LocalTime.parse((String) p.get("inicio"));
                    LocalTime fin = LocalTime.parse((String) p.get("fin"));
                    if (!inicio.isBefore(fin)) throw new IllegalArgumentException();
                }
                case "P5B_UBICACION" -> {
                    if (!Set.of("PERU", "MEXICO", "COLOMBIA").contains(p.get("pais"))) {
                        throw new IllegalArgumentException();
                    }
                }
                case "P6_DISPOSITIVO" -> nivel(p, "nivelMinimo");
                case "P8_INVITADO" -> nivel(p, "nivelMaximo");
                default -> {
                    if (!p.isEmpty()) throw new IllegalArgumentException();
                }
            }
        } catch (RuntimeException ex) {
            throw new IllegalArgumentException("Parámetros de política inválidos");
        }
    }

    private void nivel(Map<String, Object> p, String clave) {
        if (!(p.get(clave) instanceof Integer n) || n < 0 || n > 5) {
            throw new IllegalArgumentException();
        }
    }

    public record PoliticaActualizar(Boolean activa, List<String> acciones,
                                     List<String> rolesExentos, Map<String, Object> parametros) {
    }

    public record PoliticaRespuesta(String codigo, String nombre, String descripcion,
                                   List<String> acciones, List<String> rolesExentos,
                                   Map<String, Object> parametros, boolean activa) {
        static PoliticaRespuesta de(Politica p) {
            return new PoliticaRespuesta(p.getCodigo(), p.getNombre(), p.getDescripcion(),
                p.getAcciones(), p.getRolesExentos(), p.getParametros(), p.isActiva());
        }
    }
}
