package com.techcorp.securedocs.comun;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class ConfiguracionWeb implements WebMvcConfigurer {
    public static final String PREFIJO_API = "/api";

    @Override
    public void configurePathMatch(PathMatchConfigurer configurador) {
        configurador.addPathPrefix(PREFIJO_API, clase ->
            clase.isAnnotationPresent(RestController.class)
                && clase.getPackageName().startsWith("com.techcorp.securedocs"));
    }
}
