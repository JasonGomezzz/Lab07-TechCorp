package com.techcorp.securedocs.autorizacion.rbac;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import com.techcorp.securedocs.usuarios.RolPermisoRepository;
import org.springframework.stereotype.Service;

@Service
public class ServicioRbac {
    private final RolPermisoRepository repositorio;
    private final ConcurrentHashMap<String, Set<String>> cache = new ConcurrentHashMap<>();

    public ServicioRbac(RolPermisoRepository repositorio) {
        this.repositorio = repositorio;
    }

    public Set<String> permisosDe(String codigoRol) {
        return cache.computeIfAbsent(codigoRol, codigo -> repositorio.findByRol_Codigo(codigo)
            .stream().map(asignacion -> asignacion.getPermiso().getCodigo()).collect(Collectors.toUnmodifiableSet()));
    }

    public boolean permite(String codigoRol, String codigoPermiso) {
        return permisosDe(codigoRol).contains(codigoPermiso);
    }

    public void invalidarCache() {
        cache.clear();
    }
}
