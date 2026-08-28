package com.aseantec.smartfitness.coach.runtime.tool;

import java.util.Map;

/**
 * Tool 执行结果。
 *
 * @param ok      是否成功
 * @param data    结构化结果
 * @param error   失败时的错误摘要
 */
public record ToolResult(boolean ok, Map<String, Object> data, String error) {

    public static ToolResult success(Map<String, Object> data) {
        return new ToolResult(true, data == null ? Map.of() : data, null);
    }

    public static ToolResult failure(String error) {
        return new ToolResult(false, Map.of(), error);
    }
}
