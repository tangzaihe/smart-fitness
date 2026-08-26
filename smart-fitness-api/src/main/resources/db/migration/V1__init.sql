-- Smart Fitness P0 schema (PostgreSQL 16)
-- Frozen sources: 02 / 04 / 07 / 08. Snowflake IDs from application.

CREATE OR REPLACE FUNCTION trg_set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- ---------- identity ----------
CREATE TABLE app_user (
    id              BIGINT PRIMARY KEY,
    email           VARCHAR(128) NOT NULL,
    phone           VARCHAR(32),
    password_hash   VARCHAR(255) NOT NULL,
    status          VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uk_app_user_email UNIQUE (email)
);
CREATE UNIQUE INDEX uk_app_user_phone ON app_user (phone) WHERE phone IS NOT NULL;
CREATE TRIGGER tg_app_user_updated BEFORE UPDATE ON app_user
    FOR EACH ROW EXECUTE FUNCTION trg_set_updated_at();

CREATE TABLE athlete (
    id              BIGINT PRIMARY KEY,
    user_id         BIGINT NOT NULL,
    display_name    VARCHAR(64) NOT NULL,
    sex             VARCHAR(16),
    birth_date      DATE,
    height_cm       NUMERIC(5,1),
    goal            VARCHAR(32) NOT NULL,
    weekly_min      INTEGER,
    equipment       JSONB NOT NULL DEFAULT '[]',
    preferences     JSONB NOT NULL DEFAULT '{}',
    onboarded_at    TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uk_athlete_user UNIQUE (user_id)
);
CREATE TRIGGER tg_athlete_updated BEFORE UPDATE ON athlete
    FOR EACH ROW EXECUTE FUNCTION trg_set_updated_at();

CREATE TABLE athlete_constraint (
    id              BIGINT PRIMARY KEY,
    athlete_id      BIGINT NOT NULL,
    type            VARCHAR(32) NOT NULL,
    body_part       VARCHAR(32),
    severity        SMALLINT,
    starts_on       DATE,
    ends_on         DATE,
    notes           TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_constraint_athlete ON athlete_constraint (athlete_id);

CREATE TABLE wellness_log (
    id                  BIGINT PRIMARY KEY,
    athlete_id          BIGINT NOT NULL,
    log_date            DATE NOT NULL,
    sleep_hours         NUMERIC(4,1),
    subjective_fatigue  SMALLINT,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uk_wellness_athlete_date UNIQUE (athlete_id, log_date)
);

CREATE TABLE athletic_state (
    id                  BIGINT PRIMARY KEY,
    athlete_id          BIGINT NOT NULL,
    as_of               TIMESTAMPTZ NOT NULL,
    readiness           SMALLINT NOT NULL,
    recovery            SMALLINT,
    fatigue_by_muscle   JSONB NOT NULL DEFAULT '{}',
    sleep_hours         NUMERIC(4,1),
    rhr                 SMALLINT,
    hrv                 NUMERIC(6,1),
    source              VARCHAR(16) NOT NULL,
    calc_version        VARCHAR(32) NOT NULL,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_state_athlete_asof ON athletic_state (athlete_id, as_of DESC);

-- ---------- catalog / training ----------
CREATE TABLE exercise_catalog (
    id              BIGINT PRIMARY KEY,
    code            VARCHAR(64) NOT NULL,
    name            VARCHAR(128) NOT NULL,
    muscle_group    VARCHAR(32) NOT NULL,
    equipment       VARCHAR(32),
    pattern         VARCHAR(32),
    swap_group      VARCHAR(64),
    tags            JSONB NOT NULL DEFAULT '[]',
    is_stretch      BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT uk_exercise_code UNIQUE (code)
);

CREATE TABLE session_log (
    id                  BIGINT PRIMARY KEY,
    athlete_id          BIGINT NOT NULL,
    advice_id           BIGINT,
    status              VARCHAR(16) NOT NULL,
    started_at          TIMESTAMPTZ,
    ended_at            TIMESTAMPTZ,
    perceived_exertion  SMALLINT,
    notes               TEXT,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_session_athlete ON session_log (athlete_id, started_at DESC);
CREATE INDEX idx_session_athlete_status ON session_log (athlete_id, status);

CREATE TABLE set_log (
    id              BIGINT PRIMARY KEY,
    session_id      BIGINT NOT NULL,
    exercise_code   VARCHAR(64) NOT NULL,
    muscle_group    VARCHAR(32),
    set_index       SMALLINT NOT NULL,
    reps            INTEGER,
    load_kg         NUMERIC(6,2),
    rpe             NUMERIC(3,1),
    completed       BOOLEAN NOT NULL DEFAULT FALSE
);
CREATE INDEX idx_set_session ON set_log (session_id);

-- ---------- coach ----------
CREATE TABLE coach_policy (
    id              BIGINT PRIMARY KEY,
    name            VARCHAR(64) NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uk_coach_policy_name UNIQUE (name)
);

CREATE TABLE coach_policy_version (
    id              BIGINT PRIMARY KEY,
    policy_id       BIGINT NOT NULL,
    version         INTEGER NOT NULL,
    system_prompt   TEXT NOT NULL,
    tool_flags      JSONB NOT NULL DEFAULT '{}',
    guardrails      JSONB NOT NULL DEFAULT '{}',
    status          VARCHAR(16) NOT NULL,
    gray_percent    SMALLINT NOT NULL DEFAULT 0,
    created_by      BIGINT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uk_policy_version UNIQUE (policy_id, version)
);

CREATE TABLE coach_run (
    id                  BIGINT PRIMARY KEY,
    athlete_id          BIGINT NOT NULL,
    policy_version_id   BIGINT NOT NULL,
    status              VARCHAR(16) NOT NULL,
    trigger             VARCHAR(32) NOT NULL,
    model               VARCHAR(64),
    key_source          VARCHAR(16),
    token_in            INTEGER,
    token_out           INTEGER,
    started_at          TIMESTAMPTZ NOT NULL,
    ended_at            TIMESTAMPTZ
);
CREATE INDEX idx_run_athlete ON coach_run (athlete_id, started_at DESC);
CREATE INDEX idx_run_athlete_status ON coach_run (athlete_id, status);

CREATE TABLE coach_event (
    id              BIGINT PRIMARY KEY,
    run_id          BIGINT NOT NULL,
    seq             INTEGER NOT NULL,
    type            VARCHAR(32) NOT NULL,
    payload         JSONB NOT NULL DEFAULT '{}',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uk_coach_event_run_seq UNIQUE (run_id, seq)
);

CREATE TABLE advice (
    id                  BIGINT PRIMARY KEY,
    run_id              BIGINT NOT NULL,
    athlete_id          BIGINT NOT NULL,
    type                VARCHAR(16) NOT NULL,
    payload             JSONB NOT NULL,
    evidence            JSONB NOT NULL DEFAULT '{}',
    risk_level          VARCHAR(8) NOT NULL,
    confirm_required    BOOLEAN NOT NULL DEFAULT TRUE,
    status              VARCHAR(16) NOT NULL,
    decided_at          TIMESTAMPTZ,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_advice_athlete ON advice (athlete_id, created_at DESC);
CREATE INDEX idx_advice_run ON advice (run_id);

CREATE TABLE decision_event (
    id              BIGINT PRIMARY KEY,
    advice_id       BIGINT NOT NULL,
    athlete_id      BIGINT NOT NULL,
    action          VARCHAR(16) NOT NULL,
    note            TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ---------- llm ledger (04) ----------
CREATE TABLE llm_credential (
    id              BIGINT PRIMARY KEY,
    athlete_id      BIGINT,
    provider        VARCHAR(32) NOT NULL,
    base_url        VARCHAR(256) NOT NULL,
    model           VARCHAR(64) NOT NULL,
    api_key_cipher  BYTEA NOT NULL,
    key_fingerprint VARCHAR(16) NOT NULL,
    status          VARCHAR(16) NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_cred_athlete ON llm_credential (athlete_id) WHERE athlete_id IS NOT NULL;
CREATE TRIGGER tg_llm_credential_updated BEFORE UPDATE ON llm_credential
    FOR EACH ROW EXECUTE FUNCTION trg_set_updated_at();

CREATE TABLE llm_call_usage (
    id                   BIGINT PRIMARY KEY,
    athlete_id           BIGINT NOT NULL,
    run_id               BIGINT,
    call_seq             INTEGER NOT NULL DEFAULT 1,
    purpose              VARCHAR(32) NOT NULL,
    key_source           VARCHAR(16) NOT NULL,
    billed_to            VARCHAR(16) NOT NULL,
    credential_id        BIGINT,
    key_fingerprint      VARCHAR(16),
    provider             VARCHAR(32) NOT NULL,
    model                VARCHAR(64) NOT NULL,
    prompt_tokens        INTEGER NOT NULL DEFAULT 0,
    completion_tokens    INTEGER NOT NULL DEFAULT 0,
    cached_tokens        INTEGER NOT NULL DEFAULT 0,
    reasoning_tokens     INTEGER NOT NULL DEFAULT 0,
    total_tokens         INTEGER NOT NULL DEFAULT 0,
    usage_source         VARCHAR(16) NOT NULL,
    estimated_cost_minor INTEGER NOT NULL DEFAULT 0,
    currency             CHAR(3) NOT NULL DEFAULT 'CNY',
    duration_ms          INTEGER,
    http_status          INTEGER,
    status               VARCHAR(16) NOT NULL,
    error_code           VARCHAR(64),
    created_at           TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_usage_athlete_time ON llm_call_usage (athlete_id, created_at DESC);
CREATE INDEX idx_usage_run ON llm_call_usage (run_id);
CREATE INDEX idx_usage_day_source ON llm_call_usage (created_at, key_source);

CREATE TABLE llm_usage_daily (
    id                   BIGINT PRIMARY KEY,
    athlete_id           BIGINT NOT NULL,
    usage_date           DATE NOT NULL,
    key_source           VARCHAR(16) NOT NULL,
    call_count           INTEGER NOT NULL DEFAULT 0,
    fail_count           INTEGER NOT NULL DEFAULT 0,
    prompt_tokens        BIGINT NOT NULL DEFAULT 0,
    completion_tokens    BIGINT NOT NULL DEFAULT 0,
    total_tokens         BIGINT NOT NULL DEFAULT 0,
    platform_cost_minor  INTEGER NOT NULL DEFAULT 0,
    byok_est_cost_minor  INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT uk_usage_daily UNIQUE (athlete_id, usage_date, key_source)
);

CREATE TABLE llm_quota (
    id                BIGINT PRIMARY KEY,
    athlete_id        BIGINT,
    period            VARCHAR(16) NOT NULL,
    token_limit       BIGINT,
    cost_limit_minor  INTEGER,
    rpm_limit         INTEGER NOT NULL DEFAULT 20,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX uk_quota_global_period ON llm_quota (period) WHERE athlete_id IS NULL;
CREATE UNIQUE INDEX uk_quota_athlete_period ON llm_quota (athlete_id, period) WHERE athlete_id IS NOT NULL;

CREATE TABLE llm_price_list (
    id                   BIGINT PRIMARY KEY,
    provider             VARCHAR(32) NOT NULL,
    model                VARCHAR(64) NOT NULL,
    input_per_1k_minor   INTEGER NOT NULL,
    output_per_1k_minor  INTEGER NOT NULL,
    cached_per_1k_minor  INTEGER NOT NULL DEFAULT 0,
    currency             CHAR(3) NOT NULL DEFAULT 'CNY',
    effective_from       DATE NOT NULL,
    CONSTRAINT uk_price UNIQUE (provider, model, effective_from)
);

-- ---------- ops / p2 placeholders ----------
CREATE TABLE device_sample (
    id              BIGINT PRIMARY KEY,
    athlete_id      BIGINT NOT NULL,
    type            VARCHAR(32) NOT NULL,
    value           NUMERIC(10,3) NOT NULL,
    captured_at     TIMESTAMPTZ NOT NULL,
    source          VARCHAR(32) NOT NULL
);
CREATE INDEX idx_device_athlete ON device_sample (athlete_id, captured_at DESC);

CREATE TABLE admin_user (
    id              BIGINT PRIMARY KEY,
    email           VARCHAR(128) NOT NULL,
    role            VARCHAR(32) NOT NULL,
    status          VARCHAR(16) NOT NULL,
    password_hash   VARCHAR(255) NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uk_admin_user_email UNIQUE (email)
);

COMMENT ON TABLE app_user IS 'App login identity, 1:1 with athlete';
COMMENT ON TABLE athlete IS 'Athlete profile; onboarded_at null until first PUT /v1/athlete/me';
COMMENT ON TABLE wellness_log IS 'Daily sleep/fatigue inputs for readiness v1';
COMMENT ON TABLE athletic_state IS 'Readiness snapshots; insert only, never update history';
COMMENT ON TABLE llm_call_usage IS 'One row per real LLM HTTP call';
COMMENT ON TABLE advice IS 'Coach prescription pending user confirm';
