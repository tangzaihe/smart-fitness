package com.aseantec.smartfitness.coach.runtime.tool;

import com.aseantec.smartfitness.coach.runtime.tool.impl.GetTrainingRecordsTool;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ToolRegistryTest {

    @Test
    void registersAllToolBeans() {
        GetTrainingRecordsTool recordsTool = new GetTrainingRecordsTool(null);
        ToolRegistry registry = new ToolRegistry(List.of(recordsTool));
        AgentTool tool = registry.require("getTrainingRecords");
        assertNotNull(tool);
        assertEquals("getTrainingRecords", tool.name());
        assertEquals(ToolPermission.READ, tool.permission());
    }
}
