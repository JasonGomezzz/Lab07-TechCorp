package com.techcorp.securedocs.usuarios;

import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    @EntityGraph(attributePaths = {"rol", "departamento"})
    Optional<Usuario> findByUsername(String username);

    @EntityGraph(attributePaths = {"rol", "departamento"})
    Optional<Usuario> findWithRolAndDepartamentoById(Long id);

    boolean existsByCorreo(String correo);
}
