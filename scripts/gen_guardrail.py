# -*- coding: utf-8 -*-
from pathlib import Path
R = Path(r"E:\aseantec\agent\Smart Fitness")
def w(rel, t):
    p = R / rel.replace("/", "\\")
    p.parent.mkdir(parents=True, exist_ok=True)
    p.write_text(t.lstrip("\n"), encoding="utf-8")
    print(rel)

w("smart-fitness-coach/src/main/java/com/aseantec/smartfitness/coach/guardrail/GuardContext.java", """
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
""")

w("smart-fitness-coach/src/main/java/com/aseantec/smartfitness/coach/guardrail/GuardrailService.java", r'''
package com.aseantec.smartfitness.coach.guardrail;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Iterator;

@Service
@RequiredArgsConstructor
public class GuardrailService {
    private final ObjectMapper objectMapper;

    public ObjectNode apply(JsonNode payload, GuardContext ctx) {
        ObjectNode root = payload.deepCopy();
        String kind = root.path("kind").asText("SESSION");
        if (ctx.isMedical() || ctx.getReadiness() < 40 || ctx.getAvgFatigue() >= 8) {
            root.put("kind", "REST");
            root.put("title", "Rest day");
            root.put("rationale", ctx.isMedical()
                    ? "Medical constraint: rest only. This is not a diagnosis; seek clinical care if needed."
                    : "Readiness is too low for a session.");
            root.set("slots", objectMapper.createArrayNode());
            return root;
        }
        ArrayNode slots = objectMapper.createArrayNode();
        for (JsonNode slot : root.path("slots")) {
            ObjectNode copy = slot.deepCopy();
            String pick = copy.path("pick").asText(null);
            if (!allowedPick(pick, ctx)) {
                String replacement = firstAllowedAlt(copy, ctx);
                if (replacement == null) {
                    continue;
                }
                copy.put("pick", replacement);
                pick = replacement;
            }
            ArrayNode alts = objectMapper.createArrayNode();
            for (JsonNode alt : copy.path("alternatives")) {
                String code = alt.asText();
                if (allowedAlt(code, pick, ctx)) {
                    alts.add(code);
                }
            }
            copy.set("alternatives", alts);
            slots.add(copy);
        }
        root.set("slots", slots);
        if (slots.isEmpty() && "SESSION".equals(kind)) {
            root.put("kind", "REST");
            root.put("rationale", "No safe slots remained after guardrails.");
        }
        return root;
    }

    private boolean allowedPick(String code, GuardContext ctx) {
        if (code == null || !ctx.getRetrieveCodes().contains(code)) {
            return false;
        }
        String pattern = ctx.getCodeToPattern().get(code);
        if (pattern != null && ctx.getExcludePatterns().contains(pattern)) {
            return false;
        }
        String muscle = ctx.getCodeToMuscle().get(code);
        return muscle == null || !ctx.getRecentMuscles().contains(muscle);
    }

    private boolean allowedAlt(String code, String pick, GuardContext ctx) {
        if (code == null || !ctx.getCodeToSwapGroup().containsKey(code)) {
            return false;
        }
        String pattern = ctx.getCodeToPattern().get(code);
        if (pattern != null && ctx.getExcludePatterns().contains(pattern)) {
            return false;
        }
        String pickGroup = ctx.getCodeToSwapGroup().get(pick);
        String altGroup = ctx.getCodeToSwapGroup().get(code);
        return pickGroup != null && pickGroup.equals(altGroup);
    }

    private String firstAllowedAlt(JsonNode slot, GuardContext ctx) {
        for (JsonNode alt : slot.path("alternatives")) {
            String code = alt.asText();
            if (allowedPick(code, ctx)) {
                return code;
            }
        }
        return null;
    }
}
''')

w("smart-fitness-coach/src/test/java/com/aseantec/smartfitness/coach/guardrail/GuardrailServiceTest.java", r'''
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
''')
print("guardrail ok")
