package com.aseantec.smartfitness.auth.vo;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class TokenVO {
    String accessToken;
    String refreshToken;
    long expiresIn;
    String athleteId;
    boolean onboarded;
}
