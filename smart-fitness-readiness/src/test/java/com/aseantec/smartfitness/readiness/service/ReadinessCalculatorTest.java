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
