# -*- coding: utf-8 -*-
from pathlib import Path
R = Path(r"E:\aseantec\agent\Smart Fitness")
def w(rel, t):
    p = R / rel.replace("/", "\\")
    p.parent.mkdir(parents=True, exist_ok=True)
    p.write_text(t.lstrip("\n"), encoding="utf-8")
    print(rel)

w("smart-fitness-readiness/src/main/java/com/aseantec/smartfitness/readiness/model/ReadinessInput.java", """
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
""")

w("smart-fitness-readiness/src/main/java/com/aseantec/smartfitness/readiness/model/ReadinessScore.java", """
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
""")

w("smart-fitness-readiness/src/main/java/com/aseantec/smartfitness/readiness/service/ReadinessCalculator.java", r'''
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
''')

w("smart-fitness-readiness/src/test/java/com/aseantec/smartfitness/readiness/service/ReadinessCalculatorTest.java", r'''
package com.aseantec.smartfitness.readiness.service;

import com.aseantec.smartfitness.readiness.model.ReadinessInput;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReadinessCalculatorTest {
    private final ReadinessCalculator calculator = new ReadinessCalculator();

    @Test
    void sampleA() {
        var score = calculator.calculate(ReadinessInput.builder()
                .sleepHours(8)
                .subjectiveFatigue(4)
                .sets7d(Map.of())
                .medical(false)
                .painOrInjury(false)
                .build());
        assertEquals(90, score.getReadiness());
    }

    @Test
    void sampleB() {
        var score = calculator.calculate(ReadinessInput.builder()
                .sleepHours(4)
                .subjectiveFatigue(9)
                .sets7d(Map.of())
                .medical(false)
                .painOrInjury(false)
                .build());
        assertEquals(46, score.getReadiness());
        assertTrue(Math.abs(score.getReadiness() - 46) <= 1);
    }
}
''')
print("readiness calc ok")
