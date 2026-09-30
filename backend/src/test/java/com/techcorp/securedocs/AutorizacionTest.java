package com.techcorp.securedocs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.LocalTime;
import com.techcorp.securedocs.autorizacion.Accion;
import com.techcorp.securedocs.autorizacion.AccesoDenegadoException;
import com.techcorp.securedocs.autorizacion.Autorizador;
import com.techcorp.securedocs.autorizacion.Entorno;
import com.techcorp.securedocs.autorizacion.Recurso;
import com.techcorp.securedocs.autorizacion.Sujeto;
import com.techcorp.securedocs.documentos.DocumentoRepository;
import com.techcorp.securedocs.usuarios.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(properties = "JWT_SECRET=clave_de_prueba_con_mas_de_32_bytes_para_jwt")
@ActiveProfiles("demo")
@Testcontainers(disabledWithoutDocker = true)
class AutorizacionTest {
    @Container
    static final MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.4")
        .withDatabaseName("securedocs")
        .withUsername("securedocs")
        .withPassword("prueba_local")
        .withCommand("--skip-log-bin");

    @DynamicPropertySource
    static void baseDeDatos(DynamicPropertyRegistry propiedades) {
        propiedades.add("spring.datasource.url", mysql::getJdbcUrl);
        propiedades.add("spring.datasource.username", mysql::getUsername);
        propiedades.add("spring.datasource.password", mysql::getPassword);
    }

    @Autowired Autorizador autorizador;
    @Autowired UsuarioRepository usuarios;
    @Autowired DocumentoRepository documentos;
    @Autowired JdbcTemplate jdbc;

    @Test
    void permisoYRechazoAbacSeAuditanConPoliticaExacta() {
        Integer antes = jdbc.queryForObject(
            "SELECT COUNT(*) FROM auditoria WHERE usuario='diego.salas' AND accion='READ'", Integer.class);
        autorizador.exigir(sujeto("diego.salas"), Accion.READ, recurso(502), entorno());
        assertThatThrownBy(() -> autorizador.exigir(sujeto("diego.salas"), Accion.READ,
            recurso(505), entorno()))
            .isInstanceOfSatisfying(AccesoDenegadoException.class, error -> {
                assertThat(error.getDecision().capa()).isEqualTo("ABAC");
                assertThat(error.getDecision().politicas().stream().filter(p -> !p.cumple())
                    .map(p -> p.codigo())).containsExactly("P1_DEPARTAMENTO");
            });
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM auditoria WHERE usuario='diego.salas' AND accion='READ'",
            Integer.class)).isEqualTo(antes + 2);
    }

    @Test
    void rbacDeniegaSinEvaluarAbacYRegistraLaDecision() {
        assertThatThrownBy(() -> autorizador.exigir(sujeto("diego.salas"), Accion.APPROVE,
            recurso(511), entorno()))
            .isInstanceOfSatisfying(AccesoDenegadoException.class, error -> {
                assertThat(error.getDecision().capa()).isEqualTo("RBAC");
                assertThat(error.getDecision().politicas()).isEmpty();
            });
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM auditoria WHERE usuario='diego.salas' AND accion='APPROVE' AND capa='RBAC'",
            Integer.class)).isEqualTo(1);
    }

    @Test
    void auditoriaNoPermiteActualizarNiBorrarRegistros() {
        autorizador.exigir(sujeto("diego.salas"), Accion.READ, recurso(502), entorno());
        Long id = jdbc.queryForObject("SELECT MAX(id) FROM auditoria", Long.class);
        assertThatThrownBy(() -> jdbc.update("UPDATE auditoria SET motivo='alterado' WHERE id=?", id))
            .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> jdbc.update("DELETE FROM auditoria WHERE id=?", id))
            .isInstanceOf(DataAccessException.class);
    }

    private Sujeto sujeto(String username) {
        return Sujeto.de(usuarios.findByUsername(username).orElseThrow());
    }

    private Recurso recurso(long id) {
        return Recurso.de(documentos.findWithPropietarioAndDepartamentoById(id).orElseThrow());
    }

    private Entorno entorno() {
        return new Entorno(LocalTime.of(10, 30), LocalDate.of(2026, 9, 23),
            "192.168.10.20", "PERU", "CORPORATIVO");
    }
}
