package com.aseantec.smartfitness.coach.runtime.task;

/**
 * Agent 任务持久化状态。对应 {@code agent_task.status}。
 * <p>不包含 THINKING / TOOL_EXECUTING 等短暂运行态，那些落在 {@code coach_run.execution_state}。
 */
public enum TaskState {
    CREATED,
    RUNNING,
    WAITING_USER,
    WAITING_CONFIRMATION,
    PAUSED,
    COMPLETED,
    FAILED,
    CANCELLED
}
