package com.aseantec.smartfitness.coach.runtime.tool;

import com.aseantec.smartfitness.coach.runtime.event.AgentEventPublisher;
import com.aseantec.smartfitness.coach.runtime.event.AgentEventType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Tool 执行器：权限检查 → 执行 → 发布 Tool 事件。
 */
@Component
@RequiredArgsConstructor
public class ToolExecutor {

    private final ToolRegistry registry;
    private final ToolPermissionManager permissionManager;
    private final AgentEventPublisher eventPublisher;

    /**
     * 执行 Tool 并发布 {@code TOOL_CALL_STARTED/COMPLETED/FAILED}。
     *
     * @return 执行结果；WRITE 且未获授权时返回 {@code null} 表示需确认
     */
    public ToolResult execute(Long athleteId, Long executionId, String toolName, Map<String, Object> arguments,
                              boolean confirmed) throws Exception {
        AgentTool tool = registry.require(toolName);
        if (!permissionManager.mayExecute(tool, confirmed)) {
            return null;
        }
        Map<String, Object> startPayload = new HashMap<>();
        startPayload.put("tool", toolName);
        startPayload.put("arguments", arguments == null ? Map.of() : arguments);
        eventPublisher.publish(executionId, AgentEventType.TOOL_CALL_STARTED, startPayload);
        try {
            ToolResult result = tool.execute(athleteId, arguments == null ? Map.of() : arguments);
            Map<String, Object> donePayload = new HashMap<>();
            donePayload.put("tool", toolName);
            donePayload.put("ok", result.ok());
            donePayload.put("result", result.data());
            if (!result.ok()) {
                eventPublisher.publish(executionId, AgentEventType.TOOL_CALL_FAILED, donePayload);
            } else {
                eventPublisher.publish(executionId, AgentEventType.TOOL_CALL_COMPLETED, donePayload);
            }
            return result;
        } catch (Exception ex) {
            Map<String, Object> failPayload = Map.of("tool", toolName, "ok", false, "error", ex.getMessage());
            eventPublisher.publish(executionId, AgentEventType.TOOL_CALL_FAILED, failPayload);
            throw ex;
        }
    }
}
