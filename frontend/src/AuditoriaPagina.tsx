import { useCallback, useEffect, useState, type FormEvent } from 'react'
import { Download, Search } from 'lucide-react'
import { api, errorTexto } from './api'
import { Titulo, Vacio } from './App'
import type { Auditoria, Pagina } from './types'

type Filtros = { usuario: string; recurso: string; accion: string; resultado: string; desde: string; hasta: string }
const inicial: Filtros = { usuario: '', recurso: '', accion: '', resultado: '', desde: '', hasta: '' }
export function AuditoriaPagina() {
  const [filtros, setFiltros] = useState<Filtros>(inicial)
  const [aplicados, setAplicados] = useState<Filtros>(inicial)
  const [pagina, setPagina] = useState(0)
  const [datos, setDatos] = useState<Pagina<Auditoria> | null>(null)
  const [error, setError] = useState('')
  const params = useCallback((f: Filtros, p: number) => { const q = new URLSearchParams({ pagina: String(p), tamano: '20' }); Object.entries(f).forEach(([k,v]) => { if (v) q.set(k, v) }); return q }, [])
  const cargar = useCallback(async () => { try { setDatos((await api.get<Pagina<Auditoria>>(`/auditoria?${params(aplicados, pagina)}`)).data); setError('') } catch (e) { setError(errorTexto(e)) } }, [aplicados, pagina, params])
  useEffect(() => { void cargar() }, [cargar])
  function buscar(e: FormEvent) { e.preventDefault(); setPagina(0); setAplicados({ ...filtros }) }
  async function exportar() { try { const r = await api.get(`/auditoria?${params(aplicados, 0)}&formato=csv`, { responseType: 'blob' }); const url = URL.createObjectURL(r.data); const a = document.createElement('a'); a.href = url; a.download = 'auditoria.csv'; a.click(); URL.revokeObjectURL(url) } catch (e) { setError(errorTexto(e)) } }
  return <><Titulo eyebrow="Trazabilidad" titulo="Auditoría de accesos" descripcion="Cada decisión deja su resultado, capa y motivo. Los filtros respetan el alcance de tu rol." action={<button className="btn-ghost" onClick={exportar}><Download size={16} /> Exportar CSV</button>} />
    {error && <p role="alert" className="mb-4 rounded-lg bg-red-50 p-3 text-sm text-red-700">{error}</p>}
    <form onSubmit={buscar} className="card mb-5 grid gap-3 p-4 sm:grid-cols-2 xl:grid-cols-4">{(['usuario','recurso','accion','resultado'] as const).map(k => <label key={k}><span className="label">{k}</span><input className="field" value={filtros[k]} onChange={e => setFiltros({ ...filtros, [k]: e.target.value })} /></label>)}{(['desde','hasta'] as const).map(k => <label key={k}><span className="label">{k}</span><input type="datetime-local" className="field" value={filtros[k]} onChange={e => setFiltros({ ...filtros, [k]: e.target.value })} /></label>)}<div className="flex items-end"><button className="btn-primary"><Search size={16} /> Filtrar</button></div></form>
    {!datos || datos.content.length === 0 ? <Vacio texto="No hay eventos de auditoría para estos filtros." /> : <div className="card overflow-x-auto"><table className="w-full text-left text-sm"><thead className="bg-slate-50 text-xs uppercase tracking-wide text-slate-500"><tr>{['Fecha','Usuario','Operación','Recurso','Resultado','Capa / motivo'].map(h => <th key={h} className="px-4 py-4">{h}</th>)}</tr></thead><tbody>{datos.content.map(a => <tr key={a.id} className="border-t border-slate-100 align-top"><td className="whitespace-nowrap px-4 py-4 text-slate-500">{a.fechaHora?.replace('T',' ').slice(0,19)}</td><td className="px-4 py-4"><b>{a.usuario}</b><p className="text-xs text-slate-500">{a.usuarioRol}</p></td><td className="px-4 py-4">{a.accion}</td><td className="px-4 py-4">{a.recurso}</td><td className="px-4 py-4"><span className={`badge ${a.resultado === 'PERMITIDO' ? 'bg-emerald-50 text-emerald-700' : 'bg-red-50 text-red-700'}`}>{a.resultado}</span></td><td className="max-w-sm px-4 py-4"><b>{a.capa}</b><p className="text-xs text-slate-500">{a.motivo}</p>{a.politicasEvaluadas?.filter(p => p.resultado === 'DENEGADO').map(p => <span key={p.codigo} className="mr-1 text-xs text-red-700">{p.codigo}</span>)}</td></tr>)}</tbody></table></div>}
    {datos && <div className="mt-4 flex items-center justify-between text-sm text-slate-500"><span>{datos.totalElements} eventos · página {datos.number + 1} de {Math.max(1, datos.totalPages)}</span><div className="flex gap-2"><button className="btn-ghost" disabled={pagina === 0} onClick={() => setPagina(pagina - 1)}>Anterior</button><button className="btn-ghost" disabled={pagina + 1 >= datos.totalPages} onClick={() => setPagina(pagina + 1)}>Siguiente</button></div></div>}
  </>
}
