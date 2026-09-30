package com.techcorp.securedocs;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import com.techcorp.securedocs.autorizacion.Accion;
import com.techcorp.securedocs.autorizacion.ContextoAutorizacion;
import com.techcorp.securedocs.autorizacion.Entorno;
import com.techcorp.securedocs.autorizacion.Recurso;
import com.techcorp.securedocs.autorizacion.Sujeto;
import com.techcorp.securedocs.autorizacion.abac.ConfigPolitica;
import com.techcorp.securedocs.autorizacion.abac.MotorAbac;
import com.techcorp.securedocs.autorizacion.abac.reglas.ReglaDepartamento;
import com.techcorp.securedocs.autorizacion.abac.reglas.ReglaDispositivo;
import com.techcorp.securedocs.autorizacion.abac.reglas.ReglaEstadoUsuario;
import com.techcorp.securedocs.autorizacion.abac.reglas.ReglaHorario;
import com.techcorp.securedocs.autorizacion.abac.reglas.ReglaInvitado;
import com.techcorp.securedocs.autorizacion.abac.reglas.ReglaNivelSeguridad;
import com.techcorp.securedocs.autorizacion.abac.reglas.ReglaPaisUsuario;
import com.techcorp.securedocs.autorizacion.abac.reglas.ReglaPropiedad;
import com.techcorp.securedocs.autorizacion.abac.reglas.ReglaUbicacion;
import com.techcorp.securedocs.autorizacion.abac.reglas.ReglaVigenciaInvitado;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class MotorAbacTest {
    private final MotorAbac motor = new MotorAbac(List.of(new ReglaDepartamento(), new ReglaNivelSeguridad(),
        new ReglaPropiedad(), new ReglaHorario(), new ReglaPaisUsuario(), new ReglaUbicacion(),
        new ReglaDispositivo(), new ReglaEstadoUsuario(), new ReglaInvitado(), new ReglaVigenciaInvitado()));

    private final List<ConfigPolitica> configuraciones = List.of(
        config("P1_DEPARTAMENTO", List.of(), Map.of(), "CREATE", "READ", "UPDATE", "DELETE", "APPROVE"),
        config("P2_NIVEL_SEGURIDAD", List.of(), Map.of(), "CREATE", "READ", "UPDATE", "DELETE", "APPROVE"),
        config("P3_PROPIEDAD", List.of("GERENTE", "ADMINISTRADOR"), Map.of(), "UPDATE"),
        config("P4_HORARIO", List.of(), Map.of("nivelMinimo", 4, "inicio", "08:00", "fin", "18:00"),
            "READ", "UPDATE", "DELETE", "APPROVE"),
        config("P5A_PAIS_USUARIO", List.of(), Map.of(), "CREATE", "READ", "UPDATE", "DELETE", "APPROVE"),
        config("P5B_UBICACION", List.of(), Map.of("pais", "PERU"), "READ", "UPDATE", "DELETE", "APPROVE"),
        config("P6_DISPOSITIVO", List.of(), Map.of("nivelMinimo", 4), "READ", "UPDATE", "DELETE", "APPROVE"),
        config("P7_ESTADO_USUARIO", List.of(), Map.of(), "*"),
        config("P8_INVITADO", List.of(), Map.of("nivelMaximo", 1), "READ"),
        config("P9_VIGENCIA_INVITADO", List.of(), Map.of(), "READ"));

    @Test
    void casosDePoliticasAislanLosMotivosDeRechazo() {
        assertFallos(diego(), Accion.READ, doc(502, "FINANZAS", 1, "PUBLICADO", "PERU", 25), normal());
        assertFallos(diego(), Accion.READ, doc(505, "RRHH", 2, "PUBLICADO", "PERU", 31), normal(),
            "P1_DEPARTAMENTO");
        assertFallos(diego(), Accion.READ, doc(503, "FINANZAS", 4, "PUBLICADO", "PERU", 10), normal(),
            "P2_NIVEL_SEGURIDAD");
        assertFallos(laura(), Accion.READ, doc(503, "FINANZAS", 4, "PUBLICADO", "PERU", 10),
            entorno("19:00", "PERU", "CORPORATIVO"), "P4_HORARIO");
        assertFallos(laura(), Accion.READ, doc(504, "FINANZAS", 5, "PUBLICADO", "PERU", 10),
            entorno("10:30", "PERU", "PERSONAL"), "P6_DISPOSITIVO");
        assertFallos(invitado(60, LocalDate.of(2099, 12, 31)), Accion.READ,
            doc(502, "FINANZAS", 1, "PUBLICADO", "PERU", 25), normal());
        assertFallos(invitado(60, LocalDate.of(2099, 12, 31)), Accion.READ,
            doc(503, "FINANZAS", 4, "PUBLICADO", "PERU", 10), normal(),
            "P2_NIVEL_SEGURIDAD", "P8_INVITADO");
        assertFallos(diego(), Accion.UPDATE, doc(502, "FINANZAS", 1, "PUBLICADO", "PERU", 25), normal(),
            "P3_PROPIEDAD");
        assertFallos(laura(), Accion.UPDATE, doc(502, "FINANZAS", 1, "PUBLICADO", "PERU", 25), normal());
        assertFallos(diego(), Accion.READ, doc(502, "FINANZAS", 1, "PUBLICADO", "PERU", 25),
            entorno("10:30", "MEXICO", "CORPORATIVO"), "P5B_UBICACION");
        assertFallos(invitado(60, LocalDate.of(2099, 12, 31)), Accion.READ,
            doc(507, "FINANZAS", 1, "BORRADOR", "PERU", 30), normal(), "P8_INVITADO");
        assertFallos(invitado(61, LocalDate.of(2020, 1, 1)), Accion.READ,
            doc(502, "FINANZAS", 1, "PUBLICADO", "PERU", 25), normal(), "P9_VIGENCIA_INVITADO");
        Sujeto marta = new Sujeto(31, "marta.quispe", "EMPLEADO", "RRHH", 2,
            "PERU", "INTERNO", "ACTIVO", null);
        assertFallos(marta, Accion.UPDATE, doc(503, "FINANZAS", 4, "PUBLICADO", "PERU", 10),
            normal(), "P1_DEPARTAMENTO", "P2_NIVEL_SEGURIDAD", "P3_PROPIEDAD");
    }

    @ParameterizedTest
    @CsvSource({"07:59,false", "08:00,true", "17:59,true", "18:00,false"})
    void horarioIncluyeInicioYExcluyeFin(String hora, boolean permitido) {
        Set<String> fallos = fallos(laura(), Accion.READ,
            doc(503, "FINANZAS", 4, "PUBLICADO", "PERU", 10),
            entorno(hora, "PERU", "CORPORATIVO"));
        assertThat(fallos.contains("P4_HORARIO")).isEqualTo(!permitido);
    }

    private void assertFallos(Sujeto sujeto, Accion accion, Recurso recurso, Entorno entorno,
                              String... esperados) {
        assertThat(fallos(sujeto, accion, recurso, entorno)).containsExactlyInAnyOrder(esperados);
    }

    private Set<String> fallos(Sujeto sujeto, Accion accion, Recurso recurso, Entorno entorno) {
        return motor.evaluar(new ContextoAutorizacion(sujeto, accion, recurso, entorno), configuraciones)
            .stream().filter(p -> !p.cumple()).map(p -> p.codigo()).collect(Collectors.toSet());
    }

    private static ConfigPolitica config(String codigo, List<String> exentos, Map<String, Object> parametros,
                                         String... acciones) {
        return new ConfigPolitica(codigo, List.of(acciones), exentos, parametros);
    }

    private static Sujeto diego() {
        return new Sujeto(30, "diego.salas", "EMPLEADO", "FINANZAS", 2,
            "PERU", "INTERNO", "ACTIVO", null);
    }

    private static Sujeto laura() {
        return new Sujeto(10, "laura.mendez", "GERENTE", "FINANZAS", 5,
            "PERU", "INTERNO", "ACTIVO", null);
    }

    private static Sujeto invitado(long id, LocalDate accesoHasta) {
        return new Sujeto(id, "invitado", "INVITADO", "FINANZAS", 1,
            "PERU", "EXTERNO", "ACTIVO", accesoHasta);
    }

    private static Recurso doc(long id, String departamento, int nivel, String estado, String pais, long propietario) {
        return new Recurso("documento-" + id, departamento, nivel, propietario, estado, pais);
    }

    private static Entorno normal() {
        return entorno("10:30", "PERU", "CORPORATIVO");
    }

    private static Entorno entorno(String hora, String ubicacion, String dispositivo) {
        return new Entorno(LocalTime.parse(hora), LocalDate.of(2026, 9, 23),
            "192.168.10.20", ubicacion, dispositivo);
    }
}
