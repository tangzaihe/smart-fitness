package com.aseantec.smartfitness.common.context;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CurrentUser {
    Long userId;
    Long athleteId;
    String audience;
    String tokenId;
    boolean admin;
}
