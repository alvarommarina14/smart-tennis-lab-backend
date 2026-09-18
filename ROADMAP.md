# Smart Tennis Lab — Roadmap

Cross-project plan for the three repositories. It lives here because this README is already
where the design decisions are documented. Last updated: 2026-09-18.

## Workspace layout

The three repos are sibling folders inside a parent directory that is **not** a git repo:

```
smart-tennis-lab/
├── CLAUDE.md     project instructions for Claude Code (not versioned — see below)
├── backend/      smart-tennis-lab-backend   Java 21 + Spring Boot 3.5
├── mobile/       smart-tennis-lab-mobile    React Native + Expo SDK 54
└── desktop/      smart-tennis-lab-desktop   Electron 34 + React 19 + Vite
```

To recreate it on another machine:

```bash
mkdir smart-tennis-lab && cd smart-tennis-lab
git clone https://github.com/alvarommarina14/smart-tennis-lab-backend.git backend
git clone https://github.com/alvarommarina14/smart-tennis-lab-mobile.git  mobile
git clone https://github.com/alvarommarina14/smart-tennis-lab-desktop.git desktop
```

Then drop the root `CLAUDE.md` next to them (contents reproduced at the end of this file).

## Original plan — done

Phase 0 (scaffolding) plus four phases, all merged to `main` in `backend` and `mobile`:

| Phase | Scope | Status |
|---|---|---|
| 1 — Auth | Backend `shared/` + `auth/`: register, login, refresh, JWT filter in the Spring Security chain. Mobile: login screen, tokens in `expo-secure-store`, `Bearer` + automatic refresh in `api/client.ts`. | done |
| 2 — Players and matches | `players` CRUD, create match, match list. | done |
| 3 — Capture screen | Counter grid, local SQLite, undo via `deleted_at`, idempotent batch sync against `match_events`. | done |
| 4 — Reports | `GROUP BY` over the partial index, the 4 derived KPIs, per-set breakdown, PDF/CSV export. | done |

Added after the plan, also merged:

- Tennis scoreboard (15/30/40, deuce, advantage, games, tiebreak) derived purely from the
  existing `POINT_WON`/`POINT_LOST` taps — `mobile/src/lib/tennisScore.ts`, no new events or columns.
- Match format chosen at creation: `BEST_OF_3_SETS` or `TWO_SETS_SUPER_TIEBREAK`.
- Coach profile editing and logout from the header (mobile).
- `video_offset_ms` on every event, so the desktop app can jump back to the moment in the video.
- CORS for the desktop origin in development.

## Desktop — in progress

Why it exists: the coach's main use case is recording the whole session with fixed cameras at the
club and analysing the video calmly at home. Mobile stays for live matches on court with no signal.

Done in `desktop`:

- Login against the same backend, tokens encrypted by the OS (DPAPI / keychain) in the main process.
- Players and matches screens; create a match and attach a local video file.
- Local video served through a custom protocol (never uploaded).
- Analysis screen: mark the match start inside the recording, configurable lag compensation
  (default ~1.5 s), event draft persisted per match, seek back to any event.

Next, in order:

1. **Report screen.** `src/api/reports.ts` already fetches the report but no screen renders it yet.
   Reuse the backend PDF/CSV export.
2. **Packaging.** `electron-builder` for a Windows installer. macOS later (needs signing/notarising).
3. **H.265 handling.** Club cameras default to H.265+, which Chromium cannot play. Preferred fix is
   configuring the camera to H.264; fallback is transcoding on import with a bundled FFmpeg.

## Backlog

- **Doubles KPIs.** The catalogue is an enum (`com.smarttennislab.catalog.Kpi`) served by
  `GET /api/v1/kpis`, so adding them is backend-only. The KPI list itself is still to be defined.
- **Integration tests with Testcontainers.** The backend README promises them; only unit tests
  exist today.
- **`% games won` and `% break points`.** Left out of v1 because their base events are not defined.
- **Video AI (semi-automatic).** Wanted, not planned yet. Agreed stance: the AI proposes
  (point segmentation, rally length, in/out), the coach confirms. Winner vs unforced error stays
  a human call. Fixed club cameras make this tractable (one homography per court).

## Standing constraints

- Simplest design that solves the domain. No event sourcing, CQRS or queues.
- Backend: no JavaDoc. Mobile/desktop: no comments — rename or extract instead.
- Expo is pinned to SDK 54 to match the Expo Go build on the test iPhone.
- One feature branch per unit of work, merged into `main` with `--no-ff`.

## Root `CLAUDE.md`

Not versioned because the parent folder is not a repo. Copy this verbatim to `smart-tennis-lab/CLAUDE.md`:

````markdown
# Smart Tennis Lab

App de estadísticas de tenis. Tres proyectos hermanos, cada uno con su propio repo git:

- `backend/` — Java 21 + Spring Boot 3.5, PostgreSQL + Flyway, Spring Security + JWT.
- `mobile/` — React Native + Expo SDK 54, expo-router. Partidos en vivo, en la cancha.
- `desktop/` — Electron 34 + React 19 + Vite. Análisis sobre video grabado en el club.

## Estilo de código

**Backend: nada de JavaDoc.** No escribir bloques `/** ... */`. Si algo necesita explicación, va un
comentario `//` corto y solo cuando el porqué no se deduce del código.

**Mobile y desktop: nada de comentarios.** El código del front va sin comentarios. Si una parte no se entiende
sola, la solución es renombrar o extraer una función, no explicarla.

En ambos casos: nombres descriptivos antes que explicaciones.

## Diseño

Vale la restricción de siempre: **el diseño más simple que resuelva el dominio**. Nada de Event
Sourcing, CQRS ni colas. Si una tabla y un `GROUP BY` alcanzan, alcanzan.

## Expo

El proyecto está en **SDK 54**, fijado para que coincida con la versión de Expo Go instalada en el
iPhone de prueba. Antes de escribir código de Expo, consultar la doc de esa versión exacta:
https://docs.expo.dev/versions/v54.0.0/

## Levantar el entorno

```bash
# Base: PostgreSQL 17 nativo en localhost:5432 (no hace falta Docker)
psql -U postgres -f backend/scripts/create-local-db.sql

cd backend && ./gradlew bootRun      # API en :8080
cd mobile  && npx expo start         # Metro en :8081
cd desktop && npm run dev            # Vite en :5173 + ventana de Electron
```

Docker solo se necesita para los tests de integración (Testcontainers).

`mobile/.env` apunta a la IP de la máquina en la red local, no a `localhost`: el celular no ve el
localhost de la PC.
````
