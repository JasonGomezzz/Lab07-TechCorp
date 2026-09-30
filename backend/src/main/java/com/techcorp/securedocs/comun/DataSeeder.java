package com.techcorp.securedocs.comun;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Profile("demo")
public class DataSeeder implements ApplicationRunner {

    private final JdbcTemplate jdbc;
    private final String passwordDemo;

    public DataSeeder(JdbcTemplate jdbc, @Value("${SEED_PASSWORD:Secure#2026}") String passwordDemo) {
        this.jdbc = jdbc;
        this.passwordDemo = passwordDemo;
    }

    @Override
    public void run(ApplicationArguments args) {
        String hash = new BCryptPasswordEncoder(12).encode(passwordDemo);
        usuario(1, "admin", "Admin TechCorp", 1, 1, 5, "ACTIVO", "INTERNO", null, hash);
        usuario(10, "laura.mendez", "Laura Méndez", 2, 2, 5, "ACTIVO", "INTERNO", null, hash);
        usuario(25, "ana.torres", "Ana Torres", 3, 2, 3, "ACTIVO", "INTERNO", null, hash);
        usuario(26, "carlos.ruiz", "Carlos Ruiz", 3, 2, 3, "ACTIVO", "INTERNO", null, hash);
        usuario(30, "diego.salas", "Diego Salas", 4, 2, 2, "ACTIVO", "INTERNO", null, hash);
        usuario(31, "marta.quispe", "Marta Quispe", 4, 3, 2, "ACTIVO", "INTERNO", null, hash);
        usuario(40, "sofia.paredes", "Sofía Paredes", 5, 4, 4, "ACTIVO", "INTERNO", null, hash);
        usuario(50, "pedro.suspendido", "Pedro Vidal", 4, 2, 2, "SUSPENDIDO", "INTERNO", null, hash);
        usuario(51, "rosa.inactiva", "Rosa Lazo", 4, 2, 2, "INACTIVO", "INTERNO", null, hash);
        usuario(52, "juan.temporal", "Juan Ortiz", 4, 2, 2, "ACTIVO", "INTERNO", null, hash);
        usuario(60, "invitado.externo", "Invitado Externo", 6, 2, 1, "ACTIVO", "EXTERNO", LocalDate.of(2099, 12, 31), hash);
        usuario(61, "invitado.vencido", "Invitado Vencido", 6, 2, 1, "ACTIVO", "EXTERNO", LocalDate.of(2020, 1, 1), hash);

        documento(501, "Presupuesto 2027", 2, 3, "PENDIENTE", "PERU", 25);
        documento(502, "Informe de gastos Q3", 2, 1, "PUBLICADO", "PERU", 25);
        documento(503, "Presupuesto corporativo", 2, 4, "PUBLICADO", "PERU", 10);
        documento(504, "Plan estratégico 2027", 2, 5, "PUBLICADO", "PERU", 10);
        documento(505, "Política de vacaciones", 3, 2, "PUBLICADO", "PERU", 31);
        documento(506, "Contrato proveedor México", 2, 2, "PUBLICADO", "MEXICO", 10);
        documento(507, "Borrador circular interna", 2, 1, "BORRADOR", "PERU", 30);
        documento(508, "Acta de reunión", 2, 2, "PUBLICADO", "PERU", 30);
        documento(509, "Documento eliminable (prueba)", 2, 2, "PUBLICADO", "PERU", 30);
        documento(511, "Presupuesto anual", 2, 3, "PENDIENTE", "PERU", 25);
    }

    private void usuario(long id, String username, String nombre, long rolId, long departamentoId,
                         int nivel, String estado, String contrato, LocalDate accesoHasta, String hash) {
        jdbc.update("""
            INSERT INTO usuario (id, username, nombre, correo, password_hash, rol_id, departamento_id,
                                 nivel_seguridad, pais, tipo_contrato, estado, acceso_hasta)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'PERU', ?, ?, ?)
            ON DUPLICATE KEY UPDATE username=VALUES(username), nombre=VALUES(nombre), correo=VALUES(correo),
                password_hash=VALUES(password_hash), rol_id=VALUES(rol_id), departamento_id=VALUES(departamento_id),
                nivel_seguridad=VALUES(nivel_seguridad), pais=VALUES(pais), tipo_contrato=VALUES(tipo_contrato),
                estado=VALUES(estado), acceso_hasta=VALUES(acceso_hasta)
            """, id, username, nombre, username + "@techcorp.example", hash, rolId, departamentoId,
            nivel, contrato, estado, accesoHasta == null ? null : Date.valueOf(accesoHasta));
    }

    private void documento(long id, String titulo, long departamentoId, int nivel, String estado,
                           String pais, long propietarioId) {
        jdbc.update("""
            INSERT INTO documento (id, titulo, descripcion, propietario_id, departamento_id,
                                   nivel_confidencialidad, estado, pais, fecha_creacion, eliminado_en)
            VALUES (?, ?, 'Documento de demostración', ?, ?, ?, ?, ?, ?, NULL)
            ON DUPLICATE KEY UPDATE titulo=VALUES(titulo), descripcion=VALUES(descripcion),
                propietario_id=VALUES(propietario_id), departamento_id=VALUES(departamento_id),
                nivel_confidencialidad=VALUES(nivel_confidencialidad), estado=VALUES(estado),
                pais=VALUES(pais), fecha_creacion=VALUES(fecha_creacion), eliminado_en=NULL
            """, id, titulo, propietarioId, departamentoId, nivel, estado, pais,
            Timestamp.valueOf(LocalDateTime.of(2026, 1, 1, 10, 0)));
    }
}
