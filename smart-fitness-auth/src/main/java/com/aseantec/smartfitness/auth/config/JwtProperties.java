package com.aseantec.smartfitness.auth.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {
    private String secret = "change-me-to-at-least-32-bytes-long!!";
    private String adminSecret = "change-me-admin-secret-32-bytes-min";
    private long accessTtlSeconds = 7200;
    private long refreshTtlSeconds = 1209600;
}
