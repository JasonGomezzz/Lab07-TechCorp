package com.techcorp.securedocs.autenticacion;

import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TokenRevocadoRepository extends JpaRepository<TokenRevocado, String> {
    long deleteByExpiraEnBefore(LocalDateTime fecha);
}
