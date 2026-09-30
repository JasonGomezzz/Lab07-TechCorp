export type Usuario = { id: number; username: string; nombre: string; correo: string; rol: string; departamento: string; nivelSeguridad: number; pais: string; tipoContrato: string; estado: string; accesoHasta?: string | null; permisos?: string[] }
export type Documento = { id: number; titulo: string; descripcion: string; propietarioId: number; departamento: string; nivelConfidencialidad: number; estado: string; pais: string; fechaCreacion: string }
export type Politica = { codigo: string; nombre: string; descripcion: string; acciones: string[]; rolesExentos: string[]; parametros: Record<string, unknown>; activa: boolean }
export type Auditoria = { id: number; fechaHora: string; usuario: string; usuarioRol: string; usuarioDepartamento: string; recurso: string; accion: string; resultado: string; capa: string; motivo: string; politicasEvaluadas: { codigo?: string; resultado?: string; motivo?: string }[]; ip: string; ubicacion: string; dispositivo: string }
export type Pagina<T> = { content: T[]; totalElements: number; number: number; size: number; totalPages: number }
export type Simulacion = { hora: string; ubicacion: string; dispositivo: string; ip: string }
