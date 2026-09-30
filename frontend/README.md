# Interfaz de SecureDocs

React, TypeScript, Vite y Tailwind. La interfaz consume la API a través de `/api`, servido por el proxy de Vite en desarrollo o Nginx en Docker.

## Desarrollo local

1. Copia `.env.example` a `.env.local` y ajusta `VITE_DEMO` si corresponde.
2. Inicia el backend en `localhost:8080`.
3. Ejecuta `npm ci` y `npm run dev`.

`npm run build` comprueba TypeScript y genera el paquete. `npm run lint` analiza el código. En Docker, el servicio se publica en `http://localhost:3000`.

El simulador se compila solo cuando `VITE_DEMO=true` y sus cabeceras solo son aceptadas por el backend con perfil `demo`. El panel de casos solicita la contraseña demo en memoria y no la guarda. La sesión normal conserva el JWT en `sessionStorage`; se borra al cerrar sesión o la pestaña. Nunca se debe usar el perfil demo en producción.
