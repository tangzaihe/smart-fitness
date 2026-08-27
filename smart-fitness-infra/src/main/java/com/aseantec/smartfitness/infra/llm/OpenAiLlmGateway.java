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
                        SystemMessage.from(emptyToDefault(request.getSystemPrompt())),
                        UserMessage.from(buildUserMessage(request))
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
                            prompt = Math.max(1, emptyToDefault(request.getSystemPrompt()).length() / 4
                                    + emptyToDefault(request.getUserPrompt()).length() / 4);
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

    private String emptyToDefault(String value) {
        return value == null ? "" : value;
    }

    /**
     * 把结构化 context 拼进 user message，让 provider 真正看到候选动作/forceRest 等事实。
     * <p>修复 P0 缺口：原实现只传 systemPrompt + userPrompt，丢弃了 context。
     */
    private String buildUserMessage(LlmRequest request) {
        String user = emptyToDefault(request.getUserPrompt());
        if (request.getContext() == null || request.getContext().isEmpty()) {
            return user;
        }
        try {
            String ctxJson = new com.fasterxml.jackson.databind.ObjectMapper()
                    .writeValueAsString(request.getContext());
            return user + "\n\n[context]\n" + ctxJson;
        } catch (Exception ignored) {
            return user;
        }
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
