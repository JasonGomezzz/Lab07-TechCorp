package com.techcorp.securedocs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.HashSet;
import java.util.Set;
import com.fasterxml.jackson.databind.JsonNode;
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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(properties = "JWT_SECRET=clave_de_prueba_con_mas_de_32_bytes_para_jwt")
@AutoConfigureMockMvc
@ActiveProfiles("demo")
@Testcontainers(disabledWithoutDocker = true)
class DocumentosTest {
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
    void lecturasYListadoAplicanPoliticasSinFiltrarDocumentosAjenos() throws Exception {
        String diego = ingresar("diego.salas");
        String laura = ingresar("laura.mendez");
        String invitado = ingresar("invitado.externo");

        api.perform(normal(get("/documentos/502"), diego)).andExpect(status().isOk());
        api.perform(normal(get("/documentos/505"), diego)).andExpect(status().isForbidden())
            .andExpect(jsonPath("$.politicas[0]").value("P1_DEPARTAMENTO"));
        api.perform(normal(get("/documentos/503"), diego)).andExpect(status().isForbidden())
            .andExpect(jsonPath("$.politicas[0]").value("P2_NIVEL_SEGURIDAD"));
        api.perform(conEntorno(get("/documentos/503"), laura, "19:00", "PERU", "CORPORATIVO"))
            .andExpect(status().isForbidden()).andExpect(jsonPath("$.politicas[0]").value("P4_HORARIO"));
        api.perform(conEntorno(get("/documentos/504"), laura, "10:30", "PERU", "PERSONAL"))
            .andExpect(status().isForbidden()).andExpect(jsonPath("$.politicas[0]").value("P6_DISPOSITIVO"));
        api.perform(normal(get("/documentos/502"), invitado)).andExpect(status().isOk());
        api.perform(normal(get("/documentos/503"), invitado)).andExpect(status().isForbidden())
            .andExpect(jsonPath("$.politicas.length()").value(2));
        api.perform(conEntorno(get("/documentos/502"), diego, "10:30", "MEXICO", "CORPORATIVO"))
            .andExpect(status().isForbidden()).andExpect(jsonPath("$.politicas[0]").value("P5B_UBICACION"));

        String respuesta = api.perform(normal(get("/documentos"), diego))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        Set<Long> ids = new HashSet<>();
        for (JsonNode d : json.readTree(respuesta)) ids.add(d.get("id").asLong());
        assertThat(ids).containsExactlyInAnyOrder(502L, 507L, 508L, 509L);
    }

    @Test
    void aprobacionBorradoYPropiedadRespetanRbacYAbac() throws Exception {
        String carlos = ingresar("carlos.ruiz");
        String diego = ingresar("diego.salas");
        String laura = ingresar("laura.mendez");
        String sofia = ingresar("sofia.paredes");
        try {
            api.perform(normal(post("/documentos/511/aprobar"), carlos)).andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("PUBLICADO"));
            api.perform(normal(post("/documentos/511/aprobar"), diego)).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("RBAC_DENEGADO"));
            api.perform(normal(put("/documentos/502"), sofia).contentType(MediaType.APPLICATION_JSON)
                    .content("{\"titulo\":\"Intento\"}"))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.codigo").value("RBAC_DENEGADO"));
            api.perform(normal(put("/documentos/502"), diego).contentType(MediaType.APPLICATION_JSON)
                    .content("{\"titulo\":\"Intento\"}"))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.politicas[0]").value("P3_PROPIEDAD"));
            api.perform(normal(put("/documentos/502"), laura).contentType(MediaType.APPLICATION_JSON)
                    .content("{\"titulo\":\"Informe actualizado\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.titulo").value("Informe actualizado"));
            api.perform(normal(delete("/documentos/509"), laura)).andExpect(status().isNoContent());
            api.perform(normal(get("/documentos/509"), laura)).andExpect(status().isNotFound());
        } finally {
            jdbc.update("UPDATE documento SET estado='PENDIENTE' WHERE id=511");
            jdbc.update("UPDATE documento SET titulo='Informe de gastos Q3' WHERE id=502");
            jdbc.update("UPDATE documento SET estado='PUBLICADO',eliminado_en=NULL WHERE id=509");
        }
    }

    @Test
    void noSePuedenEditarAtributosDeAutorizacionConPut() throws Exception {
        String laura = ingresar("laura.mendez");
        api.perform(normal(put("/documentos/502"), laura).contentType(MediaType.APPLICATION_JSON)
                .content("{\"nivelConfidencialidad\":0}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.codigo").value("DATOS_INVALIDOS"));
    }

    private String ingresar(String username) throws Exception {
        String body = json.writeValueAsString(java.util.Map.of("username", username, "password", "Secure#2026"));
        String respuesta = api.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return json.readTree(respuesta).get("token").asText();
    }

    private MockHttpServletRequestBuilder normal(MockHttpServletRequestBuilder solicitud, String token) {
        return conEntorno(solicitud, token, "10:30", "PERU", "CORPORATIVO");
    }

    private MockHttpServletRequestBuilder conEntorno(MockHttpServletRequestBuilder solicitud, String token,
                                                      String hora, String ubicacion, String dispositivo) {
        return solicitud.header("Authorization", "Bearer " + token)
            .header("X-Sim-Hora", hora)
            .header("X-Sim-Fecha", "2026-09-23")
            .header("X-Sim-Ip", "192.168.10.20")
            .header("X-Sim-Ubicacion", ubicacion)
            .header("X-Sim-Dispositivo", dispositivo);
    }
}
