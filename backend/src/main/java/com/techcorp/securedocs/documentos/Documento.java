package com.techcorp.securedocs.documentos;

import java.time.LocalDateTime;
import com.techcorp.securedocs.usuarios.Departamento;
import com.techcorp.securedocs.usuarios.Usuario;
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
@Table(name = "documento")
@Getter
@Setter
public class Documento {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String titulo;
    private String descripcion;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "propietario_id", nullable = false)
    private Usuario propietario;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "departamento_id", nullable = false)
    private Departamento departamento;
    @Column(name = "nivel_confidencialidad")
    private byte nivelConfidencialidad;
    private String estado;
    private String pais;
    @Column(name = "fecha_creacion")
    private LocalDateTime fechaCreacion;
    @Column(name = "eliminado_en")
    private LocalDateTime eliminadoEn;
}
