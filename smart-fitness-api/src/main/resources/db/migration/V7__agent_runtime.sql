-- V7: Agent Runtime — Task / Execution / Plan 分层，复用 coach_run 作为 execution、coach_event 作为事件源。

CREATE TABLE agent_task (
    id                  BIGINT PRIMARY KEY,
    athlete_id          BIGINT NOT NULL,
    conversation_id     BIGINT,
    title               VARCHAR(256) NOT NULL,
    status              VARCHAR(32) NOT NULL DEFAULT 'CREATED',
    source_message_id   BIGINT,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_agent_task_athlete ON agent_task (athlete_id, created_at DESC);
CREATE INDEX idx_agent_task_conversation ON agent_task (conversation_id);
CREATE TRIGGER tg_agent_task_updated BEFORE UPDATE ON agent_task
    FOR EACH ROW EXECUTE FUNCTION trg_set_updated_at();

COMMENT ON TABLE agent_task IS 'Agent 任务：用户希望完成的一件事；与 HTTP 请求解耦';
COMMENT ON COLUMN agent_task.status IS 'CREATED|RUNNING|WAITING_USER|WAITING_CONFIRMATION|PAUSED|COMPLETED|FAILED|CANCELLED';

-- coach_run 兼任 execution
ALTER TABLE coach_run ADD COLUMN task_id BIGINT;
ALTER TABLE coach_run ADD COLUMN iteration INT NOT NULL DEFAULT 0;
ALTER TABLE coach_run ADD COLUMN execution_state VARCHAR(32);
ALTER TABLE coach_run ADD COLUMN error_message TEXT;
ALTER TABLE coach_run ADD COLUMN pending_action JSONB;
CREATE INDEX idx_run_task ON coach_run (task_id);

COMMENT ON COLUMN coach_run.execution_state IS 'THINKING|TOOL_EXECUTING|WAITING_USER|WAITING_CONFIRMATION|COMPLETED|FAILED';

CREATE TABLE agent_plan (
    id              BIGINT PRIMARY KEY,
    task_id         BIGINT NOT NULL,
    execution_id    BIGINT,
    status          VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_agent_plan_task ON agent_plan (task_id);
CREATE TRIGGER tg_agent_plan_updated BEFORE UPDATE ON agent_plan
    FOR EACH ROW EXECUTE FUNCTION trg_set_updated_at();

CREATE TABLE agent_plan_step (
    id              BIGINT PRIMARY KEY,
    plan_id         BIGINT NOT NULL,
    step_order      INT NOT NULL,
    name            VARCHAR(128) NOT NULL,
    description     TEXT,
    status          VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (plan_id, step_order)
);
CREATE INDEX idx_agent_plan_step_plan ON agent_plan_step (plan_id, step_order);
CREATE TRIGGER tg_agent_plan_step_updated BEFORE UPDATE ON agent_plan_step
    FOR EACH ROW EXECUTE FUNCTION trg_set_updated_at();

COMMENT ON COLUMN agent_plan_step.status IS 'PENDING|RUNNING|WAITING|COMPLETED|FAILED|SKIPPED';
