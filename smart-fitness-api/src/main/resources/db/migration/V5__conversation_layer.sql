-- V5: Conversation-first agent layer.
-- 对话驱动健身：Conversation + Message；coach_run / coach_event / advice 关联 conversation_id。
-- 不改 advice 确认门与 session_log 写入路径。

CREATE TABLE conversation (
    id              BIGINT PRIMARY KEY,
    athlete_id      BIGINT NOT NULL,
    primary_intent  VARCHAR(32) NOT NULL DEFAULT 'CHAT',
    status          VARCHAR(16) NOT NULL DEFAULT 'OPEN',
    title           VARCHAR(128),
    summary         TEXT,
    coach_plan_id   BIGINT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_conversation_athlete ON conversation (athlete_id, created_at DESC);
CREATE TRIGGER tg_conversation_updated BEFORE UPDATE ON conversation
    FOR EACH ROW EXECUTE FUNCTION trg_set_updated_at();

CREATE TABLE conversation_message (
    id              BIGINT PRIMARY KEY,
    conversation_id BIGINT NOT NULL,
    athlete_id      BIGINT NOT NULL,
    role            VARCHAR(16) NOT NULL,
    content         TEXT NOT NULL DEFAULT '',
    content_type    VARCHAR(16) NOT NULL DEFAULT 'TEXT',
    metadata        JSONB NOT NULL DEFAULT '{}',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_message_conversation ON conversation_message (conversation_id, created_at ASC);
CREATE INDEX idx_message_athlete ON conversation_message (athlete_id, created_at DESC);

-- 关联现有表到对话线程（可空，向后兼容 P0 旧 run）
ALTER TABLE coach_run ADD COLUMN conversation_id BIGINT;
ALTER TABLE coach_run ADD COLUMN message_id BIGINT;
ALTER TABLE coach_run ADD COLUMN skill VARCHAR(32);
CREATE INDEX idx_run_conversation ON coach_run (conversation_id);

ALTER TABLE coach_event ADD COLUMN conversation_id BIGINT;
ALTER TABLE coach_event ADD COLUMN message_id BIGINT;

ALTER TABLE advice ADD COLUMN conversation_id BIGINT;
CREATE INDEX idx_advice_conversation ON advice (conversation_id);

COMMENT ON TABLE conversation IS '对话线程；对话驱动健身的主入口';
COMMENT ON TABLE conversation_message IS '对话消息；role=USER|ASSISTANT|SYSTEM；content_type=TEXT|CARD_REF|MIXED';
COMMENT ON COLUMN conversation.primary_intent IS 'CHAT | TODAY_SESSION | WORKOUT_PLANNING | REST';
