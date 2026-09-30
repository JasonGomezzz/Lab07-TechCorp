package com.techcorp.securedocs.politicas;

import java.util.List;
import com.techcorp.securedocs.autorizacion.abac.ConfigPolitica;
import org.springframework.stereotype.Service;

@Service
public class ConfiguracionPoliticas {
    private final PoliticaRepository repositorio;
    private volatile List<ConfigPolitica> cache;

    public ConfiguracionPoliticas(PoliticaRepository repositorio) {
        this.repositorio = repositorio;
    }

    public List<ConfigPolitica> activas() {
        List<ConfigPolitica> actual = cache;
        if (actual == null) {
            synchronized (this) {
                if (cache == null) {
                    cache = repositorio.findByActivaTrue().stream().map(ConfigPolitica::de).toList();
                }
                actual = cache;
            }
        }
        return actual;
    }

    public void invalidarCache() {
        cache = null;
    }
}
