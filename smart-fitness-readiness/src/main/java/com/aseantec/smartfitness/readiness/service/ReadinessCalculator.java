package com.aseantec.smartfitness.readiness.service;

import com.aseantec.smartfitness.readiness.model.ReadinessInput;
import com.aseantec.smartfitness.readiness.model.ReadinessScore;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class ReadinessCalculator {
    private static final Map<String, Integer> CAP = Map.ofEntries(
            Map.entry("CHEST", 16), Map.entry("BACK", 16), Map.entry("QUAD", 16),
            Map.entry("GLUTE", 16), Map.entry("HAMSTRING", 16),
            Map.entry("SHOULDER", 12), Map.entry("BICEP", 12), Map.entry("TRICEP", 12), Map.entry("CORE", 12),
            Map.entry("CALF", 10), Map.entry("FOREARM", 10)
    );

    public ReadinessScore calculate(ReadinessInput input) {
        double sleep = input.getSleepHours();
        double sleepScore;
        if (sleep >= 8.0) {
            sleepScore = 100;
        } else if (sleep >= 7.0) {
            sleepScore = 80 + 20 * (sleep - 7.0);
        } else if (sleep >= 5.0) {
            sleepScore = 40 + 20 * (sleep - 5.0);
        } else {
            sleepScore = 20;
        }
        Map<String, Double> fatigue = new HashMap<>();
        double sum = 0;
        int n = 0;
        if (input.getSets7d() != null) {
            for (var e : input.getSets7d().entrySet()) {
                if (e.getValue() == null || e.getValue() <= 0) {
                    continue;
                }
                int cap = CAP.getOrDefault(e.getKey(), 12);
                double f = Math.min(10.0, 10.0 * e.getValue() / cap);
                f = Math.round(f * 10.0) / 10.0;
                fatigue.put(e.getKey(), f);
                sum += f;
                n++;
            }
        }
        double avgFatigue = n == 0 ? 0 : sum / n;
        double loadScore = 100 - avgFatigue * 10;
        double feelScore = 100 - input.getSubjectiveFatigue() * 10;
        int penalty = 0;
        if (input.isMedical()) {
            penalty = 20;
        } else if (input.isPainOrInjury()) {
            penalty = 15;
        }
        double raw = 0.40 * sleepScore + 0.35 * loadScore + 0.25 * feelScore - penalty;
        int readiness = (int) Math.round(Math.max(0, Math.min(100, raw)));
        int recovery = (int) Math.round(Math.max(0, Math.min(100, 0.5 * sleepScore + 0.5 * loadScore)));
        return ReadinessScore.builder()
                .readiness(readiness)
                .recovery(recovery)
                .sleepScore(sleepScore)
                .loadScore(loadScore)
                .feelScore(feelScore)
                .constraintPenalty(penalty)
                .avgFatigue(avgFatigue)
                .fatigueByMuscle(fatigue)
                .build();
    }
}
