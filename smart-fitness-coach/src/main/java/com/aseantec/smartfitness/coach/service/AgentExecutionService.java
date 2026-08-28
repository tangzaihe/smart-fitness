package com.aseantec.smartfitness.coach.service;

import com.aseantec.smartfitness.coach.entity.AgentTask;
import com.aseantec.smartfitness.coach.entity.CoachEvent;
import com.aseantec.smartfitness.coach.entity.CoachRun;
import com.aseantec.smartfitness.coach.runtime.event.AgentEventStore;
import com.aseantec.smartfitness.coach.runtime.execution.ExecutionManager;
import com.aseantec.smartfitness.coach.runtime.execution.ExecutionState;
import com.aseantec.smartfitness.coach.runtime.task.TaskService;
import com.aseantec.smartfitness.coach.runtime.task.TaskState;
import com.aseantec.smartfitness.coach.runtime.tool.ToolExecutor;
import com.aseantec.smartfitness.common.exception.BizException;
import com.aseantec.smartfitness.common.exception.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * Agent Execution 管理 API 服务：确认、取消、事件回放。
 */
@Service
@RequiredArgsConstructor
public class AgentExecutionService {

    private final ExecutionManager executionManager;
    private final TaskService taskService;
    private final AgentEventStore eventStore;
    private final ToolExecutor toolExecutor;
    private final ObjectMapper objectMapper;

    /** 列出 execution 的历史事件（SSE 断线重连用）。 */
    public List<CoachEvent> events(Long athleteId, Long executionId) {
        CoachRun run = requireOwned(athleteId, executionId);
        return eventStore.listByExecution(run.getId());
    }

    /**
     * 用户确认 WRITE Tool 或挂起操作后继续执行。
     */
    @Transactional
    public Map<String, Object> confirm(Long athleteId, Long executionId, boolean approved) throws Exception {
        CoachRun run = requireOwned(athleteId, executionId);
        AgentTask task = taskService.require(run.getTaskId());
        if (!TaskState.WAITING_CONFIRMATION.name().equals(task.getStatus())) {
            throw new BizException(ErrorCode.BAD_REQUEST, "execution not waiting confirmation");
        }
        if (!approved) {
            executionManager.cancel(run, task);
            return Map.of("executionId", executionId, "status", "CANCELLED");
        }
        Map<String, Object> pending = parsePending(run.getPendingAction());
        String toolName = String.valueOf(pending.getOrDefault("tool", ""));
        @SuppressWarnings("unchecked")
        Map<String, Object> args = (Map<String, Object>) pending.getOrDefault("arguments", Map.of());
        toolExecutor.execute(athleteId, executionId, toolName, args, true);
        executionManager.complete(run, task, null, null);
        return Map.of("executionId", executionId, "status", "COMPLETED");
    }

    /** 取消进行中的 Execution。 */
    @Transactional
    public Map<String, Object> cancel(Long athleteId, Long executionId) {
        CoachRun run = requireOwned(athleteId, executionId);
        AgentTask task = taskService.require(run.getTaskId());
        executionManager.cancel(run, task);
        return Map.of("executionId", executionId, "status", "CANCELLED");
    }

    private CoachRun requireOwned(Long athleteId, Long executionId) {
        CoachRun run = executionManager.require(executionId);
        if (!athleteId.equals(run.getAthleteId())) {
            throw new BizException(ErrorCode.NOT_FOUND);
        }
        if (ExecutionState.CANCELLED.name().equals(run.getExecutionState())
                || "CANCELLED".equals(run.getStatus())) {
            throw new BizException(ErrorCode.BAD_REQUEST, "execution already cancelled");
        }
        return run;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parsePending(String json) throws Exception {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        return objectMapper.readValue(json, Map.class);
    }
}
