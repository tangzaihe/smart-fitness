package com.aseantec.smartfitness.coach.agent.skill;

import com.aseantec.smartfitness.coach.agent.CoachPolicyService;
import com.aseantec.smartfitness.coach.agent.CoachSkill;
import com.aseantec.smartfitness.coach.agent.LlmRunner;
import com.aseantec.smartfitness.coach.agent.SkillContext;
import com.aseantec.smartfitness.coach.agent.SkillEmitter;
import com.aseantec.smartfitness.coach.agent.SkillOutcome;
import com.aseantec.smartfitness.common.port.llm.LlmCompleteEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 闲聊 Skill：L0 上下文 + 单次 LLM 自然语言回复，不产出卡片。
 * <p>意图 {@code CHAT} / {@code REST} / {@code WORKOUT_PLANNING}（P0 暂以聊天兜底）走本 Skill。
 */
@Component
@RequiredArgsConstructor
public class ChatSkill implements CoachSkill {

    private final LlmRunner llmRunner;
    private final CoachPolicyService policyService;

    @Override
    public String name() {
        return "chat";
    }

    @Override
    public SkillOutcome run(SkillContext ctx, SkillEmitter emitter) throws Exception {
        String systemPrompt = policyService.promptFor("CHAT");
        String userPrompt = buildUserPrompt(ctx);
        LlmCompleteEvent complete = llmRunner.stream(ctx, emitter, null, "chat",
                systemPrompt, userPrompt, null);
        String text = complete.getText();
        return new SimpleOutcome(text);
    }

    private String buildUserPrompt(SkillContext ctx) {
        return "用户事实：" + ctx.getL0Context() + "\n\n用户消息：" + ctx.getUserText();
    }

    private record SimpleOutcome(String text) implements SkillOutcome {}
}
