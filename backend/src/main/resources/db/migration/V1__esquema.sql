CREATE TABLE rol (
    id BIGINT PRIMARY KEY,
    codigo VARCHAR(30) NOT NULL UNIQUE,
    nombre VARCHAR(80) NOT NULL
);

CREATE TABLE permiso (
    id BIGINT PRIMARY KEY,
    codigo VARCHAR(40) NOT NULL UNIQUE,
    descripcion VARCHAR(200) NOT NULL
);

CREATE TABLE rol_permiso (
    rol_id BIGINT NOT NULL,
    permiso_id BIGINT NOT NULL,
    PRIMARY KEY (rol_id, permiso_id),
    CONSTRAINT fk_rol_permiso_rol FOREIGN KEY (rol_id) REFERENCES rol(id),
    CONSTRAINT fk_rol_permiso_permiso FOREIGN KEY (permiso_id) REFERENCES permiso(id)
);

CREATE TABLE departamento (
    id BIGINT PRIMARY KEY,
    codigo VARCHAR(30) NOT NULL UNIQUE,
    nombre VARCHAR(80) NOT NULL
);

CREATE TABLE usuario (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(80) NOT NULL UNIQUE,
    nombre VARCHAR(120) NOT NULL,
    correo VARCHAR(160) NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    rol_id BIGINT NOT NULL,
    departamento_id BIGINT NOT NULL,
    nivel_seguridad TINYINT NOT NULL,
    pais VARCHAR(20) NOT NULL,
    tipo_contrato VARCHAR(20) NOT NULL,
    estado VARCHAR(20) NOT NULL,
    acceso_hasta DATE NULL,
    CONSTRAINT chk_usuario_nivel CHECK (nivel_seguridad BETWEEN 0 AND 5),
    CONSTRAINT fk_usuario_rol FOREIGN KEY (rol_id) REFERENCES rol(id),
    CONSTRAINT fk_usuario_departamento FOREIGN KEY (departamento_id) REFERENCES departamento(id)
);

CREATE TABLE documento (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    titulo VARCHAR(200) NOT NULL,
    descripcion TEXT NOT NULL,
    propietario_id BIGINT NOT NULL,
    departamento_id BIGINT NOT NULL,
    nivel_confidencialidad TINYINT NOT NULL,
    estado VARCHAR(20) NOT NULL,
    pais VARCHAR(20) NOT NULL,
    fecha_creacion DATETIME(6) NOT NULL,
    eliminado_en DATETIME(6) NULL,
    CONSTRAINT chk_documento_nivel CHECK (nivel_confidencialidad BETWEEN 0 AND 5),
    CONSTRAINT fk_documento_propietario FOREIGN KEY (propietario_id) REFERENCES usuario(id),
    CONSTRAINT fk_documento_departamento FOREIGN KEY (departamento_id) REFERENCES departamento(id),
    INDEX idx_documento_departamento_estado (departamento_id, estado)
);

CREATE TABLE politica (
    codigo VARCHAR(40) PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    descripcion VARCHAR(500) NOT NULL,
    acciones JSON NOT NULL,
    roles_exentos JSON NOT NULL,
    parametros JSON NOT NULL,
    activa BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE token_revocado (
    jti VARCHAR(80) PRIMARY KEY,
    expira_en DATETIME(6) NOT NULL,
    INDEX idx_token_revocado_expira (expira_en)
);

CREATE TABLE auditoria (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    fecha_hora DATETIME(6) NOT NULL,
    usuario VARCHAR(80) NOT NULL,
    usuario_rol VARCHAR(30) NULL,
    usuario_departamento VARCHAR(30) NULL,
    recurso VARCHAR(100) NOT NULL,
    accion VARCHAR(30) NOT NULL,
    resultado VARCHAR(20) NOT NULL,
    capa VARCHAR(20) NOT NULL,
    motivo TEXT NOT NULL,
    politicas_evaluadas JSON NOT NULL,
    ip VARCHAR(45) NULL,
    ubicacion VARCHAR(30) NULL,
    dispositivo VARCHAR(30) NULL,
    departamento_recurso VARCHAR(30) NULL,
    INDEX idx_auditoria_fecha (fecha_hora),
    INDEX idx_auditoria_usuario (usuario),
    INDEX idx_auditoria_recurso (recurso),
    INDEX idx_auditoria_resultado (resultado)
);
