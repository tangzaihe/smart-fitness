package com.aseantec.smartfitness.coach.runtime.execution;

/**
 * 单次 Execution（{@code coach_run}）的运行态。
 * <p>与 {@link com.aseantec.smartfitness.coach.runtime.task.TaskState} 分离：Task 是用户可见任务，Execution 是一次具体执行过程。
 */
public enum ExecutionState {
    CREATED,
    THINKING,
    TOOL_EXECUTING,
    WAITING_USER,
    WAITING_CONFIRMATION,
    COMPLETED,
    FAILED,
    CANCELLED
}
