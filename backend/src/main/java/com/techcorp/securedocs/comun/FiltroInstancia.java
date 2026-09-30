package com.techcorp.securedocs.comun;

import java.io.IOException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class FiltroInstancia extends OncePerRequestFilter {
    private final String nombre;

    public FiltroInstancia(@Value("${securedocs.instancia:local}") String nombre) {
        this.nombre = nombre;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain cadena) throws ServletException, IOException {
        response.setHeader("X-Served-By", nombre);
        cadena.doFilter(request, response);
    }
}
