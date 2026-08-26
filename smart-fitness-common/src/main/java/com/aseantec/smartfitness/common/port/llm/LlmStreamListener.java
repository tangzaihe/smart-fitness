package com.aseantec.smartfitness.common.port.llm;

public interface LlmStreamListener {
    void onToken(String delta);

    void onComplete(LlmCompleteEvent event);

    void onError(Throwable error);
}
