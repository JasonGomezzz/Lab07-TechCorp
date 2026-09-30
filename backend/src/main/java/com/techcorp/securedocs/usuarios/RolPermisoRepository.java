package com.techcorp.securedocs.usuarios;

import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RolPermisoRepository extends JpaRepository<RolPermiso, RolPermisoId> {
    @EntityGraph(attributePaths = "permiso")
    List<RolPermiso> findByRol_Codigo(String codigo);
}
