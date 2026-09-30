import axios, { type AxiosRequestConfig } from 'axios'
import { entornoNormal } from './api'
import type { Documento, Simulacion, Usuario } from './types'

export type Caso = { id: string; descripcion: string; usuario: string; metodo: 'GET' | 'PUT' | 'POST' | 'DELETE'; ruta: string; esperado: number; codigo?: string; politicas?: string[]; entorno?: Partial<Simulacion>; body?: object; especial?: 'inactivos' | 'revocacion' | 'listado' }
export const casos: Caso[] = [
  { id: 'T1', descripcion: 'Diego consulta un documento autorizado', usuario: 'diego.salas', metodo: 'GET', ruta: '/documentos/502', esperado: 200 },
  { id: 'T2', descripcion: 'Diego accede a otro departamento', usuario: 'diego.salas', metodo: 'GET', ruta: '/documentos/505', esperado: 403, codigo: 'ABAC_DENEGADO', politicas: ['P1_DEPARTAMENTO'] },
  { id: 'T3', descripcion: 'Carlos aprueba el presupuesto pendiente', usuario: 'carlos.ruiz', metodo: 'POST', ruta: '/documentos/511/aprobar', esperado: 200 },
  { id: 'T4', descripcion: 'Diego intenta aprobar sin permiso', usuario: 'diego.salas', metodo: 'POST', ruta: '/documentos/511/aprobar', esperado: 403, codigo: 'RBAC_DENEGADO' },
  { id: 'T5', descripcion: 'Diego consulta un nivel superior', usuario: 'diego.salas', metodo: 'GET', ruta: '/documentos/503', esperado: 403, codigo: 'ABAC_DENEGADO', politicas: ['P2_NIVEL_SEGURIDAD'] },
  { id: 'T6', descripcion: 'Laura elimina un documento (ejecutar al final)', usuario: 'laura.mendez', metodo: 'DELETE', ruta: '/documentos/509', esperado: 204 },
  { id: 'T7', descripcion: 'Sofía modifica sin permiso RBAC', usuario: 'sofia.paredes', metodo: 'PUT', ruta: '/documentos/502', body: { titulo: 'Intento sin permiso' }, esperado: 403, codigo: 'RBAC_DENEGADO' },
  { id: 'T8', descripcion: 'Pedro y Rosa no pueden iniciar sesión', usuario: 'pedro.suspendido / rosa.inactiva', metodo: 'POST', ruta: '/auth/login', esperado: 403, codigo: 'USUARIO_NO_ACTIVO', especial: 'inactivos' },
  { id: 'T9', descripcion: 'Laura consulta fuera de horario', usuario: 'laura.mendez', metodo: 'GET', ruta: '/documentos/503', esperado: 403, codigo: 'ABAC_DENEGADO', politicas: ['P4_HORARIO'], entorno: { hora: '19:00' } },
  { id: 'T10', descripcion: 'Laura usa un dispositivo personal', usuario: 'laura.mendez', metodo: 'GET', ruta: '/documentos/504', esperado: 403, codigo: 'ABAC_DENEGADO', politicas: ['P6_DISPOSITIVO'], entorno: { dispositivo: 'PERSONAL' } },
  { id: 'T11', descripcion: 'Invitado externo consulta un documento público', usuario: 'invitado.externo', metodo: 'GET', ruta: '/documentos/502', esperado: 200 },
  { id: 'T12', descripcion: 'Invitado externo consulta un documento sensible', usuario: 'invitado.externo', metodo: 'GET', ruta: '/documentos/503', esperado: 403, codigo: 'ABAC_DENEGADO', politicas: ['P2_NIVEL_SEGURIDAD','P8_INVITADO'] },
  { id: 'T13', descripcion: 'Diego modifica un documento ajeno', usuario: 'diego.salas', metodo: 'PUT', ruta: '/documentos/502', body: { titulo: 'Intento ajeno' }, esperado: 403, codigo: 'ABAC_DENEGADO', politicas: ['P3_PROPIEDAD'] },
  { id: 'T14', descripcion: 'Laura modifica con exención de propiedad', usuario: 'laura.mendez', metodo: 'PUT', ruta: '/documentos/502', body: { titulo: 'Informe de gastos Q3' }, esperado: 200 },
  { id: 'T15', descripcion: 'Juan pierde acceso al desactivarse', usuario: 'juan.temporal', metodo: 'GET', ruta: '/documentos', esperado: 401, codigo: 'USUARIO_NO_ACTIVO', especial: 'revocacion' },
  { id: 'T16', descripcion: 'Diego consulta desde México', usuario: 'diego.salas', metodo: 'GET', ruta: '/documentos/502', esperado: 403, codigo: 'ABAC_DENEGADO', politicas: ['P5B_UBICACION'], entorno: { ubicacion: 'MEXICO' } },
  { id: 'T17', descripcion: 'El listado oculta recursos no autorizados', usuario: 'diego.salas', metodo: 'GET', ruta: '/documentos', esperado: 200, especial: 'listado' },
]
export type Resultado = { id: string; esperado: string; obtenido: string; correcto: boolean; detalle: string }
const cliente = axios.create({ baseURL: import.meta.env.VITE_API_URL || '/api', validateStatus: () => true })
function cabeceras(token: string | null, entorno: Partial<Simulacion> = {}): Record<string,string> { const e = { ...entornoNormal, ...entorno }; return { ...(token ? { Authorization: `Bearer ${token}` } : {}), 'X-Sim-Hora': e.hora, 'X-Sim-Ubicacion': e.ubicacion, 'X-Sim-Dispositivo': e.dispositivo, 'X-Sim-Ip': e.ip } }
async function token(usuario: string, password: string): Promise<string> { const r = await cliente.post('/auth/login', { username: usuario, password }, { headers: cabeceras(null) }); if (r.status !== 200) throw new Error(`Login ${usuario}: HTTP ${r.status} ${r.data?.codigo || ''}`); return r.data.token }
async function request(caso: Caso, clave: string) {
  const auth = await token(caso.usuario, clave)
  const config: AxiosRequestConfig = { method: caso.metodo, url: caso.ruta, data: caso.body, headers: cabeceras(auth, caso.entorno) }
  return cliente.request(config)
}
export async function ejecutarCaso(caso: Caso, password: string): Promise<Resultado> {
  const esperado = `HTTP ${caso.esperado}${caso.codigo ? ` · ${caso.codigo}` : ''}${caso.politicas ? ` · ${caso.politicas.join(', ')}` : ''}`
  try {
    let status: number; let data: unknown
    if (caso.especial === 'inactivos') {
      const resultados = await Promise.all(['pedro.suspendido', 'rosa.inactiva'].map(username => cliente.post('/auth/login', { username, password }, { headers: cabeceras(null) })))
      status = resultados.every(r => r.status === 403 && r.data?.codigo === caso.codigo) ? 403 : resultados[0].status
      data = resultados.map(r => `${r.status} ${r.data?.codigo || ''}`).join(' / ')
    } else if (caso.especial === 'revocacion') {
      const juanToken = await token('juan.temporal', password)
      const adminToken = await token('admin', password)
      const usuarios = await cliente.get<Usuario[]>('/usuarios', { headers: cabeceras(adminToken) })
      const juan = usuarios.data.find(u => u.username === 'juan.temporal')
      if (!juan) throw new Error('No se encontró a Juan para T15')
      const cambio = await cliente.put(`/usuarios/${juan.id}`, { estado: 'INACTIVO' }, { headers: cabeceras(adminToken) })
      if (cambio.status !== 200) throw new Error(`No se pudo desactivar a Juan: HTTP ${cambio.status}`)
      let restauracion = 0
      try { const r = await cliente.get('/documentos', { headers: cabeceras(juanToken) }); status = r.status; data = r.data }
      finally { restauracion = (await cliente.put(`/usuarios/${juan.id}`, { estado: 'ACTIVO' }, { headers: cabeceras(adminToken) })).status }
      if (restauracion !== 200) throw new Error(`Juan quedó inactivo: restauración HTTP ${restauracion}`)
    } else { const r = await request(caso, password); status = r.status; data = r.data }
    const body = data as { codigo?: string; politicas?: string[] } | undefined
    let correcto = status === caso.esperado && (!caso.codigo || (caso.especial === 'inactivos' ? true : body?.codigo === caso.codigo))
    if (caso.politicas) correcto = correcto && JSON.stringify([...(body?.politicas || [])].sort()) === JSON.stringify([...caso.politicas].sort())
    if (caso.especial === 'listado') correcto = correcto && JSON.stringify((data as Documento[]).map(d => d.id).sort()) === JSON.stringify([502,507,508,509])
    const obtenido = `HTTP ${status}${body?.codigo ? ` · ${body.codigo}` : ''}${body?.politicas ? ` · ${body.politicas.join(', ')}` : ''}`
    return { id: caso.id, esperado, obtenido, correcto, detalle: caso.especial === 'listado' ? `IDs: ${(data as Documento[]).map(d => d.id).join(', ')}` : caso.especial === 'inactivos' ? String(data) : body?.codigo || 'Respuesta recibida' }
  } catch (e) { return { id: caso.id, esperado, obtenido: 'Error de ejecución', correcto: false, detalle: e instanceof Error ? e.message : String(e) } }
}
export const ordenCasos = [...casos.filter(c => !['T6','T17'].includes(c.id)), casos.find(c => c.id === 'T17')!, casos.find(c => c.id === 'T6')!]
