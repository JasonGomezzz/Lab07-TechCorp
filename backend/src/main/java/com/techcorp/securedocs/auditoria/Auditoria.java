package com.techcorp.securedocs.auditoria;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "auditoria")
@Getter
@Setter
public class Auditoria {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "fecha_hora")
    private LocalDateTime fechaHora;
    private String usuario;
    @Column(name = "usuario_rol")
    private String usuarioRol;
    @Column(name = "usuario_departamento")
    private String usuarioDepartamento;
    private String recurso;
    private String accion;
    private String resultado;
    private String capa;
    private String motivo;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "politicas_evaluadas")
    private List<Map<String, Object>> politicasEvaluadas;
    private String ip;
    private String ubicacion;
    private String dispositivo;
    @Column(name = "departamento_recurso")
    private String departamentoRecurso;
}
