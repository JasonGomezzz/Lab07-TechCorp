package com.techcorp.securedocs.documentos;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentoRepository extends JpaRepository<Documento, Long> {
    @EntityGraph(attributePaths = {"propietario", "departamento"})
    Optional<Documento> findWithPropietarioAndDepartamentoById(Long id);

    @EntityGraph(attributePaths = {"propietario", "departamento"})
    List<Documento> findByEstadoNot(String estado);
}
