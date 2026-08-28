package com.aseantec.smartfitness.coach.runtime.tool;

import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Tool 注册表。按名称查找 {@link AgentTool}。
 */
@Component
public class ToolRegistry {

    private final Map<String, AgentTool> tools;

    public ToolRegistry(Collection<AgentTool> toolBeans) {
        this.tools = toolBeans.stream().collect(Collectors.toMap(AgentTool::name, Function.identity()));
    }

    public AgentTool require(String name) {
        AgentTool tool = tools.get(name);
        if (tool == null) {
            throw new IllegalArgumentException("unknown tool: " + name);
        }
        return tool;
    }

    public Collection<AgentTool> all() {
        return tools.values();
    }
}
