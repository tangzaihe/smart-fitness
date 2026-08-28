package com.aseantec.smartfitness.coach.runtime.context;

import com.aseantec.smartfitness.coach.agent.L0ContextBuilder;
import com.aseantec.smartfitness.coach.runtime.tool.ToolResult;
import lombok.Builder;
import lombok.Value;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 送入 LLM / 规则引擎的上下文快照。
 */
@Value
@Builder
public class AgentContext {

    Long athleteId;
    Long taskId;
    Long executionId;
    String intent;
    String userText;
    String systemInstructions;
    String l0Facts;
    List<Map<String, String>> recentConversation;
    @Builder.Default
    Map<String, ToolResult> toolResults = new LinkedHashMap<>();
    String pendingUserAnswer;

    public AgentContext withToolResult(String toolName, ToolResult result) {
        Map<String, ToolResult> copy = new LinkedHashMap<>(toolResults);
        copy.put(toolName, result);
        return AgentContext.builder()
                .athleteId(athleteId)
                .taskId(taskId)
                .executionId(executionId)
                .intent(intent)
                .userText(userText)
                .systemInstructions(systemInstructions)
                .l0Facts(l0Facts)
                .recentConversation(recentConversation)
                .toolResults(copy)
                .pendingUserAnswer(pendingUserAnswer)
                .build();
    }

    public String toPromptBlock() {
        StringBuilder sb = new StringBuilder();
        sb.append("用户事实：").append(l0Facts).append('\n');
        if (!toolResults.isEmpty()) {
            sb.append("工具结果：").append(toolResults).append('\n');
        }
        if (pendingUserAnswer != null && !pendingUserAnswer.isBlank()) {
            sb.append("用户补充：").append(pendingUserAnswer).append('\n');
        }
        sb.append("用户消息：").append(userText);
        return sb.toString();
    }
}
