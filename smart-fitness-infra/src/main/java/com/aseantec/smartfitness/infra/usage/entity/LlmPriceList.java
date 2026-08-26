package com.aseantec.smartfitness.infra.usage.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;

/**
 * 模型单价。对应表 {@code llm_price_list}。
 * <p>按 provider+model+生效日查找，无 FK。金额单位为分。本表无 {@code created_at}，不继承 {@link com.aseantec.smartfitness.common.entity.BaseEntity}。
 */
@Data
@TableName("llm_price_list")
public class LlmPriceList {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String provider;
    private String model;

    /** 输入每 1k token 的分。 */
    @TableField("input_per_1k_minor")
    private Integer inputPer1kMinor;

    @TableField("output_per_1k_minor")
    private Integer outputPer1kMinor;

    @TableField("cached_per_1k_minor")
    private Integer cachedPer1kMinor;
    private String currency;
    private LocalDate effectiveFrom;
}
