package com.aseantec.smartfitness.readiness.model;

import lombok.Builder;
import lombok.Value;

import java.util.Map;

@Value
@Builder
public class ReadinessScore {
    int readiness;
    int recovery;
    double sleepScore;
    double loadScore;
    double feelScore;
    int constraintPenalty;
    double avgFatigue;
    Map<String, Double> fatigueByMuscle;
}
