package com.techcorp.securedocs.usuarios;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "rol_permiso")
@Getter
@Setter
public class RolPermiso {
    @EmbeddedId
    private RolPermisoId id;
    @MapsId("rolId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rol_id")
    private Rol rol;
    @MapsId("permisoId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "permiso_id")
    private Permiso permiso;
}
