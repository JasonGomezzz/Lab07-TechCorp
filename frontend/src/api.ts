import axios from 'axios'
import type { Simulacion } from './types'

export const demo = import.meta.env.VITE_DEMO === 'true'
export const entornoNormal: Simulacion = { hora: '10:30', ubicacion: 'PERU', dispositivo: 'CORPORATIVO', ip: '192.168.10.20' }
export const api = axios.create({ baseURL: import.meta.env.VITE_API_URL || '/api' })

api.interceptors.request.use(config => {
  const token = sessionStorage.getItem('securedocs_token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  if (demo) {
    const sim = JSON.parse(sessionStorage.getItem('securedocs_sim') || 'null') as Simulacion | null
    if (sim) {
      config.headers['X-Sim-Hora'] = sim.hora
      config.headers['X-Sim-Ubicacion'] = sim.ubicacion
      config.headers['X-Sim-Dispositivo'] = sim.dispositivo
      config.headers['X-Sim-Ip'] = sim.ip
    }
  }
  return config
})

export function errorTexto(error: unknown): string {
  if (axios.isAxiosError(error)) return error.response?.data?.detail || error.response?.data?.title || error.message
  return error instanceof Error ? error.message : 'Ocurrió un error inesperado'
}
