package com.techcorp.securedocs.usuarios;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DepartamentoRepository extends JpaRepository<Departamento, Long> {
    Optional<Departamento> findByCodigo(String codigo);
}
