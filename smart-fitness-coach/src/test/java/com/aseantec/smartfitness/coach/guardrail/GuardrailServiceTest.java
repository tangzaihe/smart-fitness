package com.aseantec.smartfitness.coach.guardrail;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GuardrailServiceTest {
    private final ObjectMapper mapper = new ObjectMapper();
    private final GuardrailService service = new GuardrailService(mapper);

    @Test
    void g1DropsVerticalPushPick() throws Exception {
        ObjectNode payload = (ObjectNode) mapper.readTree("""
                {"schema_version":1,"kind":"SESSION","slots":[
                  {"slot":"A","pick":"ohp_bb","alternatives":["bb_bench"],"sets":3,"reps":8}
                ]}
                """);
        var ctx = GuardContext.builder()
                .readiness(80).avgFatigue(2).medical(false)
                .excludePatterns(Set.of("VERTICAL_PUSH"))
                .recentMuscles(Set.of())
                .retrieveCodes(Set.of("ohp_bb", "bb_bench"))
                .codeToPattern(Map.of("ohp_bb", "VERTICAL_PUSH", "bb_bench", "HORIZONTAL_PUSH"))
                .codeToMuscle(Map.of("ohp_bb", "SHOULDER", "bb_bench", "CHEST"))
                .codeToSwapGroup(Map.of("ohp_bb", "vpush_shoulder", "bb_bench", "hpush_chest"))
                .build();
        ObjectNode out = service.apply(payload, ctx);
        String pick = out.path("slots").get(0).path("pick").asText();
        assertNotEquals("ohp_bb", pick);
        assertEquals("bb_bench", pick);
    }

    @Test
    void g2LowReadinessBecomesRest() throws Exception {
        ObjectNode payload = (ObjectNode) mapper.readTree("""
                {"schema_version":1,"kind":"SESSION","slots":[{"slot":"A","pick":"bb_bench","alternatives":[],"sets":3,"reps":8}]}
                """);
        var ctx = GuardContext.builder()
                .readiness(30).avgFatigue(1).medical(false)
                .excludePatterns(Set.of()).recentMuscles(Set.of())
                .retrieveCodes(Set.of("bb_bench"))
                .codeToPattern(Map.of("bb_bench", "HORIZONTAL_PUSH"))
                .codeToMuscle(Map.of("bb_bench", "CHEST"))
                .codeToSwapGroup(Map.of("bb_bench", "hpush_chest"))
                .build();
        ObjectNode out = service.apply(payload, ctx);
        assertEquals("REST", out.path("kind").asText());
        assertTrue(out.path("slots").isEmpty());
    }
}
