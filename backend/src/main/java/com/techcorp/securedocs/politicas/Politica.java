package com.techcorp.securedocs.politicas;

import java.util.List;
import java.util.Map;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "politica")
@Getter
@Setter
public class Politica {
    @Id
    private String codigo;
    private String nombre;
    private String descripcion;
    @JdbcTypeCode(SqlTypes.JSON)
    private List<String> acciones;
    @JdbcTypeCode(SqlTypes.JSON)
    private List<String> rolesExentos;
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> parametros;
    private boolean activa;
}
