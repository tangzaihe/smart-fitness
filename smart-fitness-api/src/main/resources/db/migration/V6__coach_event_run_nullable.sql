-- V6: 对话驱动后，coach_event 不一定挂某个 coach_run（ChatSkill 不建 run；
-- 对话级 message.start/message.done 也不属于任何 run）。放开 run_id NOT NULL。
-- uk_coach_event_run_seq 在 PostgreSQL 中 NULL 视为互不相同，多行 null run_id 不冲突。

ALTER TABLE coach_event ALTER COLUMN run_id DROP NOT NULL;

COMMENT ON COLUMN coach_event.run_id IS '关联 coach_run.id；对话级事件（无 run）为 NULL';
