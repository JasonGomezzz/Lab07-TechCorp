package com.techcorp.securedocs.usuarios;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "permiso")
@Getter
@Setter
public class Permiso {
    @Id
    private Long id;
    private String codigo;
    private String descripcion;
}
