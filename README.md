# SecureDocs · TechCorp — Balanceo de carga

Proyecto del Laboratorio 07 (Diseñar un entorno) de Desarrollo de Soluciones en la Nube. Parte de la aplicación SecureDocs del Laboratorio 06 (login + CRUD de documentos con RBAC y ABAC) y la ejecuta como un **monolito** en tres réplicas detrás de un balanceador.

- **Parte A (local):** la misma aplicación en los puertos 8081, 8082 y 8083 con Docker, y Nginx con round robin en el puerto 80.
- **Parte B (AWS):** Application Load Balancer con 2 instancias EC2 en dos zonas de disponibilidad.

## Arquitectura

```
Cliente ──► Nginx :80 (round robin) ──► app1 :8081 ┐
                                        app2 :8082 ├──► MySQL (Mac, :3306)
                                        app3 :8083 ┘
```

Cada contenedor es el mismo monolito: el jar de Spring Boot sirve la interfaz React en `/` y la API en `/api`. Son solo 3 contenedores; Nginx y MySQL se ejecutan en el equipo, fuera de Docker. El estado es compartido (base de datos y JWT sin estado), por eso cualquier réplica atiende cualquier petición.

Cada respuesta incluye la cabecera `X-Served-By` con el nombre de la réplica, y `GET /api/instancia` devuelve su nombre y host.

## Requisitos

- Git y Docker con Docker Compose.
- MySQL 8 o superior y Nginx en el equipo (en macOS: `brew install mysql nginx`).
- Puertos libres: 80, 3306, 8081, 8082 y 8083.

No hace falta instalar Java ni Node.js: Docker compila la interfaz y el backend.

## Instalación y ejecución

1. Clona el repositorio:

   ```bash
   git clone https://github.com/JasonGomezzz/Lab07-TechCorp.git
   cd Lab07-TechCorp
   ```

2. Inicia MySQL con `log_bin_trust_function_creators=1` (Flyway crea triggers). Con Homebrew, añade esto a `/opt/homebrew/etc/my.cnf` y arranca el servicio:

   ```ini
   [mysqld]
   bind-address = 127.0.0.1
   log_bin_trust_function_creators = 1
   ```

   ```bash
   brew services start mysql
   ```

3. Crea la base de datos y el usuario de la aplicación (cambia la contraseña):

   ```bash
   mysql -uroot -e "CREATE DATABASE securedocs CHARACTER SET utf8mb4;
   CREATE USER 'securedocs'@'%' IDENTIFIED BY 'CAMBIAR_PASSWORD_BD';
   GRANT ALL PRIVILEGES ON securedocs.* TO 'securedocs'@'%';"
   ```

4. Crea el archivo local de configuración y reemplaza los valores `CAMBIAR` (`DB_PASSWORD` igual que en el paso anterior y `JWT_SECRET` de al menos 32 bytes). Git ignora `.env`:

   ```bash
   cp .env.example .env
   ```

5. Construye e inicia las tres réplicas. Levanta `app1` primero para que Flyway cree las tablas:

   ```bash
   docker compose up --build -d app1
   docker compose up -d
   docker ps
   ```

   Espera a que los tres contenedores aparezcan como `healthy`. Cada uno responde en [8081](http://localhost:8081), [8082](http://localhost:8082) y [8083](http://localhost:8083).

6. Configura Nginx con la copia del repositorio y arráncalo:

   ```bash
   cp nginx/nginx.conf /opt/homebrew/etc/nginx/nginx.conf
   nginx -t && brew services start nginx
   ```

7. Abre [http://localhost](http://localhost) e inicia sesión con `admin`, `laura.mendez`, `ana.torres`, `diego.salas`, `sofia.paredes` o `invitado.externo` y la contraseña definida en `SEED_PASSWORD`.

## Comprobaciones del balanceo

Rotación round robin (debe alternar app1, app2 y app3):

```bash
for i in $(seq 1 9); do curl -si localhost/api/instancia | grep -i x-served-by; done
```

Tolerancia a fallos: detén una réplica y repite el bucle; Nginx sigue respondiendo con las otras dos.

```bash
docker stop app2
for i in $(seq 1 6); do curl -si localhost/api/instancia | grep -i x-served-by; done
docker start app2
```

Para detener todo: `docker compose down`.

## Parte B: AWS

Región us-east-1, VPC `10.0.0.0/16` con dos subredes públicas (`10.0.1.0/24` en us-east-1a y `10.0.2.0/24` en us-east-1b), dos instancias EC2 `t2.micro` con Apache que muestran su `instance-id` y su zona, y un Application Load Balancer (HTTP:80) con health checks sobre `/`. Los pasos y las capturas están en el informe. Al terminar se eliminan el ALB, el target group, las EC2 y la red para no generar costos.

## Desarrollo y comprobaciones

El servidor está en `backend/` (Java 21, Spring Boot, MySQL y Flyway) y la interfaz en `frontend/` (React, TypeScript y Vite). Las pruebas del backend usan Testcontainers, así que requieren Docker:

```bash
(cd backend && ./mvnw test)
```

Para desarrollar la interfaz, con el backend en `localhost:8080`, usa `npm run dev` dentro de `frontend/`; Vite envía `/api` al backend.

Repositorio: [JasonGomezzz/Lab07-TechCorp](https://github.com/JasonGomezzz/Lab07-TechCorp).
