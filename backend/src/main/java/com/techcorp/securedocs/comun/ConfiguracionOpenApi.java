package com.techcorp.securedocs.comun;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@OpenAPIDefinition(info = @Info(title = "SecureDocs API", version = "1.0",
    description = "Laboratorio de autenticación, RBAC, ABAC y auditoría de TechCorp. "
        + "Las cabeceras X-Sim-* solo afectan al perfil demo."),
    security = @SecurityRequirement(name = "bearerAuth"))
@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
public class ConfiguracionOpenApi {
    @Bean
    @Profile("demo")
    OpenApiCustomizer cabecerasDeSimulacion() {
        return api -> api.getPaths().values().forEach(ruta -> ruta.readOperations().forEach(operacion -> {
            for (String nombre : new String[] {"X-Sim-Hora", "X-Sim-Fecha", "X-Sim-Ip",
                    "X-Sim-Ubicacion", "X-Sim-Dispositivo"}) {
                operacion.addParametersItem(new Parameter().name(nombre).in("header").required(false)
                    .description("Solo perfil demo; simula el atributo de entorno " + nombre)
                    .schema(new StringSchema()));
            }
        }));
    }
}
