package com.aseantec.smartfitness.coach.agent;

import com.aseantec.smartfitness.coach.entity.CoachPolicyVersion;
import com.aseantec.smartfitness.coach.mapper.CoachPolicyVersionMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 按 Skill 意图选取 system prompt。
 * <p>{@code today_session} 复用 {@code coach_policy_version} 的 ACTIVE system prompt（含 JSON 处方约束）；
 * {@code chat} 用内置中文私教 prompt；其余意图默认回退到 chat。
 */
@Component
@RequiredArgsConstructor
public class CoachPolicyService {

    private static final String CHAT_PROMPT = """
            你是 Smart Fitness 的私教，像用户常去健身房遇见的资深教练。
            用中文自然对话，简短、亲切、专业。不要诊断伤病，不要编造动作编码。
            结合给定的用户事实（档案/准备度/约束/进行中课次）给出有依据的建议。
            不要输出 JSON，除非用户明确要排今日训练。""";

    private final CoachPolicyVersionMapper policyVersionMapper;

    /** 返回 ACTIVE 的 policy version；{@code today_session} Skill 用其 systemPrompt。 */
    public CoachPolicyVersion requireActive() {
        CoachPolicyVersion policy = policyVersionMapper.selectOne(new LambdaQueryWrapper<CoachPolicyVersion>()
                .eq(CoachPolicyVersion::getStatus, "ACTIVE")
                .orderByDesc(CoachPolicyVersion::getVersion)
                .last("LIMIT 1"));
        if (policy == null) {
            throw new IllegalStateException("no active coach policy");
        }
        return policy;
    }

    /** 按意图返回 system prompt。 */
    public String promptFor(String intent) {
        if ("TODAY_SESSION".equals(intent) || "WORKOUT_PLANNING".equals(intent)) {
            return requireActive().getSystemPrompt();
        }
        return CHAT_PROMPT;
    }
}
