package com.aseantec.smartfitness.common.port.llm;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class LlmCompleteEvent {
    String text;
    int promptTokens;
    int completionTokens;
    String usageSource;
    String keySource;
    String model;
    String provider;
}
