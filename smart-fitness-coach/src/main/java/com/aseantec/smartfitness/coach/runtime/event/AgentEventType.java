package com.aseantec.smartfitness.coach.runtime.event;

/**
 * Agent Runtime 结构化事件类型。持久化到 {@code coach_event.type}，SSE 同名推送。
 */
public enum AgentEventType {
    TASK_CREATED,
    TASK_STARTED,
    STEP_STARTED,
    STEP_COMPLETED,
    TOOL_CALL_STARTED,
    TOOL_CALL_COMPLETED,
    TOOL_CALL_FAILED,
    PLAN_CREATED,
    PLAN_UPDATED,
    MESSAGE_DELTA,
    MESSAGE_COMPLETED,
    USER_INPUT_REQUIRED,
    CONFIRMATION_REQUIRED,
    TASK_PAUSED,
    TASK_RESUMED,
    TASK_COMPLETED,
    TASK_FAILED,
    TASK_CANCELLED,
    // 兼容旧客户端契约
    MESSAGE_START,
    MESSAGE_DONE,
    CARD_ADVICE,
    PROGRESS,
    ERROR
}
