-- Smart Tennis Lab — esquema inicial
--
-- Nota sobre los IDs: matches, match_sets y match_events usan UUID generado por el CLIENTE.
-- El profe puede empezar un partido sin señal en la cancha, así que el dispositivo tiene que poder
-- crear esas entidades offline y sincronizarlas después. El id del cliente es a la vez la clave de
-- idempotencia: reenviar el mismo lote no duplica nada.

-- ---------------------------------------------------------------------------
-- Usuarios (profes) y autenticación
-- ---------------------------------------------------------------------------

CREATE TABLE users (
    id            UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    email         VARCHAR(255) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    full_name     VARCHAR(120) NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- El email se guarda siempre en minúsculas; el índice único lo garantiza sin distinguir mayúsculas.
CREATE UNIQUE INDEX ux_users_email ON users (lower(email));

CREATE TABLE refresh_tokens (
    id         UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token_hash VARCHAR(64) NOT NULL UNIQUE,  -- SHA-256 en hex: el token en claro nunca se persiste
    expires_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX ix_refresh_tokens_user ON refresh_tokens (user_id);

-- ---------------------------------------------------------------------------
-- Alumnos
-- ---------------------------------------------------------------------------

CREATE TABLE players (
    id            UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    coach_id      UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    first_name    VARCHAR(80) NOT NULL,
    last_name     VARCHAR(80) NOT NULL,
    birth_date    DATE,
    dominant_hand VARCHAR(10),  -- RIGHT | LEFT
    notes         TEXT,
    archived_at   TIMESTAMPTZ,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT ck_players_dominant_hand CHECK (dominant_hand IN ('RIGHT', 'LEFT'))
);

CREATE INDEX ix_players_coach_active ON players (coach_id) WHERE archived_at IS NULL;

-- ---------------------------------------------------------------------------
-- Partidos
-- ---------------------------------------------------------------------------

CREATE TABLE matches (
    id            UUID         PRIMARY KEY,  -- generado por el cliente
    coach_id      UUID         NOT NULL REFERENCES users (id)   ON DELETE CASCADE,
    player_id     UUID         NOT NULL REFERENCES players (id) ON DELETE RESTRICT,
    opponent_name VARCHAR(120),
    tournament    VARCHAR(120),
    surface       VARCHAR(20),
    discipline    VARCHAR(10)  NOT NULL DEFAULT 'SINGLES',
    status        VARCHAR(20)  NOT NULL DEFAULT 'IN_PROGRESS',
    started_at    TIMESTAMPTZ  NOT NULL,
    finished_at   TIMESTAMPTZ,
    notes         TEXT,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT ck_matches_surface    CHECK (surface IS NULL OR surface IN ('CLAY', 'HARD', 'GRASS', 'CARPET', 'INDOOR')),
    CONSTRAINT ck_matches_discipline CHECK (discipline IN ('SINGLES', 'DOUBLES')),
    CONSTRAINT ck_matches_status     CHECK (status IN ('IN_PROGRESS', 'FINISHED', 'ABANDONED')),
    CONSTRAINT ck_matches_finished   CHECK (finished_at IS NULL OR finished_at >= started_at)
);

CREATE INDEX ix_matches_coach_started ON matches (coach_id, started_at DESC);
CREATE INDEX ix_matches_player        ON matches (player_id);

CREATE TABLE match_sets (
    id          UUID        PRIMARY KEY,  -- generado por el cliente
    match_id    UUID        NOT NULL REFERENCES matches (id) ON DELETE CASCADE,
    set_number  SMALLINT    NOT NULL,
    started_at  TIMESTAMPTZ NOT NULL,
    finished_at TIMESTAMPTZ,

    CONSTRAINT uq_match_sets_number   UNIQUE (match_id, set_number),
    CONSTRAINT ck_match_sets_number   CHECK (set_number BETWEEN 1 AND 5)
);

CREATE INDEX ix_match_sets_match ON match_sets (match_id);

-- ---------------------------------------------------------------------------
-- Eventos: un tap del profe = una fila
-- ---------------------------------------------------------------------------
--
-- Log append-only. Deshacer un tap NO borra la fila: marca deleted_at. Eso mantiene el sync
-- idempotente (reenviar un undo da el mismo resultado) y deja auditoría de lo que pasó.
--
-- kpi_code guarda el nombre de la constante del enum Kpi. A propósito no hay FK a una tabla de
-- catálogo: el catálogo vive en código. La validación de que el código existe la hace la aplicación.

CREATE TABLE match_events (
    id          UUID        PRIMARY KEY,  -- generado por el cliente = clave de idempotencia
    match_id    UUID        NOT NULL REFERENCES matches (id)    ON DELETE CASCADE,
    set_id      UUID                 REFERENCES match_sets (id) ON DELETE SET NULL,
    kpi_code    VARCHAR(60) NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,  -- reloj del dispositivo, cuándo ocurrió en la cancha
    recorded_at TIMESTAMPTZ NOT NULL DEFAULT now(),  -- reloj del servidor, cuándo llegó
    deleted_at  TIMESTAMPTZ,           -- undo
    client_seq  BIGINT      NOT NULL   -- orden de los taps dentro del dispositivo
);

-- Los reportes siempre agregan por partido sobre eventos vivos.
CREATE INDEX ix_match_events_live      ON match_events (match_id, kpi_code) WHERE deleted_at IS NULL;
CREATE INDEX ix_match_events_set       ON match_events (set_id)             WHERE deleted_at IS NULL;
-- El pull incremental (?since=) recorre por recorded_at.
CREATE INDEX ix_match_events_recorded  ON match_events (match_id, recorded_at);
