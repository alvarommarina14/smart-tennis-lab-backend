# Smart Tennis Lab — Backend

API del servicio de análisis de partidos de tenis de Smart Tennis Lab. El profe registra
estadísticas tocando botones mientras mira el partido en vivo, y después obtiene un reporte con
totales, porcentajes y desglose por set.

La app móvil que consume esta API vive en [`smart-tennis-lab-mobile`](../mobile).

## Stack

| | |
|---|---|
| Lenguaje | Java 21 (LTS) |
| Framework | Spring Boot 3.5 |
| Persistencia | PostgreSQL 16 + Spring Data JPA + Flyway |
| Seguridad | Spring Security 6 + JWT (access + refresh) |
| Documentación | springdoc-openapi (Swagger UI) |
| Tests | JUnit 5, Mockito, Testcontainers |
| Build | Gradle 9 (toolchain Java 21) |

## Decisiones de diseño

**Un tap = una fila.** `match_events` es un log append-only: cada botón que toca el profe inserta
una fila y deshacer marca `deleted_at` en vez de borrar. Eso da tres cosas de una: undo trivial,
sincronización idempotente y auditoría de lo que pasó en la cancha. No es Event Sourcing — no hay
event store ni reconstrucción de agregados, solo el modelo de datos que el dominio pide.

**IDs generados por el cliente.** Los partidos, sets y eventos usan UUID creado en el dispositivo.
En una cancha la señal es mala, así que el celular tiene que poder crear todo offline. El id del
cliente funciona además como clave de idempotencia: reenviar un lote no duplica nada.

**Los totales se calculan, no se guardan.** Un partido son cientos de eventos: un `GROUP BY` sobre
el índice parcial alcanza y sobra. Si algún día no alcanzara, se agrega una tabla de contadores
denormalizada — pero recién cuando haga falta.

**El catálogo de KPIs vive en código** (`com.smarttennislab.catalog.Kpi`), pero la app no lo
replica: lo pide por `GET /api/v1/kpis` y arma la pantalla con lo que recibe. Así, agregar los KPIs
de dobles es agregar constantes al enum y redeployar el backend, sin publicar una versión nueva de
la app en la store.

**Aislamiento entre profes.** Todas las consultas filtran por el `coach_id` que sale del JWT. Un
profe no puede ver ni tocar los datos de otro, y hay tests que lo verifican.

## KPIs

23 KPIs sobre la lista base de singles: **19 contadores** que el profe toca (saque, devolución,
definición del punto, largo del rally, resultado) y **4 calculados** (puntos ganados, puntos
jugados, duración y % de puntos ganados).

Quedan fuera de esta versión `% de juegos ganados` y `% de break points convertidos y salvados`:
no son calculables sin eventos base que la lista todavía no define.

## Cómo levantarlo

El JDK 21 lo descarga Gradle solo, no hace falta instalarlo.

Para la base hay dos caminos; con cualquiera de los dos la API queda igual.

**Con Docker:**

```bash
docker compose up -d          # Postgres 16 en localhost:5432
```

**Con un PostgreSQL ya instalado en la máquina:**

```bash
psql -U postgres -f scripts/create-local-db.sql   # crea la base y el usuario stl
```

Después, en ambos casos:

```bash
./gradlew bootRun             # API en http://localhost:8080
```

Swagger UI queda en http://localhost:8080/swagger-ui.html

## Tests

```bash
./gradlew test                # unitarios + integración
./gradlew build               # lo anterior + el jar
```

Los tests de integración levantan su propio Postgres con **Testcontainers**, así que **esos**
sí necesitan Docker. Es a propósito: un test que depende de la base de tu máquina deja de ser
reproducible y no corre igual en CI. Sin Docker instalado, los unitarios corren igual y los de
integración corren en GitHub Actions, cuyos runners ya traen Docker.

## Configuración

Todo tiene default para desarrollo local; en producción se pasa por variable de entorno.

| Variable | Default | |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/smarttennislab` | |
| `DB_USER` / `DB_PASSWORD` | `stl` / `stl` | |
| `JWT_SECRET` | valor de desarrollo | **obligatorio en producción** |
| `CORS_ALLOWED_ORIGINS` | orígenes de Expo en local | |
| `PORT` | `8080` | |

## Estructura

Un paquete por feature y, dentro de cada uno, un subpaquete por capa
(`controller` → `service` → `repository` → `model`, más `dto`). Las dependencias van siempre en esa
dirección: la capa web no conoce JPA.

```
com.smarttennislab
├── config/     seguridad, CORS, filtro JWT
├── shared/     manejo de errores y tipos compartidos
├── auth/       registro, login, refresh, JWT
├── catalog/    enum de KPIs y su endpoint
├── player/     alumnos del profe
├── match/      partidos, sets y sincronización de eventos
└── report/     cálculo de KPIs derivados y export PDF/CSV
```

El cálculo de los KPIs vive en `report.service.KpiCalculator`, que es una clase pura: recibe cuántas
veces se tocó cada botón y cuánto duró el partido, y devuelve los 23 valores. No sabe de JPA ni de
HTTP, así que se testea sin levantar nada.
