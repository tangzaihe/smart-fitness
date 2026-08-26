# -*- coding: utf-8 -*-
from pathlib import Path
ROOT = Path(r"E:\aseantec\agent\Smart Fitness")

def w(rel, text):
    p = ROOT / rel.replace("/", "\\")
    p.parent.mkdir(parents=True, exist_ok=True)
    p.write_text(text.lstrip("\n"), encoding="utf-8")
    print(rel)

# ---------- infra fixes ----------
w("smart-fitness-infra/src/main/java/com/aseantec/smartfitness/infra/llm/FakeLlmGateway.java", r'''
package com.aseantec.smartfitness.infra.llm;

import com.aseantec.smartfitness.common.exception.BizException;
import com.aseantec.smartfitness.common.exception.ErrorCode;
import com.aseantec.smartfitness.common.port.llm.LlmCompleteEvent;
import com.aseantec.smartfitness.common.port.llm.LlmGateway;
import com.aseantec.smartfitness.common.port.llm.LlmRequest;
import com.aseantec.smartfitness.common.port.llm.LlmStreamListener;
import com.aseantec.smartfitness.infra.usage.QuotaService;
import com.aseantec.smartfitness.infra.usage.UsageRecorder;
import com.aseantec.smartfitness.infra.usage.entity.LlmCallUsage;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.llm.provider", havingValue = "fake", matchIfMissing = true)
public class FakeLlmGateway implements LlmGateway {
    private final QuotaService quotaService;
    private final UsageRecorder usageRecorder;
    private final ObjectMapper objectMapper;

    @Override
    public void chatStream(LlmRequest request, LlmStreamListener listener) {
        long started = System.currentTimeMillis();
        try {
            quotaService.assertCanCall(request.getAthleteId());
        } catch (BizException ex) {
            persist(request, 0, 0, "ESTIMATED", "QUOTA_BLOCKED", ex.getErrorCode().name(),
                    (int) (System.currentTimeMillis() - started), null);
            listener.onError(ex);
            return;
        }
        try {
            String json = buildAdviceJson(request);
            for (String token : json.split("(?<=\\{)|(?<=,)|(?<=:)")) {
                if (!token.isEmpty()) {
                    listener.onToken(token);
                }
            }
            int prompt = estimate(request.getSystemPrompt()) + estimate(request.getUserPrompt());
            int completion = estimate(json);
            persist(request, prompt, completion, "ESTIMATED", "SUCCESS", null,
                    (int) (System.currentTimeMillis() - started), 200);
            listener.onComplete(LlmCompleteEvent.builder()
                    .text(json)
                    .promptTokens(prompt)
                    .completionTokens(completion)
                    .usageSource("ESTIMATED")
                    .keySource("SYSTEM")
                    .provider("fake")
                    .model("fake-coach")
                    .build());
        } catch (Exception ex) {
            log.error("fake llm failed", ex);
            persist(request, 0, 0, "ESTIMATED", "FAILED", "FAKE_ERROR",
                    (int) (System.currentTimeMillis() - started), 500);
            listener.onError(new BizException(ErrorCode.LLM_UPSTREAM, ex.getMessage()));
        }
    }

    @SuppressWarnings("unchecked")
    private String buildAdviceJson(LlmRequest request) throws Exception {
        Map<String, Object> ctx = request.getContext() == null ? Map.of() : request.getContext();
        List<Map<String, Object>> options = (List<Map<String, Object>>) ctx.getOrDefault("retrieveOptions", List.of());
        boolean rest = Boolean.TRUE.equals(ctx.get("forceRest"));
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("schema_version", 1);
        payload.put("kind", rest ? "REST" : "SESSION");
        payload.put("title", rest ? "Rest day" : "Coach session");
        payload.put("rationale", rest ? "Readiness or medical constraint requires rest." : "Built from catalog retrieve.");
        payload.put("slots", rest ? List.of() : buildSlots(options));
        return objectMapper.writeValueAsString(payload);
    }

    private List<Map<String, Object>> buildSlots(List<Map<String, Object>> options) {
        if (options == null || options.isEmpty()) {
            return List.of();
        }
        Map<String, Object> first = options.get(0);
        List<String> alts = options.stream().skip(1).limit(3)
                .map(o -> String.valueOf(o.get("code"))).toList();
        Map<String, Object> slot = new LinkedHashMap<>();
        slot.put("slot", "A");
        slot.put("pick", first.get("code"));
        slot.put("alternatives", alts);
        slot.put("sets", 3);
        slot.put("reps", 8);
        slot.put("load_kg", null);
        slot.put("rpe_cap", 8);
        return List.of(slot);
    }

    private int estimate(String text) {
        if (text == null || text.isBlank()) {
            return 0;
        }
        return Math.max(1, text.length() / 4);
    }

    private void persist(LlmRequest request, int prompt, int completion, String usageSource,
                         String status, String error, int durationMs, Integer httpStatus) {
        LlmCallUsage row = new LlmCallUsage();
        row.setAthleteId(request.getAthleteId());
        row.setRunId(request.getRunId());
        row.setCallSeq(1);
        row.setPurpose(request.getPurpose() == null ? "chat" : request.getPurpose());
        row.setKeySource("SYSTEM");
        row.setBilledTo("PLATFORM");
        row.setProvider("fake");
        row.setModel("fake-coach");
        row.setPromptTokens(prompt);
        row.setCompletionTokens(completion);
        row.setUsageSource(usageSource);
        row.setDurationMs(durationMs);
        row.setHttpStatus(httpStatus);
        row.setStatus(status);
        row.setErrorCode(error);
        usageRecorder.record(row);
    }
}
''')

w("smart-fitness-infra/src/main/java/com/aseantec/smartfitness/infra/llm/OpenAiLlmGateway.java", r'''
package com.aseantec.smartfitness.infra.llm;

import com.aseantec.smartfitness.common.exception.BizException;
import com.aseantec.smartfitness.common.exception.ErrorCode;
import com.aseantec.smartfitness.common.port.llm.LlmCompleteEvent;
import com.aseantec.smartfitness.common.port.llm.LlmGateway;
import com.aseantec.smartfitness.common.port.llm.LlmRequest;
import com.aseantec.smartfitness.common.port.llm.LlmStreamListener;
import com.aseantec.smartfitness.infra.config.LlmProperties;
import com.aseantec.smartfitness.infra.usage.QuotaService;
import com.aseantec.smartfitness.infra.usage.UsageRecorder;
import com.aseantec.smartfitness.infra.usage.entity.LlmCallUsage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.llm.provider", havingValue = "openai")
public class OpenAiLlmGateway implements LlmGateway {
    private final LlmProperties properties;
    private final QuotaService quotaService;
    private final UsageRecorder usageRecorder;

    @Override
    public void chatStream(LlmRequest request, LlmStreamListener listener) {
        long started = System.currentTimeMillis();
        try {
            quotaService.assertCanCall(request.getAthleteId());
        } catch (BizException ex) {
            persist(request, 0, 0, "ESTIMATED", "QUOTA_BLOCKED", ex.getErrorCode().name(),
                    (int) (System.currentTimeMillis() - started), null);
            listener.onError(ex);
            return;
        }
        if (properties.getApiKey() == null || properties.getApiKey().isBlank()) {
            BizException missing = new BizException(ErrorCode.LLM_UPSTREAM, "LLM_API_KEY is empty");
            persist(request, 0, 0, "ESTIMATED", "FAILED", "MISSING_KEY",
                    (int) (System.currentTimeMillis() - started), null);
            listener.onError(missing);
            return;
        }
        OpenAiStreamingChatModel model = OpenAiStreamingChatModel.builder()
                .baseUrl(properties.getBaseUrl())
                .apiKey(properties.getApiKey())
                .modelName(properties.getModel())
                .timeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
                .build();
        StringBuilder full = new StringBuilder();
        model.chat(
                java.util.List.of(
                        SystemMessage.from(nvl(request.getSystemPrompt())),
                        UserMessage.from(nvl(request.getUserPrompt()))
                ),
                new StreamingChatResponseHandler() {
                    @Override
                    public void onPartialResponse(String partialResponse) {
                        full.append(partialResponse);
                        listener.onToken(partialResponse);
                    }

                    @Override
                    public void onCompleteResponse(ChatResponse completeResponse) {
                        int prompt = 0;
                        int completion = 0;
                        String usageSource = "ESTIMATED";
                        if (completeResponse.metadata() != null && completeResponse.metadata().tokenUsage() != null) {
                            var usage = completeResponse.metadata().tokenUsage();
                            if (usage.inputTokenCount() != null) {
                                prompt = usage.inputTokenCount();
                            }
                            if (usage.outputTokenCount() != null) {
                                completion = usage.outputTokenCount();
                            }
                            if (prompt + completion > 0) {
                                usageSource = "PROVIDER";
                            }
                        }
                        if ("ESTIMATED".equals(usageSource)) {
                            prompt = Math.max(1, nvl(request.getSystemPrompt()).length() / 4
                                    + nvl(request.getUserPrompt()).length() / 4);
                            completion = Math.max(1, full.length() / 4);
                        }
                        persist(request, prompt, completion, usageSource, "SUCCESS", null,
                                (int) (System.currentTimeMillis() - started), 200);
                        listener.onComplete(LlmCompleteEvent.builder()
                                .text(full.toString())
                                .promptTokens(prompt)
                                .completionTokens(completion)
                                .usageSource(usageSource)
                                .keySource("SYSTEM")
                                .provider("openai")
                                .model(properties.getModel())
                                .build());
                    }

                    @Override
                    public void onError(Throwable error) {
                        log.error("openai llm failed", error);
                        persist(request, 0, 0, "ESTIMATED", "FAILED", "PROVIDER_ERROR",
                                (int) (System.currentTimeMillis() - started), 502);
                        listener.onError(new BizException(ErrorCode.LLM_UPSTREAM, error.getMessage()));
                    }
                }
        );
    }

    private String nvl(String s) {
        return s == null ? "" : s;
    }

    private void persist(LlmRequest request, int prompt, int completion, String usageSource,
                         String status, String error, int durationMs, Integer httpStatus) {
        LlmCallUsage row = new LlmCallUsage();
        row.setAthleteId(request.getAthleteId());
        row.setRunId(request.getRunId());
        row.setCallSeq(1);
        row.setPurpose(request.getPurpose() == null ? "chat" : request.getPurpose());
        row.setKeySource("SYSTEM");
        row.setBilledTo("PLATFORM");
        row.setProvider("openai");
        row.setModel(properties.getModel());
        row.setPromptTokens(prompt);
        row.setCompletionTokens(completion);
        row.setUsageSource(usageSource);
        row.setDurationMs(durationMs);
        row.setHttpStatus(httpStatus);
        row.setStatus(status);
        row.setErrorCode(error);
        usageRecorder.record(row);
    }
}
''')

print("infra llm ok")
