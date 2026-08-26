package com.aseantec.smartfitness.infra.usage.entity;

import com.aseantec.smartfitness.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 一次真实 LLM HTTP 调用的账本。对应表 {@code llm_call_usage}。
 * <p>无 usage 时仍写 ESTIMATED，禁止 token 列为 null。Fake 提供商也记账以扣日配额。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("llm_call_usage")
public class LlmCallUsage extends BaseEntity {

    private Long athleteId;

    /** 可空：非教练回合的调用。 */
    private Long runId;

    private Integer callSeq;

    /** 如 {@code COACH_RUN}。 */
    private String purpose;

    /** {@code SYSTEM} | {@code BYOK}。 */
    private String keySource;

    private String billedTo;
    private Long credentialId;
    private String keyFingerprint;
    private String provider;
    private String model;
    private Integer promptTokens;
    private Integer completionTokens;
    private Integer cachedTokens;
    private Integer reasoningTokens;
    private Integer totalTokens;

    /** {@code PROVIDER} | {@code ESTIMATED}。 */
    private String usageSource;

    /** 估算费用，分。 */
    private Integer estimatedCostMinor;
    private String currency;
    private Integer durationMs;
    private Integer httpStatus;

    /** {@code SUCCESS} | {@code FAIL}。 */
    private String status;
    private String errorCode;
}
