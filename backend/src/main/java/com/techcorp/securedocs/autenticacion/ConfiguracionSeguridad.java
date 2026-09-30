package com.techcorp.securedocs.autenticacion;

import java.io.IOException;
import java.util.Map;
import java.util.List;
import com.techcorp.securedocs.auditoria.AuditoriaService;
import com.techcorp.securedocs.auditoria.EventoAuditoria;
import com.techcorp.securedocs.entorno.EntornoResolver;
import jakarta.servlet.http.HttpServletRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.techcorp.securedocs.usuarios.UsuarioRepository;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class ConfiguracionSeguridad {
    @Bean
    PasswordEncoder codificadorPassword() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    SecurityFilterChain seguridad(HttpSecurity http, ServicioJwt jwt, UsuarioRepository usuarios,
                                 ObjectMapper json, AuditoriaService auditoria,
                                 EntornoResolver entornoResolver) throws Exception {
        http.csrf(csrf -> csrf.disable())
            .formLogin(form -> form.disable())
            .httpBasic(basic -> basic.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(reglas -> reglas
                .requestMatchers(HttpMethod.POST, "/auth/login").permitAll()
                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                .anyRequest().authenticated())
            .exceptionHandling(errores -> errores.authenticationEntryPoint((request, response, ex) ->
                errorSinToken(request, response, json, auditoria, entornoResolver)))
            .addFilterBefore(new FiltroJwt(jwt, usuarios, json, auditoria, entornoResolver),
                UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    private static void errorSinToken(HttpServletRequest request, HttpServletResponse response,
                                      ObjectMapper json, AuditoriaService auditoria,
                                      EntornoResolver entornoResolver) throws IOException {
        auditoria.registrarEvento(new EventoAuditoria("anonimo", null, null, request.getRequestURI(),
            "AUTH_DENIED", "DENEGADO", "AUTH", "TOKEN_AUSENTE", List.of(), null,
            entornoResolver.resolver(request)));
        response.setStatus(401);
        response.setContentType("application/problem+json");
        json.writeValue(response.getWriter(), Map.of(
            "type", "about:blank", "title", "No autenticado", "status", 401,
            "codigo", "TOKEN_AUSENTE"));
    }
}
