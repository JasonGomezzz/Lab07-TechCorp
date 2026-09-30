import { useCallback, useEffect, useState } from 'react'
import { RefreshCw, ShieldCheck } from 'lucide-react'
import { api, errorTexto } from './api'
import { Titulo, Vacio } from './App'
import type { Politica } from './types'

export function Politicas({ puedeEditar }: { puedeEditar: boolean }) {
  const [politicas, setPoliticas] = useState<Politica[]>([])
  const [error, setError] = useState('')
  const [aviso, setAviso] = useState('')
  const [seleccionada, setSeleccionada] = useState<Politica | null>(null)
  const [json, setJson] = useState('')
  const cargar = useCallback(async () => { try { setPoliticas((await api.get<Politica[]>('/politicas')).data); setError('') } catch (e) { setError(errorTexto(e)) } }, [])
  useEffect(() => { void cargar() }, [cargar])
  async function alternar(p: Politica) { try { await api.put(`/politicas/${p.codigo}`, { activa: !p.activa }); setAviso(`${p.codigo} ${p.activa ? 'desactivada' : 'activada'}`); await cargar() } catch (e) { setError(errorTexto(e)) } }
  async function guardar() { if (!seleccionada) return; try { const data = JSON.parse(json) as Record<string, unknown>; await api.put(`/politicas/${seleccionada.codigo}`, data); setSeleccionada(null); setAviso('Política actualizada'); await cargar() } catch (e) { setError(errorTexto(e)) } }
  return <><Titulo eyebrow="Motor ABAC" titulo="Políticas" descripcion="Reglas configurables que se evalúan tras validar el permiso RBAC." action={<button className="btn-ghost" onClick={cargar}><RefreshCw size={16} /> Actualizar</button>} />
    {error && <p role="alert" className="mb-4 rounded-lg bg-red-50 p-3 text-sm text-red-700">{error}</p>}{aviso && <p role="status" className="mb-4 rounded-lg bg-emerald-50 p-3 text-sm text-emerald-700">{aviso}</p>}
    {politicas.length === 0 ? <Vacio texto="No hay políticas disponibles." /> : <div className="grid gap-4 xl:grid-cols-2">{politicas.map(p => <article key={p.codigo} className="card p-5"><div className="flex items-start justify-between gap-4"><div className="flex gap-3"><div className="rounded-lg bg-cyan-50 p-2 text-[#145a72]"><ShieldCheck size={20} /></div><div><p className="text-xs font-bold text-[#145a72]">{p.codigo}</p><h2 className="mt-1 font-bold">{p.nombre}</h2></div></div><span className={`badge ${p.activa ? 'bg-emerald-50 text-emerald-700' : 'bg-slate-100 text-slate-600'}`}>{p.activa ? 'Activa' : 'Inactiva'}</span></div><p className="mt-4 text-sm text-slate-600">{p.descripcion}</p><div className="mt-4 flex flex-wrap gap-2 text-xs text-slate-500">{p.acciones.map(a => <span key={a} className="rounded bg-slate-100 px-2 py-1">{a}</span>)}</div>{Object.keys(p.parametros).length > 0 && <p className="mt-3 text-xs text-slate-500">Parámetros: {JSON.stringify(p.parametros)}</p>}{p.rolesExentos.length > 0 && <p className="mt-2 text-xs text-slate-500">Roles exentos: {p.rolesExentos.join(', ')}</p>}{puedeEditar && p.codigo !== 'P7_ESTADO_USUARIO' && <div className="mt-5 flex gap-2 border-t border-slate-100 pt-4"><button className="btn-ghost" onClick={() => { setSeleccionada(p); setJson(JSON.stringify({ activa: p.activa, acciones: p.acciones, rolesExentos: p.rolesExentos, parametros: p.parametros }, null, 2)) }}>Editar configuración</button><button className="btn-ghost" onClick={() => alternar(p)}>{p.activa ? 'Desactivar' : 'Activar'}</button></div>}</article>)}</div>}
    {seleccionada && <div className="fixed inset-0 z-30 flex items-center justify-center bg-slate-950/50 p-4" role="dialog" aria-modal="true"><div className="card w-full max-w-xl p-6"><h2 className="text-xl font-bold">Editar {seleccionada.codigo}</h2><p className="mt-2 text-sm text-slate-500">Acciones, roles exentos y parámetros JSON. La API valida los cambios.</p><textarea className="field mt-4 min-h-72 font-mono" aria-label="Configuración JSON" value={json} onChange={e => setJson(e.target.value)} /><div className="mt-4 flex gap-2"><button className="btn-primary" onClick={guardar}>Guardar</button><button className="btn-ghost" onClick={() => setSeleccionada(null)}>Cancelar</button></div></div></div>}
  </>
}
