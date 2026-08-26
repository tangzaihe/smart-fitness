package com.aseantec.smartfitness.coach.guardrail;

import lombok.Builder;
import lombok.Value;
import java.util.Map;
import java.util.Set;

@Value
@Builder
public class GuardContext {
    int readiness;
    double avgFatigue;
    boolean medical;
    Set<String> excludePatterns;
    Set<String> recentMuscles;
    Set<String> retrieveCodes;
    Map<String, String> codeToPattern;
    Map<String, String> codeToMuscle;
    Map<String, String> codeToSwapGroup;
}
