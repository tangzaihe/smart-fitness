package com.aseantec.smartfitness.common.port.llm;

import lombok.Builder;
import lombok.Value;

import java.util.Map;

@Value
@Builder
public class LlmRequest {
    Long athleteId;
    Long runId;
    String purpose;
    String systemPrompt;
    String userPrompt;
    String model;
    Map<String, Object> context;
}
