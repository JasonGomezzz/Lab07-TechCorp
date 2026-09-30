package com.techcorp.securedocs.entorno;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import com.techcorp.securedocs.autorizacion.Entorno;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

@Component
public class EntornoResolver {
    private static final Logger LOG = LoggerFactory.getLogger(EntornoResolver.class);
    private final Clock reloj;
    private final boolean proxyConfiable;
    private final boolean demo;
    private final List<String> rangosPeru;

    public EntornoResolver(Clock reloj, Environment perfiles,
                           @Value("${securedocs.proxy-confiable:false}") boolean proxyConfiable,
                           @Value("${securedocs.rangos-ip-peru}") List<String> rangosPeru) {
        this.reloj = reloj;
        this.proxyConfiable = proxyConfiable;
        this.demo = perfiles.acceptsProfiles(Profiles.of("demo"));
        this.rangosPeru = List.copyOf(rangosPeru);
    }

    public Entorno resolver(HttpServletRequest request) {
        String ip = request.getRemoteAddr();
        if (proxyConfiable && request.getHeader("X-Forwarded-For") != null) {
            ip = request.getHeader("X-Forwarded-For").split(",")[0].trim();
        }
        LocalTime hora = LocalTime.now(reloj);
        LocalDate fecha = LocalDate.now(reloj);
        String ubicacion = ubicacionDe(ip);
        String dispositivo = "PERSONAL";
        if (demo) {
            if (request.getHeader("X-Sim-Hora") != null) {
                hora = LocalTime.parse(request.getHeader("X-Sim-Hora"));
            }
            if (request.getHeader("X-Sim-Fecha") != null) {
                fecha = LocalDate.parse(request.getHeader("X-Sim-Fecha"));
            }
            if (request.getHeader("X-Sim-Ip") != null) {
                ip = request.getHeader("X-Sim-Ip");
                ubicacion = ubicacionDe(ip);
            }
            if (request.getHeader("X-Sim-Ubicacion") != null) {
                ubicacion = request.getHeader("X-Sim-Ubicacion");
            }
            if (request.getHeader("X-Sim-Dispositivo") != null) {
                dispositivo = request.getHeader("X-Sim-Dispositivo");
            }
        } else if (List.of("X-Sim-Hora", "X-Sim-Fecha", "X-Sim-Ip", "X-Sim-Ubicacion", "X-Sim-Dispositivo")
            .stream().anyMatch(nombre -> request.getHeader(nombre) != null)) {
            LOG.warn("Se ignoraron cabeceras de simulación fuera del perfil demo");
        }
        return new Entorno(hora, fecha, ip, ubicacion, dispositivo);
    }

    private String ubicacionDe(String ip) {
        if ("::1".equals(ip)) {
            return "PERU";
        }
        for (String rango : rangosPeru) {
            if (pertenece(ip, rango)) {
                return "PERU";
            }
        }
        return "DESCONOCIDA";
    }

    private boolean pertenece(String ip, String cidr) {
        try {
            String[] partes = cidr.trim().split("/");
            byte[] direccion = InetAddress.getByName(ip).getAddress();
            byte[] red = InetAddress.getByName(partes[0]).getAddress();
            if (direccion.length != red.length) {
                return false;
            }
            int bits = Integer.parseInt(partes[1]);
            for (int i = 0; i < direccion.length; i++) {
                int usados = Math.max(0, Math.min(8, bits - i * 8));
                int mascara = usados == 0 ? 0 : 0xff << (8 - usados);
                if (((direccion[i] ^ red[i]) & mascara) != 0) {
                    return false;
                }
            }
            return true;
        } catch (UnknownHostException | NumberFormatException | ArrayIndexOutOfBoundsException ex) {
            return false;
        }
    }
}
