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

/**
 * 处方确认。LLM 不写课次；仅 ACCEPT 且 payload 含课表时创建 {@code session_log}。
 */
@Service
@RequiredArgsConstructor
public class AdviceService {
    private final AdviceMapper adviceMapper;
    private final DecisionEventMapper decisionEventMapper;
    private final SessionService sessionService;
    private final AthleteService athleteService;
    private final ObjectMapper objectMapper;

    /**
     * 按运动员校验处方归属。
     *
     * @throws BizException {@code NOT_FOUND} 不存在或不属于该人
     */
    public Advice requireOwned(Long athleteId, Long adviceId) {
        Advice advice = adviceMapper.selectById(adviceId);
        if (advice == null || !athleteId.equals(advice.getAthleteId())) {
            throw new BizException(ErrorCode.NOT_FOUND);
        }
        return advice;
    }

    /**
     * 处方 payload 中 pick + alternatives 的动作编码，记组时白名单用。
     */
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

    /**
     * 用户确认处方。ACCEPT 已确认单返回原 sessionId，不新建课。
     *
     * @return adviceId、status、sessionId（无课为空串）、idempotent
     * @throws BizException {@code NOT_FOUND}；{@code ADVICE_NOT_DECIDABLE} 非 PENDING；
     *                      {@code BAD_REQUEST} action 非法
     */
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
