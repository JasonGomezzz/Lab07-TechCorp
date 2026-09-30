package com.techcorp.securedocs.autorizacion;

import java.time.LocalDate;
import com.techcorp.securedocs.usuarios.Usuario;

public record Sujeto(long id, String username, String rol, String departamento,
                     int nivelSeguridad, String pais, String tipoContrato,
                     String estado, LocalDate accesoHasta) {
    public static Sujeto de(Usuario usuario) {
        return new Sujeto(usuario.getId(), usuario.getUsername(), usuario.getRol().getCodigo(),
            usuario.getDepartamento().getCodigo(), usuario.getNivelSeguridad(), usuario.getPais(),
            usuario.getTipoContrato(), usuario.getEstado(), usuario.getAccesoHasta());
    }
}
