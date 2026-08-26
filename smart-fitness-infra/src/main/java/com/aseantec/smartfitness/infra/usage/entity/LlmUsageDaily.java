package com.aseantec.smartfitness.infra.usage.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;

/**
 * 按日聚合的用量。对应表 {@code llm_usage_daily}。
 * <p>由 {@link com.aseantec.smartfitness.infra.usage.UsageRecorder} 从调用行滚出来，无 FK。给 {@code GET /v1/me/usage}。
 * 本表无 {@code created_at}，不继承 {@link com.aseantec.smartfitness.common.entity.BaseEntity}。
 */
@Data
@TableName("llm_usage_daily")
public class LlmUsageDaily {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long athleteId;
    private LocalDate usageDate;

    /** {@code SYSTEM} | {@code BYOK}。与日期、运动员组成唯一键。 */
    private String keySource;

    private Integer callCount;
    private Integer failCount;
    private Long promptTokens;
    private Long completionTokens;
    private Long totalTokens;
    private Integer platformCostMinor;
    private Integer byokEstCostMinor;
}
