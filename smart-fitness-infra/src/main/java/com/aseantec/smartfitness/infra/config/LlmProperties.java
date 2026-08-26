package com.aseantec.smartfitness.infra.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app.llm")
public class LlmProperties {
    /**
     * fake | openai
     */
    private String provider = "fake";
    private String baseUrl = "https://api.openai.com/v1";
    private String apiKey = "";
    private String model = "gpt-4o-mini";
    private int timeoutSeconds = 60;
}
