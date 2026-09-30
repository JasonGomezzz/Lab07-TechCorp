package com.techcorp.securedocs.comun;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class InstanciaController {
    private final String nombre;
    private final String host;

    public InstanciaController(@Value("${securedocs.instancia:local}") String nombre) {
        this.nombre = nombre;
        this.host = nombreDeHost();
    }

    @GetMapping("/instancia")
    Map<String, String> instancia() {
        return Map.of("instancia", nombre, "host", host);
    }

    private static String nombreDeHost() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            return "desconocido";
        }
    }
}
