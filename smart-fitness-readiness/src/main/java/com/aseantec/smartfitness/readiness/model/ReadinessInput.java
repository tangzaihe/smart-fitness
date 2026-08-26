package com.aseantec.smartfitness.readiness.model;

import lombok.Builder;
import lombok.Value;

import java.util.Map;

@Value
@Builder
public class ReadinessInput {
    double sleepHours;
    int subjectiveFatigue;
    Map<String, Integer> sets7d;
    boolean medical;
    boolean painOrInjury;
}
