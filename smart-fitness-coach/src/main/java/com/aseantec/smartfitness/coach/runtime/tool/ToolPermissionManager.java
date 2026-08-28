package com.aseantec.smartfitness.coach.runtime.tool;

import org.springframework.stereotype.Component;

/**
 * Tool 权限管理。WRITE Tool 未经用户确认不得执行。
 */
@Component
public class ToolPermissionManager {

    /**
     * 判断是否允许执行 Tool。
     *
     * @param tool      目标 Tool
     * @param confirmed 用户是否已确认（针对 WRITE）
     */
    public boolean mayExecute(AgentTool tool, boolean confirmed) {
        if (tool.permission() == ToolPermission.READ) {
            return true;
        }
        return confirmed;
    }

    /** WRITE Tool 且未确认时需要走确认门。 */
    public boolean requiresConfirmation(AgentTool tool) {
        return tool.permission() == ToolPermission.WRITE;
    }
}
