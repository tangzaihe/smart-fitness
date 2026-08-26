package com.aseantec.smartfitness.common.port.llm;

public interface LlmGateway {
    void chatStream(LlmRequest request, LlmStreamListener listener);
}
