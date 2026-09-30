package com.techcorp.securedocs.autenticacion;

import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class LimpiezaTokensRevocados {
    private final TokenRevocadoRepository tokens;
    private final Clock reloj;

    public LimpiezaTokensRevocados(TokenRevocadoRepository tokens, Clock reloj) {
        this.tokens = tokens;
        this.reloj = reloj;
    }

    @Transactional
    @Scheduled(initialDelay = 3_600_000, fixedDelay = 3_600_000)
    public void limpiarExpirados() {
        tokens.deleteByExpiraEnBefore(LocalDateTime.now(reloj));
    }
}
