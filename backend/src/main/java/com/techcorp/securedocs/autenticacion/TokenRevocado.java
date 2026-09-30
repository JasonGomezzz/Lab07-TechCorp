package com.techcorp.securedocs.autenticacion;

import java.time.LocalDateTime;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "token_revocado")
@Getter
@Setter
public class TokenRevocado {
    @Id
    private String jti;
    @Column(name = "expira_en")
    private LocalDateTime expiraEn;
}
