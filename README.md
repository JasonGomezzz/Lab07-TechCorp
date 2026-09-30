# SecureDocs · TechCorp

Proyecto del Laboratorio 06 de Seguridad en la Nube. Permite iniciar sesión, gestionar documentos y evaluar cada acceso mediante roles (RBAC), atributos y contexto (ABAC). Registra las decisiones de autorización en una bitácora de auditoría.

## Requisitos

- Git.
- Docker con Docker Compose (`docker compose`).
- Puertos locales 3000, 8080 y 3307 disponibles.

Docker construye el frontend y el backend; no necesitas instalar Java, Node.js ni MySQL para iniciar la aplicación de esta forma.

## Instalación y ejecución

1. Clona el repositorio y entra en el proyecto:

   ```bash
   git clone https://github.com/JasonGomezzz/Lab06-TechCorp.git
   cd Lab06-TechCorp
   ```

2. Crea el archivo local de configuración:

   ```bash
   cp .env.example .env
   ```

3. Abre `.env` y reemplaza los valores `CAMBIAR` de `DB_PASSWORD`, `DB_ROOT_PASSWORD` y `JWT_SECRET`. El secreto JWT debe tener al menos 32 bytes UTF-8. Define también `SEED_PASSWORD` para las cuentas de prueba. Guarda `.env` solo en tu equipo: Git lo ignora.

4. Construye e inicia los servicios:

   ```bash
   docker compose up --build -d
   docker compose ps
   ```

   Espera a que `mysql` aparezca como saludable y que `backend` y `frontend` estén iniciados.

5. Abre [http://localhost:3000](http://localhost:3000). La API se publica en [http://localhost:8080](http://localhost:8080) y MySQL usa el puerto local 3307.

Para ver los mensajes del backend o detener los servicios:

```bash
docker compose logs -f backend
docker compose down
```

`docker compose down` conserva los datos en el volumen de MySQL. La primera instalación crea las tablas con Flyway y carga los datos de demostración.

## Acceso de demostración

El perfil `demo` se activa en Docker Compose. Inicia sesión con cualquiera de estos usuarios y la contraseña que configuraste en `SEED_PASSWORD`:

| Usuario | Rol |
| --- | --- |
| `admin` | Administrador |
| `laura.mendez` | Gerente |
| `ana.torres` | Supervisor |
| `diego.salas` | Empleado |
| `sofia.paredes` | Auditor |
| `invitado.externo` | Invitado |

La interfaz incluye un panel para ejecutar los casos T1–T17 y consultar el registro de auditoría. En el perfil `demo` también están disponibles [Swagger UI](http://localhost:8080/swagger-ui.html) y la [especificación OpenAPI](http://localhost:8080/v3/api-docs).

## Desarrollo y comprobaciones

El código del servidor está en `backend/` (Java 21, Spring Boot, MySQL y Flyway). La interfaz está en `frontend/` (React, TypeScript y Vite).

Con Docker en ejecución, las pruebas del backend usan Testcontainers con MySQL:

```bash
(cd backend && ./mvnw test)
```

Para comprobar la interfaz necesitas Node.js 22 y npm:

```bash
(cd frontend && npm ci && npm run build && npm run lint)
```

Si ejecutas el frontend fuera de Docker, crea `frontend/.env.local` a partir de `frontend/.env.example` y usa `npm run dev`; Vite enviará `/api` al backend en `localhost:8080`.

## Autorización y auditoría

El backend exige autenticación JWT y evalúa conjuntamente el permiso RBAC y las políticas ABAC antes de permitir una operación. El perfil de demostración permite simular hora, ubicación, dispositivo e IP desde la interfaz para comprobar las reglas. La bitácora guarda las decisiones permitidas y denegadas.

Repositorio: [JasonGomezzz/Lab06-TechCorp](https://github.com/JasonGomezzz/Lab06-TechCorp).
