-- V8: 统一训练计划表 training_plan（plan_source + scope 区分来源与粒度）。
-- 不变量：
--   1) 同一 athlete 仅一条 scope=CYCLE 且 status=ACTIVE
--   2) 同一 athlete + plan_date 仅一条 scope=DAY 且 status IN (PENDING, ACTIVE)

CREATE TABLE training_plan (
    id                  BIGINT PRIMARY KEY,
    athlete_id          BIGINT NOT NULL,
    scope               VARCHAR(16) NOT NULL,
    plan_source         VARCHAR(16) NOT NULL,
    status              VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',

    -- scope=CYCLE
    title               VARCHAR(128),
    goal                VARCHAR(32),
    duration_days       INTEGER,
    payload             JSONB,
    current_day_index   INTEGER,
    started_on          DATE,
    conversation_id     BIGINT,

    -- scope=DAY
    plan_date           DATE,
    label               VARCHAR(64),
    session_template    JSONB,

    parent_id           BIGINT,
    superseded_by       BIGINT,
    resolved_advice_id  BIGINT,

    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_training_plan_athlete ON training_plan (athlete_id, created_at DESC);
CREATE INDEX idx_training_plan_day_date ON training_plan (athlete_id, plan_date)
    WHERE scope = 'DAY';
CREATE INDEX idx_training_plan_parent ON training_plan (parent_id)
    WHERE parent_id IS NOT NULL;
CREATE INDEX idx_training_plan_source ON training_plan (plan_source, status);

CREATE UNIQUE INDEX uq_training_plan_one_active_cycle
    ON training_plan (athlete_id)
    WHERE scope = 'CYCLE' AND status = 'ACTIVE';

CREATE UNIQUE INDEX uq_training_plan_one_active_day
    ON training_plan (athlete_id, plan_date)
    WHERE scope = 'DAY' AND status IN ('PENDING', 'ACTIVE');

CREATE TRIGGER tg_training_plan_updated BEFORE UPDATE ON training_plan
    FOR EACH ROW EXECUTE FUNCTION trg_set_updated_at();

COMMENT ON TABLE training_plan IS '统一训练计划：scope=CYCLE|DAY，plan_source=COACH|USER|AGENT';
COMMENT ON COLUMN training_plan.scope IS 'CYCLE=周期计划 DAY=日计划';
COMMENT ON COLUMN training_plan.plan_source IS 'COACH=教练周期 USER=用户自定义 AGENT=Agent产出';
COMMENT ON COLUMN training_plan.status IS 'CYCLE: DRAFT|ACTIVE|PAUSED|COMPLETED|ABANDONED; DAY: PENDING|ACTIVE|DONE|SKIPPED|ADJUSTED|SUPERSEDED';

ALTER TABLE advice ADD COLUMN training_plan_id BIGINT;
ALTER TABLE advice ADD COLUMN plan_date DATE;
CREATE INDEX idx_advice_plan_date ON advice (athlete_id, plan_date, status);

ALTER TABLE session_log ADD COLUMN training_plan_id BIGINT;
