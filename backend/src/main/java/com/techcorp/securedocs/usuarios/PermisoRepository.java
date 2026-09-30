package com.techcorp.securedocs.usuarios;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PermisoRepository extends JpaRepository<Permiso, Long> {
    Optional<Permiso> findByCodigo(String codigo);
}
