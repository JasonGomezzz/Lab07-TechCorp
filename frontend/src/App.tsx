import { useEffect, useState, type FormEvent, type ReactNode } from 'react'
import { Link, NavLink, Navigate, Route, Routes, useNavigate } from 'react-router'
import { Activity, BookOpenCheck, ClipboardList, FileText, LogOut, Menu, Settings2, ShieldCheck, Users } from 'lucide-react'
import { api, demo, entornoNormal, errorTexto } from './api'
import type { Simulacion, Usuario } from './types'
import { Documentos } from './Documentos'
import { Usuarios } from './Usuarios'
import { Politicas } from './Politicas'
import { AuditoriaPagina } from './AuditoriaPagina'
import { CasosPagina } from './CasosPagina'

function App() {
  const [usuario, setUsuario] = useState<Usuario | null>(null)
  const [cargando, setCargando] = useState(true)
  const [error, setError] = useState('')
  const [menu, setMenu] = useState(false)
  const [simulacion, setSimulacion] = useState<Simulacion>(() => JSON.parse(sessionStorage.getItem('securedocs_sim') || 'null') || entornoNormal)
  const navigate = useNavigate()

  useEffect(() => {
    if (!sessionStorage.getItem('securedocs_token')) { setCargando(false); return }
    api.get<Usuario>('/auth/me').then(r => setUsuario(r.data)).catch(() => sessionStorage.removeItem('securedocs_token')).finally(() => setCargando(false))
  }, [])

  async function ingresar(username: string, password: string) {
    setError('')
    try {
      const respuesta = await api.post<{ token: string }>('/auth/login', { username, password })
      sessionStorage.setItem('securedocs_token', respuesta.data.token)
      const perfil = await api.get<Usuario>('/auth/me')
      setUsuario(perfil.data)
      navigate('/documentos')
    } catch (e) { sessionStorage.removeItem('securedocs_token'); setError(errorTexto(e)) }
  }

  async function salir() {
    try { await api.post('/auth/logout') } finally {
      sessionStorage.removeItem('securedocs_token')
      setUsuario(null)
      navigate('/login')
    }
  }

  function actualizarSimulacion(sim: Simulacion) {
    setSimulacion(sim)
    sessionStorage.setItem('securedocs_sim', JSON.stringify(sim))
  }

  if (cargando) return <div className="grid min-h-screen place-items-center text-slate-500">Cargando SecureDocs…</div>
  if (!usuario) return <Routes><Route path="*" element={<Login onLogin={ingresar} error={error} />} /></Routes>

  const permisos = new Set(usuario.permisos || [])
  const acceso = (permiso: string) => permisos.has(permiso)
  const navegacion = [
    { to: '/documentos', text: 'Documentos', icon: FileText, visible: true },
    { to: '/usuarios', text: 'Usuarios', icon: Users, visible: acceso('GESTIONAR_USUARIOS') },
    { to: '/politicas', text: 'Políticas', icon: Settings2, visible: acceso('GESTIONAR_CONFIGURACION') },
    { to: '/auditoria', text: 'Auditoría', icon: Activity, visible: acceso('VER_AUDITORIA') },
    { to: '/casos', text: 'Panel de casos', icon: BookOpenCheck, visible: demo },
  ]

  return <div className="min-h-screen lg:flex">
    <aside className={`${menu ? 'block' : 'hidden'} border-r border-slate-200 bg-white lg:block lg:w-64 lg:shrink-0`}>
      <div className="flex items-center gap-3 border-b border-slate-100 px-6 py-6">
        <div className="rounded-xl bg-[#145a72] p-2 text-white"><ShieldCheck size={24} /></div>
        <div><p className="text-lg font-bold leading-tight">SecureDocs</p><p className="text-xs text-slate-500">TechCorp · Lab 06</p></div>
      </div>
      <nav className="space-y-1 p-3" aria-label="Principal">
        {navegacion.filter(item => item.visible).map(item => <NavLink key={item.to} to={item.to} onClick={() => setMenu(false)} className={({ isActive }) => `flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium ${isActive ? 'bg-cyan-50 text-[#145a72]' : 'text-slate-600 hover:bg-slate-50'}`}><item.icon size={18} />{item.text}</NavLink>)}
      </nav>
      <div className="m-3 mt-6 rounded-xl bg-slate-50 p-4 text-sm">
        <p className="font-semibold">{usuario.nombre}</p><p className="mt-1 text-slate-500">{usuario.rol} · {usuario.departamento}</p>
        <button className="mt-4 flex items-center gap-2 text-red-700 hover:underline" onClick={salir}><LogOut size={16} /> Cerrar sesión</button>
      </div>
    </aside>
    <div className="min-w-0 flex-1">
      <header className="flex items-center justify-between border-b border-slate-200 bg-white px-5 py-4 lg:px-9">
        <button className="lg:hidden" onClick={() => setMenu(!menu)} aria-label="Abrir menú"><Menu /></button>
        <div className="hidden text-sm text-slate-500 lg:block">Laboratorio de seguridad en la nube <span className="mx-2">/</span> RBAC + ABAC</div>
        <div className="flex items-center gap-2"><span className="rounded-full bg-emerald-50 px-3 py-1 text-xs font-semibold text-emerald-700">Sesión activa</span>{demo && <span className="rounded-full bg-amber-50 px-3 py-1 text-xs font-semibold text-amber-700">DEMO</span>}</div>
      </header>
      <main className="mx-auto max-w-7xl p-5 lg:p-9">
        {demo && <Simulador value={simulacion} onChange={actualizarSimulacion} />}
        <Routes>
          <Route path="/documentos" element={<Documentos usuario={usuario} acceso={acceso} />} />
          <Route path="/usuarios" element={acceso('GESTIONAR_USUARIOS') ? <Usuarios /> : <SinPermiso />} />
          <Route path="/politicas" element={acceso('GESTIONAR_CONFIGURACION') ? <Politicas puedeEditar={acceso('GESTIONAR_CONFIGURACION')} /> : <SinPermiso />} />
          <Route path="/auditoria" element={acceso('VER_AUDITORIA') ? <AuditoriaPagina /> : <SinPermiso />} />
          <Route path="/casos" element={demo ? <CasosPagina /> : <SinPermiso />} />
          <Route path="*" element={<Navigate to="/documentos" replace />} />
        </Routes>
      </main>
    </div>
  </div>
}

function Login({ onLogin, error }: { onLogin: (u: string, p: string) => Promise<void>; error: string }) {
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [ocupado, setOcupado] = useState(false)
  async function enviar(e: FormEvent) { e.preventDefault(); setOcupado(true); await onLogin(username, password); setOcupado(false) }
  return <div className="grid min-h-screen lg:grid-cols-2">
    <div className="relative hidden overflow-hidden bg-[#0d3443] p-12 text-white lg:flex lg:flex-col lg:justify-between">
      <div className="absolute -right-24 -top-24 h-96 w-96 rounded-full border border-white/10" /><div className="absolute bottom-0 left-16 h-80 w-80 rounded-full bg-cyan-400/10 blur-3xl" />
      <div className="relative flex items-center gap-3"><ShieldCheck size={30} /><span className="text-xl font-bold">SecureDocs</span></div>
      <div className="relative max-w-lg"><p className="mb-4 text-xs font-bold uppercase tracking-[.2em] text-cyan-300">TechCorp · Laboratorio 06</p><h1 className="text-5xl font-semibold leading-tight">Cada acceso tiene una razón.</h1><p className="mt-6 text-lg leading-relaxed text-slate-300">Documentos protegidos por permisos de rol, políticas de contexto y un registro de auditoría verificable.</p><div className="mt-10 flex gap-3 text-sm text-cyan-200"><span className="rounded-full border border-cyan-200/30 px-4 py-2">RBAC</span><span className="rounded-full border border-cyan-200/30 px-4 py-2">ABAC</span><span className="rounded-full border border-cyan-200/30 px-4 py-2">Auditoría</span></div></div>
      <p className="relative text-sm text-slate-400">Proyecto académico · Seguridad en la nube</p>
    </div>
    <div className="flex items-center justify-center p-6"><div className="w-full max-w-md"><div className="mb-8 flex items-center gap-3 lg:hidden"><ShieldCheck className="text-[#145a72]" /><b>SecureDocs</b></div><p className="text-sm font-semibold uppercase tracking-widest text-[#145a72]">Acceso seguro</p><h2 className="mt-2 text-3xl font-bold">Inicia sesión</h2><p className="mt-3 text-slate-500">Usa una cuenta habilitada del laboratorio.</p>
      <form onSubmit={enviar} className="mt-9 space-y-5"><div><label className="label" htmlFor="username">Usuario</label><input id="username" className="field" autoComplete="username" required value={username} onChange={e => setUsername(e.target.value)} placeholder="usuario.apellido" /></div><div><label className="label" htmlFor="password">Contraseña</label><input id="password" type="password" className="field" autoComplete="current-password" required value={password} onChange={e => setPassword(e.target.value)} /></div>{error && <p role="alert" className="rounded-lg bg-red-50 p-3 text-sm text-red-700">{error}</p>}<button className="btn-primary w-full py-3" disabled={ocupado}>{ocupado ? 'Ingresando…' : 'Entrar a SecureDocs'}</button></form>
      {demo && <p className="mt-7 rounded-lg bg-amber-50 p-4 text-sm text-amber-800">Perfil demo: las cabeceras de simulación se habilitan tras iniciar sesión. La contraseña de demostración se configura en el servidor.</p>}
    </div></div>
  </div>
}

function Simulador({ value, onChange }: { value: Simulacion; onChange: (s: Simulacion) => void }) {
  const [abierto, setAbierto] = useState(false)
  const campo = (label: string, control: ReactNode) => <label className="block"><span className="label">{label}</span>{control}</label>
  return <section className="mb-7 rounded-xl border border-amber-200 bg-amber-50/70 p-4 text-sm"><div className="flex flex-wrap items-center justify-between gap-3"><div className="flex items-center gap-3"><Settings2 className="text-amber-700" size={20} /><div><b>Simulador de entorno</b><p className="text-xs text-amber-800">Hora {value.hora} · {value.ubicacion} · {value.dispositivo} · IP {value.ip}</p></div></div><button className="btn-ghost" onClick={() => setAbierto(!abierto)}>{abierto ? 'Cerrar' : 'Configurar'}</button></div>
    {abierto && <div className="mt-4 grid gap-3 border-t border-amber-200 pt-4 sm:grid-cols-2 xl:grid-cols-4">{campo('Hora', <input type="time" className="field" value={value.hora} onChange={e => onChange({ ...value, hora: e.target.value })} />)}{campo('Ubicación', <select className="field" value={value.ubicacion} onChange={e => onChange({ ...value, ubicacion: e.target.value })}><option>PERU</option><option>MEXICO</option><option>DESCONOCIDA</option></select>)}{campo('Dispositivo', <select className="field" value={value.dispositivo} onChange={e => onChange({ ...value, dispositivo: e.target.value })}><option>CORPORATIVO</option><option>PERSONAL</option></select>)}{campo('IP', <input className="field" value={value.ip} onChange={e => onChange({ ...value, ip: e.target.value })} />)}<button className="btn-ghost sm:col-span-2 xl:col-span-4" onClick={() => onChange(entornoNormal)}>Restablecer entorno normal</button></div>}
  </section>
}

function SinPermiso() { return <div className="card p-8"><h2 className="text-xl font-bold">Sin permiso para esta sección</h2><p className="mt-2 text-slate-500">La API verifica cada solicitud de manera independiente.</p><Link to="/documentos" className="mt-4 inline-block text-[#145a72] underline">Volver a documentos</Link></div> }
export function Titulo({ eyebrow, titulo, descripcion, action }: { eyebrow: string; titulo: string; descripcion: string; action?: ReactNode }) { return <div className="mb-6 flex flex-wrap items-end justify-between gap-4"><div><p className="mb-2 text-xs font-bold uppercase tracking-[.17em] text-[#145a72]">{eyebrow}</p><h1 className="text-3xl font-bold tracking-tight">{titulo}</h1><p className="mt-2 text-sm text-slate-500">{descripcion}</p></div>{action}</div> }
export function Vacio({ texto }: { texto: string }) { return <div className="card grid min-h-48 place-items-center p-6 text-center text-sm text-slate-500"><div><ClipboardList className="mx-auto mb-3 text-slate-400" />{texto}</div></div> }
export default App
