package com.techcorp.securedocs.autenticacion;

import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.UUID;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ServicioJwt {
    private final byte[] clave;
    private final Clock reloj;
    private final TokenRevocadoRepository revocados;

    public ServicioJwt(@Value("${JWT_SECRET:}") String secreto, Clock reloj,
                       TokenRevocadoRepository revocados) {
        this.clave = secreto.getBytes(StandardCharsets.UTF_8);
        if (clave.length < 32) {
            throw new IllegalStateException("JWT_SECRET debe tener al menos 32 bytes UTF-8");
        }
        this.reloj = reloj;
        this.revocados = revocados;
    }

    public String emitir(Long usuarioId) {
        Instant ahora = reloj.instant();
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
            .subject(usuarioId.toString())
            .jwtID(UUID.randomUUID().toString())
            .issueTime(Date.from(ahora))
            .expirationTime(Date.from(ahora.plusSeconds(3600)))
            .build();
        try {
            SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
            jwt.sign(new MACSigner(clave));
            return jwt.serialize();
        } catch (JOSEException ex) {
            throw new IllegalStateException("No se pudo emitir el token", ex);
        }
    }

    public DatosToken verificar(String token) {
        try {
            SignedJWT jwt = SignedJWT.parse(token);
            if (!JWSAlgorithm.HS256.equals(jwt.getHeader().getAlgorithm())
                || !jwt.verify(new MACVerifier(clave))) {
                throw new TokenInvalidoException("TOKEN_INVALIDO");
            }
            JWTClaimsSet claims = jwt.getJWTClaimsSet();
            Date vencimiento = claims.getExpirationTime();
            Date emision = claims.getIssueTime();
            String jti = claims.getJWTID();
            if (vencimiento == null || emision == null || jti == null || jti.isBlank()
                || !vencimiento.toInstant().isAfter(reloj.instant())
                || emision.toInstant().isAfter(reloj.instant())) {
                throw new TokenInvalidoException("TOKEN_EXPIRADO_O_INVALIDO");
            }
            Long usuarioId = Long.valueOf(claims.getSubject());
            if (revocados.existsById(jti)) {
                throw new TokenInvalidoException("TOKEN_REVOCADO");
            }
            return new DatosToken(usuarioId, jti, vencimiento.toInstant());
        } catch (ParseException | JOSEException | NumberFormatException | NullPointerException ex) {
            throw new TokenInvalidoException("TOKEN_INVALIDO");
        }
    }

    @Transactional
    public void revocar(String token) {
        DatosToken datos = verificar(token);
        TokenRevocado revocado = new TokenRevocado();
        revocado.setJti(datos.jti());
        revocado.setExpiraEn(LocalDateTime.ofInstant(datos.expiraEn(), reloj.getZone()));
        revocados.save(revocado);
    }

    public record DatosToken(Long usuarioId, String jti, Instant expiraEn) {
    }
}
