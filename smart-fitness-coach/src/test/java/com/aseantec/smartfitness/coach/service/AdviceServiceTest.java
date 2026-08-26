package com.aseantec.smartfitness.coach.service;

import com.aseantec.smartfitness.athlete.service.AthleteService;
import com.aseantec.smartfitness.coach.dto.DecideRequest;
import com.aseantec.smartfitness.coach.entity.Advice;
import com.aseantec.smartfitness.coach.mapper.AdviceMapper;
import com.aseantec.smartfitness.coach.mapper.DecisionEventMapper;
import com.aseantec.smartfitness.training.entity.SessionLog;
import com.aseantec.smartfitness.training.service.SessionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdviceServiceTest {
    @Mock
    private AdviceMapper adviceMapper;
    @Mock
    private DecisionEventMapper decisionEventMapper;
    @Mock
    private SessionService sessionService;
    @Mock
    private AthleteService athleteService;
    @Mock
    private ObjectMapper objectMapper;
    @InjectMocks
    private AdviceService adviceService;

    @Test
    void acceptTwiceIsIdempotent() {
        Advice advice = new Advice();
        advice.setId(9L);
        advice.setAthleteId(1L);
        advice.setStatus("ACCEPTED");
        when(adviceMapper.selectById(9L)).thenReturn(advice);
        SessionLog session = new SessionLog();
        session.setId(88L);
        when(sessionService.findInProgress(1L)).thenReturn(session);

        DecideRequest request = new DecideRequest();
        request.setAction("ACCEPT");
        Map<String, Object> first = adviceService.decide(1L, 9L, request);
        Map<String, Object> second = adviceService.decide(1L, 9L, request);

        assertEquals("88", first.get("sessionId"));
        assertEquals(first.get("sessionId"), second.get("sessionId"));
        assertTrue((Boolean) second.get("idempotent"));
        verify(sessionService, never()).createFromAdvice(anyLong(), anyLong(), org.mockito.ArgumentMatchers.any());
    }
}
