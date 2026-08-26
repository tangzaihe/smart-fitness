# -*- coding: utf-8 -*-
from pathlib import Path
R = Path(r"E:\aseantec\agent\Smart Fitness")
def w(rel, t):
    p = R / rel.replace("/", "\\")
    p.parent.mkdir(parents=True, exist_ok=True)
    p.write_text(t.lstrip("\n"), encoding="utf-8")
    print(rel)

w("smart-fitness-coach/src/main/java/com/aseantec/smartfitness/coach/dto/DecideRequest.java", """
package com.aseantec.smartfitness.coach.dto;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
@Data
public class DecideRequest {
    @NotBlank
    private String action;
    private String note;
}
""")

w("smart-fitness-coach/src/main/java/com/aseantec/smartfitness/coach/service/AdviceService.java", r'''
package com.aseantec.smartfitness.coach.service;

import com.aseantec.smartfitness.athlete.service.AthleteService;
import com.aseantec.smartfitness.coach.dto.DecideRequest;
import com.aseantec.smartfitness.coach.entity.Advice;
import com.aseantec.smartfitness.coach.entity.DecisionEvent;
import com.aseantec.smartfitness.coach.mapper.AdviceMapper;
import com.aseantec.smartfitness.coach.mapper.DecisionEventMapper;
import com.aseantec.smartfitness.common.exception.BizException;
import com.aseantec.smartfitness.common.exception.ErrorCode;
import com.aseantec.smartfitness.training.entity.SessionLog;
import com.aseantec.smartfitness.training.service.SessionService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AdviceService {
    private final AdviceMapper adviceMapper;
    private final DecisionEventMapper decisionEventMapper;
    private final SessionService sessionService;
    private final AthleteService athleteService;
    private final ObjectMapper objectMapper;

    public Advice requireOwned(Long athleteId, Long adviceId) {
        Advice advice = adviceMapper.selectById(adviceId);
        if (advice == null || !athleteId.equals(advice.getAthleteId())) {
            throw new BizException(ErrorCode.NOT_FOUND);
        }
        return advice;
    }

    public Set<String> allowedCodes(Advice advice) {
        Set<String> codes = new HashSet<>();
        try {
            JsonNode root = objectMapper.readTree(advice.getPayload());
            for (JsonNode slot : root.path("slots")) {
                if (slot.hasNonNull("pick")) {
                    codes.add(slot.get("pick").asText());
                }
                for (JsonNode alt : slot.path("alternatives")) {
                    codes.add(alt.asText());
                }
            }
        } catch (Exception ignored) {
            return Set.of();
        }
        return codes;
    }

    @Transactional
    public Map<String, Object> decide(Long athleteId, Long adviceId, DecideRequest request) {
        athleteService.assertOnboarded(athleteId);
        Advice advice = requireOwned(athleteId, adviceId);
        String action = request.getAction().toUpperCase();
        if ("ACCEPTED".equals(advice.getStatus()) && "ACCEPT".equals(action)) {
            SessionLog existing = sessionService.findInProgress(athleteId);
            return Map.of("adviceId", String.valueOf(advice.getId()),
                    "status", advice.getStatus(),
                    "sessionId", existing == null ? "" : String.valueOf(existing.getId()),
                    "idempotent", true);
        }
        if (!"PENDING".equals(advice.getStatus())) {
            throw new BizException(ErrorCode.ADVICE_NOT_DECIDABLE);
        }
        if (!Set.of("ACCEPT", "REST", "REJECT").contains(action)) {
            throw new BizException(ErrorCode.BAD_REQUEST, "action must be ACCEPT/REST/REJECT");
        }
        advice.setStatus("REJECT".equals(action) ? "REJECTED" : "ACCEPTED");
        advice.setDecidedAt(OffsetDateTime.now(ZoneOffset.UTC));
        adviceMapper.updateById(advice);
        DecisionEvent event = new DecisionEvent();
        event.setAdviceId(adviceId);
        event.setAthleteId(athleteId);
        event.setAction(action);
        event.setNote(request.getNote());
        decisionEventMapper.insert(event);
        SessionLog session = null;
        if ("ACCEPT".equals(action)) {
            session = sessionService.createFromAdvice(athleteId, adviceId, advice.getPayload());
        }
        return Map.of(
                "adviceId", String.valueOf(advice.getId()),
                "status", advice.getStatus(),
                "sessionId", session == null ? "" : String.valueOf(session.getId()),
                "idempotent", false
        );
    }
}
''')
print("advice service ok")
