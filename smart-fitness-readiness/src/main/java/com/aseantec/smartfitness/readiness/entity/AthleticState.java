package com.aseantec.smartfitness.readiness.entity;

import com.aseantec.smartfitness.common.entity.BaseEntity;
import com.aseantec.smartfitness.infra.mybatis.JsonbStringTypeHandler;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.ibatis.type.JdbcType;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * 准备度快照。对应表 {@code athletic_state}。
 * <p>只插入、不更新历史。完课或写入当日 wellness 后追加一行；身体 Tab 读最新 {@code asOf}。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "athletic_state", autoResultMap = true)
public class AthleticState extends BaseEntity {

    private Long athleteId;

    /** 快照时刻（UTC）。 */
    private OffsetDateTime asOf;

    /** 准备度 0–100，规则引擎 v1 产出，非 LLM。 */
    private Integer readiness;

    /** 恢复分，可空。 */
    private Integer recovery;

    /** JSON：肌群 → 疲劳分，完课由组数据回写。 */
    @TableField(jdbcType = JdbcType.OTHER, typeHandler = JsonbStringTypeHandler.class)
    private String fatigueByMuscle;

    private BigDecimal sleepHours;

    /** {@code RULE} | {@code MANUAL} | {@code DEVICE}。P0 为 {@code RULE}。 */
    private String source;

    /** 公式版本，如 {@code v1}。前端必须原样展示。 */
    private String calcVersion;
}
