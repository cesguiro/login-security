# login-security

API REST didáctica de autenticación con **JWT de acceso** (HS256 con **JJWT**) y **refresh token opaco**, construida sobre **Spring Security**. Evolución de `login-jwt`: mismo flujo de tokens, pero la autenticación se publica en el `SecurityContextHolder` y la autorización se declara con `@PreAuthorize` en los controladores. Detalle funcional, endpoints y ejemplos de uso en `README.md`.

## Rol didáctico

- **DWS**: tercera técnica de la serie de logins — se delega en el framework de seguridad sin perder de vista el flujo emitido manualmente en los proyectos anteriores.
- Conserva los errores 401/403 en el formato `ApiErrorResponse` para que la API no cambie aunque el mecanismo cambie.

## Tecnologías

- Java 25 · Spring Boot 4.1.1 (Web MVC, JDBC, Validation, **Security**)
- MariaDB + Flyway: usuarios y `refresh_tokens` (el access token no se almacena)
- `SecurityFilterChain` stateless · `@EnableMethodSecurity` · `BCryptPasswordEncoder`
- JJWT 0.12.6 para la firma y validación del access token
- Testcontainers (MariaDB real en los tests de integración)
- Maven Wrapper

## Arquitectura

`controller` / `domain` / `persistence` / `auth` / `config`:

- `controller`: endpoints REST, DTOs y manejo de excepciones; la autorización se declara con `@PreAuthorize`.
- `domain`: `AuthService`, `UserService`, `JwtService` y los puertos (interfaces de repositorios).
- `persistence`: DAOs JDBC y mapeadores de filas al dominio.
- `auth`: filtro servlet que valida el JWT `Bearer`, publica un `UsernamePasswordAuthenticationToken` y define los errores 401/403 (`AuthenticationEntryPoint` y `AccessDeniedHandler` con formato `ApiErrorResponse`).
- `config`: `SecurityFilterChain` (rutas públicas: `POST /auth/login` y `GET /demo/public`) y bean `PasswordEncoder`.

## Ejecución y pruebas

- MariaDB local (BD `login_security`, credenciales `root`/`root` en `application.properties`) y `./mvnw spring-boot:run` → `http://localhost:8080`.
- `./mvnw test` — Testcontainers: requiere Docker en ejecución.
- Usuarios sembrados por Flyway (`demo.user@example.com`, contraseña `password`): didácticos, no reutilizar fuera de desarrollo.

## Configuración

- Firma del JWT: `app.jwt.secret`, configurable con la variable de entorno **`JWT_SECRET`** (mínimo 256 bits; el valor por defecto del repositorio es solo de desarrollo); emisor `app.jwt.issuer`.
- Vida de los tokens: access 15 min (`app.auth.access-token-ttl`), refresh 30 días (`app.auth.refresh-token-ttl`).
- `refresh_tokens` soporta revocación (`revoked_at`) y trazabilidad de rotación (`consumed_at`, `replaced_by_token_id`).

## Estándares

- `docs/base-standards.md`: reglas generales para agentes (idiomas, pasos pequeños, …).
- `docs/backend-standards.md`: estándares específicos del proyecto (capas, mapeadores, flujo de tokens con Spring Security).

## Estado

- ✅ Login con emisión de par de tokens (JWT + refresh opaco), validación de JWT integrada con Spring Security, autorización por roles con `@PreAuthorize`, formato de errores 401/403.
- 🚧 Pendiente: endpoint de refresco con rotación y cierre de sesión (revocación). Ver README.

## Repositorio

`git@github.com:cesguiro/login-security.git`
