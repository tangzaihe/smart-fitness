package com.aseantec.smartfitness.coach.runtime.loop;

import com.aseantec.smartfitness.coach.runtime.action.CompleteAction;
import com.aseantec.smartfitness.coach.runtime.action.ToolCallAction;
import com.aseantec.smartfitness.coach.runtime.context.AgentContext;
import com.aseantec.smartfitness.coach.runtime.event.AgentEventPublisher;
import com.aseantec.smartfitness.coach.runtime.execution.ExecutionState;
import com.aseantec.smartfitness.coach.runtime.tool.AgentTool;
import com.aseantec.smartfitness.coach.runtime.tool.ToolExecutor;
import com.aseantec.smartfitness.coach.runtime.tool.ToolPermission;
import com.aseantec.smartfitness.coach.runtime.tool.ToolRegistry;
import com.aseantec.smartfitness.coach.runtime.tool.ToolResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgentLoopTest {

    private AgentLoop agentLoop;
    private final AtomicReference<String> lastEvent = new AtomicReference<>();

    @BeforeEach
    void setUp() {
        AgentTool trainingRecords = new AgentTool() {
            @Override
            public String name() {
                return "getTrainingRecords";
            }

            @Override
            public String description() {
                return "test";
            }

            @Override
            public ToolPermission permission() {
                return ToolPermission.READ;
            }

            @Override
            public ToolResult execute(Long athleteId, Map<String, Object> arguments) {
                return ToolResult.success(Map.of("count", 5));
            }
        };
        ToolRegistry registry = new ToolRegistry(List.of(trainingRecords));
        AgentEventPublisher publisher = (executionId, type, data) -> lastEvent.set(type.name());
        ToolExecutor executor = new ToolExecutor(registry, new com.aseantec.smartfitness.coach.runtime.tool.ToolPermissionManager(), publisher);
        agentLoop = new AgentLoop(executor, publisher, new ObjectMapper());
    }

    @Test
    void runsToolThenCompletes() throws Exception {
        AgentContext context = AgentContext.builder()
                .athleteId(1L)
                .executionId(99L)
                .userText("我最近训练了几次")
                .l0Facts("goal=STRENGTH")
                .build();
        LoopResult result = agentLoop.run(context, (ctx, state) -> {
            if (!ctx.getToolResults().containsKey("getTrainingRecords")) {
                return new ToolCallAction("getTrainingRecords", Map.of("days", 30));
            }
            return new CompleteAction();
        }, (state, iteration) -> null, false);
        assertEquals(LoopResult.Status.COMPLETED, result.status());
        assertTrue(lastEvent.get().contains("TASK_COMPLETED") || lastEvent.get().contains("TOOL_CALL"));
    }
}
