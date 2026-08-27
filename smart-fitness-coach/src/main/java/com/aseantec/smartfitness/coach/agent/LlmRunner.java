package com.aseantec.smartfitness.coach.agent;

import com.aseantec.smartfitness.common.exception.BizException;
import com.aseantec.smartfitness.common.exception.ErrorCode;
import com.aseantec.smartfitness.common.port.llm.LlmCompleteEvent;
import com.aseantec.smartfitness.common.port.llm.LlmGateway;
import com.aseantec.smartfitness.common.port.llm.LlmRequest;
import com.aseantec.smartfitness.common.port.llm.LlmStreamListener;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * LLM 流式调用辅助：阻塞等待完成，token 通过 emitter 实时转发。
 * <p>统一超时与错误处理，供 Skill 复用。
 */
@Component
@RequiredArgsConstructor
public class LlmRunner {

    private static final long TIMEOUT_SECONDS = 60L;

    private final LlmGateway llmGateway;

    /**
     * 发起流式调用；token 实时经 emitter 发出，返回完整文本。
     *
     * @param ctx     Skill 上下文（取 athleteId）
     * @param emitter 流式输出
     * @param runId   关联 coach_run.id，用于 usage 记账
     * @param purpose 调用用途，如 {@code chat} / {@code coach}
     * @param systemPrompt system prompt
     * @param userPrompt  完整 user message（含 L0 上下文）
     * @param context 额外结构化上下文，可空
     * @return 完整文本
     * @throws BizException {@code LLM_UPSTREAM} 超时或失败
     */
    public LlmCompleteEvent stream(SkillContext ctx, SkillEmitter emitter, Long runId, String purpose,
                                   String systemPrompt, String userPrompt, Map<String, Object> context) throws Exception {
        AtomicReference<LlmCompleteEvent> completeRef = new AtomicReference<>();
        AtomicReference<Throwable> errorRef = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);
        llmGateway.chatStream(LlmRequest.builder()
                        .athleteId(ctx.getAthleteId())
                        .runId(runId)
                        .purpose(purpose)
                        .systemPrompt(systemPrompt)
                        .userPrompt(userPrompt)
                        .context(context == null ? Map.of() : context)
                        .build(),
                new LlmStreamListener() {
                    @Override
                    public void onToken(String delta) {
                        try {
                            emitter.token(delta);
                        } catch (Exception ignored) {
                            // client gone
                        }
                    }

                    @Override
                    public void onComplete(LlmCompleteEvent event) {
                        completeRef.set(event);
                        latch.countDown();
                    }

                    @Override
                    public void onError(Throwable error) {
                        errorRef.set(error);
                        latch.countDown();
                    }
                });
        if (!latch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS) || completeRef.get() == null) {
            throw new BizException(ErrorCode.LLM_UPSTREAM,
                    errorRef.get() == null ? "llm timeout or failed" : errorRef.get().getMessage());
        }
        return completeRef.get();
    }
}
