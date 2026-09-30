package com.techcorp.securedocs;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.techcorp.securedocs.autorizacion.rbac.ServicioRbac;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(properties = "JWT_SECRET=clave_de_prueba_con_mas_de_32_bytes_para_jwt")
@AutoConfigureMockMvc
@ActiveProfiles("demo")
@Testcontainers(disabledWithoutDocker = true)
class AutenticacionTest {
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

    @Autowired MockMvc api;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;
    @Autowired ServicioRbac rbac;

    @Test
    void matrizRbacCoincideConLasOchoOperacionesDeLaGuia() {
        Map<String, Set<String>> matriz = Map.of(
            "ADMINISTRADOR", Set.of("CREAR_DOCUMENTO", "CONSULTAR_DOCUMENTO", "MODIFICAR_DOCUMENTO",
                "ELIMINAR_DOCUMENTO", "APROBAR_DOCUMENTO", "VER_AUDITORIA", "GESTIONAR_USUARIOS", "ASIGNAR_ROLES"),
            "GERENTE", Set.of("CREAR_DOCUMENTO", "CONSULTAR_DOCUMENTO", "MODIFICAR_DOCUMENTO",
                "ELIMINAR_DOCUMENTO", "APROBAR_DOCUMENTO", "VER_AUDITORIA"),
            "SUPERVISOR", Set.of("CREAR_DOCUMENTO", "CONSULTAR_DOCUMENTO", "MODIFICAR_DOCUMENTO", "APROBAR_DOCUMENTO"),
            "EMPLEADO", Set.of("CREAR_DOCUMENTO", "CONSULTAR_DOCUMENTO", "MODIFICAR_DOCUMENTO"),
            "AUDITOR", Set.of("CONSULTAR_DOCUMENTO", "VER_AUDITORIA"),
            "INVITADO", Set.of("CONSULTAR_DOCUMENTO"));
        Set<String> operaciones = matriz.get("ADMINISTRADOR");
        for (var entrada : matriz.entrySet()) {
            for (String operacion : operaciones) {
                org.assertj.core.api.Assertions.assertThat(rbac.permite(entrada.getKey(), operacion))
                    .as(entrada.getKey() + " / " + operacion)
                    .isEqualTo(entrada.getValue().contains(operacion));
            }
        }
    }

    @Test
    void loginMeYLogoutRevocaElToken() throws Exception {
        String token = ingresar("admin", "Secure#2026");
        api.perform(get("/auth/me").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.rol").value("ADMINISTRADOR"))
            .andExpect(jsonPath("$.permisos.length()").value(9));
        api.perform(post("/auth/logout").header("Authorization", "Bearer " + token))
            .andExpect(status().isNoContent());
        api.perform(get("/auth/me").header("Authorization", "Bearer " + token))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.codigo").value("TOKEN_REVOCADO"));
    }

    @Test
    void credencialesIncorrectasYUsuariosNoActivosSeDistinguenSinFiltrarExistencia() throws Exception {
        api.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"admin\",\"password\":\"incorrecta\"}"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.codigo").value("CREDENCIALES_INVALIDAS"));
        api.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"desconocido\",\"password\":\"incorrecta\"}"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.codigo").value("CREDENCIALES_INVALIDAS"));
        api.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"pedro.suspendido\",\"password\":\"Secure#2026\"}"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.codigo").value("USUARIO_NO_ACTIVO"));
    }

    @Test
    void tokenDeUsuarioDesactivadoDejaDeFuncionar() throws Exception {
        String token = ingresar("juan.temporal", "Secure#2026");
        try {
            jdbc.update("UPDATE usuario SET estado='INACTIVO' WHERE id=52");
            api.perform(get("/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("USUARIO_NO_ACTIVO"));
        } finally {
            jdbc.update("UPDATE usuario SET estado='ACTIVO' WHERE id=52");
        }
    }

    @Test
    void auditoriaRespetaPermisosYPermiteExportarCsv() throws Exception {
        String admin = ingresar("admin", "Secure#2026");
        api.perform(get("/auditoria").header("Authorization", "Bearer " + admin)
                .param("accion", "LOGIN"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].accion").value("LOGIN"));
        api.perform(get("/auditoria").header("Authorization", "Bearer " + admin)
                .param("formato", "csv"))
            .andExpect(status().isOk())
            .andExpect(result -> org.assertj.core.api.Assertions.assertThat(
                result.getResponse().getContentAsString()).startsWith("id,fecha_hora,"));

        String empleado = ingresar("diego.salas", "Secure#2026");
        api.perform(get("/auditoria").header("Authorization", "Bearer " + empleado))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.codigo").value("RBAC_DENEGADO"));

        String gerente = ingresar("laura.mendez", "Secure#2026");
        String respuesta = api.perform(get("/auditoria").header("Authorization", "Bearer " + gerente))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        for (JsonNode registro : json.readTree(respuesta).get("content")) {
            org.assertj.core.api.Assertions.assertThat(registro.get("departamentoRecurso").asText())
                .isEqualTo("FINANZAS");
        }
    }

    private String ingresar(String usuario, String password) throws Exception {
        String body = json.writeValueAsString(new AuthDatos(usuario, password));
        String respuesta = api.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        JsonNode datos = json.readTree(respuesta);
        return datos.get("token").asText();
    }

    private record AuthDatos(String username, String password) {
    }
}
