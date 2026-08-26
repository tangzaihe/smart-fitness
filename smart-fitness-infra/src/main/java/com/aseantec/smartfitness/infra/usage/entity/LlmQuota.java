package com.aseantec.smartfitness.infra.usage.entity;

import com.aseantec.smartfitness.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Token/费用配额。对应表 {@code llm_quota}。
 * <p>{@code athleteId} 为空表示全局默认（V3 种子 DAY）。非空为该运动员覆盖。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("llm_quota")
public class LlmQuota extends BaseEntity {

    /** 空 = 全局配额。 */
    private Long athleteId;

    /** {@code DAY} | {@code MONTH}。 */
    private String period;

    private Long tokenLimit;

    /** 费用上限，分；可空表示不限费用。 */
    private Integer costLimitMinor;

    private Integer rpmLimit;
}
