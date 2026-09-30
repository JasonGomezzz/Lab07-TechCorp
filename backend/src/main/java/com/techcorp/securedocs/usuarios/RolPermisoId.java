package com.techcorp.securedocs.usuarios;

import java.io.Serializable;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Embeddable
@Getter
@Setter
@EqualsAndHashCode
public class RolPermisoId implements Serializable {
    @Column(name = "rol_id")
    private Long rolId;
    @Column(name = "permiso_id")
    private Long permisoId;
}
