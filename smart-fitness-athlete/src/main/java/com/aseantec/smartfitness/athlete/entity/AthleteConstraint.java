package com.aseantec.smartfitness.athlete.entity;

import com.aseantec.smartfitness.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 伤痛/医疗约束。对应表 {@code athlete_constraint}。
 * <p>Observe 每回合读取未过期行；高严重度可把处方压成 REST。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("athlete_constraint")
public class AthleteConstraint extends BaseEntity {

    private Long athleteId;

    /** 约束类型，如 {@code PAIN} | {@code MEDICAL} | {@code PREFERENCE}。 */
    private String type;

    /** 部位，如 {@code SHOULDER} | {@code KNEE} | {@code LOWER_BACK}。 */
    private String bodyPart;

    /** 严重度 1–5；护栏 G1 使用。 */
    private Integer severity;

    private LocalDate startsOn;

    /** 空表示仍有效。 */
    private LocalDate endsOn;

    private String notes;
}
