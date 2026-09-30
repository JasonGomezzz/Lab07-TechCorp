package com.techcorp.securedocs.usuarios;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "rol")
@Getter
@Setter
public class Rol {
    @Id
    private Long id;
    private String codigo;
    private String nombre;
}
