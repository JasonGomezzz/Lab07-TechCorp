package com.techcorp.securedocs.usuarios;

import java.time.LocalDate;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "usuario")
@Getter
@Setter
public class Usuario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String username;
    private String nombre;
    private String correo;
    @Column(name = "password_hash")
    private String passwordHash;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rol_id", nullable = false)
    private Rol rol;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "departamento_id", nullable = false)
    private Departamento departamento;
    @Column(name = "nivel_seguridad")
    private byte nivelSeguridad;
    private String pais;
    @Column(name = "tipo_contrato")
    private String tipoContrato;
    private String estado;
    @Column(name = "acceso_hasta")
    private LocalDate accesoHasta;
}
