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
