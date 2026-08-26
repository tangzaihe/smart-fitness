package com.aseantec.smartfitness.training.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 课内一组。对应表 {@code set_log}。
 * <p>重量/次数由用户 PATCH，不向 LLM 询问。{@code exerciseCode} 逻辑关联 {@code exercise_catalog.code}。
 */
@Data
@TableName("set_log")
public class SetLog {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** {@code session_log.id}。 */
    private Long sessionId;

    /** 动作编码，必须已在目录中。 */
    private String exerciseCode;

    /** 冗余肌群，完课回写疲劳用。 */
    private String muscleGroup;

    /** 组序，从 1 起。 */
    private Integer setIndex;

    private Integer reps;

    /** 负荷千克。 */
    private BigDecimal loadKg;

    /** 本组 RPE。 */
    private BigDecimal rpe;

    /** 用户是否勾选完成。 */
    private Boolean completed;
}
