package com.techcorp.securedocs.politicas;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PoliticaRepository extends JpaRepository<Politica, String> {
    List<Politica> findByActivaTrue();
}
