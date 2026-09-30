package com.techcorp.securedocs.autorizacion;

public enum Accion {
    CREATE("CREAR_DOCUMENTO"),
    READ("CONSULTAR_DOCUMENTO"),
    LIST("CONSULTAR_DOCUMENTO"),
    UPDATE("MODIFICAR_DOCUMENTO"),
    DELETE("ELIMINAR_DOCUMENTO"),
    APPROVE("APROBAR_DOCUMENTO"),
    AUDIT_READ("VER_AUDITORIA"),
    USER_LIST("GESTIONAR_USUARIOS"),
    USER_CREATE("GESTIONAR_USUARIOS"),
    USER_UPDATE("GESTIONAR_USUARIOS"),
    USER_ROLE_ASSIGN("ASIGNAR_ROLES"),
    POLICY_LIST("GESTIONAR_CONFIGURACION"),
    POLICY_UPDATE("GESTIONAR_CONFIGURACION");

    private final String permiso;

    Accion(String permiso) {
        this.permiso = permiso;
    }

    public String permiso() {
        return permiso;
    }

    public boolean documental() {
        return switch (this) {
            case CREATE, READ, LIST, UPDATE, DELETE, APPROVE -> true;
            default -> false;
        };
    }
}
