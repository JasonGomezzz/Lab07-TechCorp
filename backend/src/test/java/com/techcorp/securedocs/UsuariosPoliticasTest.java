package com.techcorp.securedocs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;
import com.fasterxml.jackson.databind.ObjectMapper;
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
class UsuariosPoliticasTest {
    @Container
    static final MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.4")
        .withDatabaseName("securedocs").withUsername("securedocs")
        .withPassword("prueba_local").withCommand("--skip-log-bin");

    @DynamicPropertySource
    static void baseDeDatos(DynamicPropertyRegistry propiedades) {
        propiedades.add("spring.datasource.url", mysql::getJdbcUrl);
        propiedades.add("spring.datasource.username", mysql::getUsername);
        propiedades.add("spring.datasource.password", mysql::getPassword);
    }

    @Autowired MockMvc api;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;

    @Test
    void usuariosSoloParaAdministradorSinExponerHashes() throws Exception {
        String admin = ingresar("admin");
        String diego = ingresar("diego.salas");
        String respuesta = api.perform(get("/usuarios").header("Authorization", "Bearer " + admin))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(12))
            .andReturn().getResponse().getContentAsString();
        assertThat(respuesta).doesNotContain("passwordHash", "$2a$", "$2b$");
        api.perform(get("/usuarios").header("Authorization", "Bearer " + diego))
            .andExpect(status().isForbidden()).andExpect(jsonPath("$.codigo").value("RBAC_DENEGADO"));
        api.perform(put("/usuarios/1").header("Authorization", "Bearer " + admin)
                .contentType(MediaType.APPLICATION_JSON).content("{\"estado\":\"INACTIVO\"}"))
            .andExpect(status().isConflict());
    }

    @Test
    void desactivarUsuarioInvalidaSuTokenEnLaSiguientePeticion() throws Exception {
        String admin = ingresar("admin");
        String juan = ingresar("juan.temporal");
        try {
            api.perform(put("/usuarios/52").header("Authorization", "Bearer " + admin)
                    .contentType(MediaType.APPLICATION_JSON).content("{\"estado\":\"INACTIVO\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("INACTIVO"));
            api.perform(get("/documentos").header("Authorization", "Bearer " + juan))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("USUARIO_NO_ACTIVO"));
        } finally {
            jdbc.update("UPDATE usuario SET estado='ACTIVO' WHERE id=52");
        }
    }

    @Test
    void crearYAsignarRolExigePermisosYNoDevuelvePassword() throws Exception {
        String admin = ingresar("admin");
        String cuerpo = json.writeValueAsString(Map.ofEntries(
            Map.entry("username", "prueba.usuario"), Map.entry("nombre", "Usuario de prueba"),
            Map.entry("correo", "prueba.usuario@techcorp.example"), Map.entry("password", "Clave#Prueba2026"),
            Map.entry("rol", "EMPLEADO"), Map.entry("departamento", "FINANZAS"),
            Map.entry("nivelSeguridad", 2), Map.entry("pais", "PERU"),
            Map.entry("tipoContrato", "INTERNO"), Map.entry("estado", "ACTIVO")));
        Long id = null;
        try {
            String respuesta = api.perform(post("/usuarios").header("Authorization", "Bearer " + admin)
                    .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rol").value("EMPLEADO"))
                .andReturn().getResponse().getContentAsString();
            assertThat(respuesta).doesNotContain("password", "hash");
            id = json.readTree(respuesta).get("id").asLong();
            api.perform(put("/usuarios/{id}", id).header("Authorization", "Bearer " + admin)
                    .contentType(MediaType.APPLICATION_JSON).content("{\"rol\":\"SUPERVISOR\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.rol").value("SUPERVISOR"));
        } finally {
            if (id != null) jdbc.update("DELETE FROM usuario WHERE id=?", id);
        }
    }

    @Test
    void cambiarPoliticaInvalidaCacheYNoPermiteCambiarP7() throws Exception {
        String admin = ingresar("admin");
        String laura = ingresar("laura.mendez");
        try {
            api.perform(put("/politicas/P4_HORARIO").header("Authorization", "Bearer " + admin)
                    .contentType(MediaType.APPLICATION_JSON).content("{\"activa\":false}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.activa").value(false));
            api.perform(get("/documentos/503").header("Authorization", "Bearer " + laura)
                    .header("X-Sim-Hora", "19:00").header("X-Sim-Dispositivo", "CORPORATIVO"))
                .andExpect(status().isOk());
        } finally {
            api.perform(put("/politicas/P4_HORARIO").header("Authorization", "Bearer " + admin)
                    .contentType(MediaType.APPLICATION_JSON).content("{\"activa\":true}"))
                .andExpect(status().isOk());
        }
        api.perform(get("/documentos/503").header("Authorization", "Bearer " + laura)
                .header("X-Sim-Hora", "19:00").header("X-Sim-Dispositivo", "CORPORATIVO"))
            .andExpect(status().isForbidden()).andExpect(jsonPath("$.politicas[0]").value("P4_HORARIO"));
        api.perform(put("/politicas/P7_ESTADO_USUARIO").header("Authorization", "Bearer " + admin)
                .contentType(MediaType.APPLICATION_JSON).content("{\"activa\":false}"))
            .andExpect(status().isConflict());
    }

    private String ingresar(String username) throws Exception {
        String body = json.writeValueAsString(Map.of("username", username, "password", "Secure#2026"));
        String respuesta = api.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return json.readTree(respuesta).get("token").asText();
    }
}
