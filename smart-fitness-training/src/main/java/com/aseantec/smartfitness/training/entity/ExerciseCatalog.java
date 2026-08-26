package com.aseantec.smartfitness.training.entity;

import com.aseantec.smartfitness.infra.mybatis.JsonbStringTypeHandler;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import org.apache.ibatis.type.JdbcType;

/**
 * 动作字典。对应表 {@code exercise_catalog}。
 * <p>Retrieve 与 {@code set_log.exercise_code} 都认 {@code code}，禁止模型自造编码。
 */
@Data
@TableName(value = "exercise_catalog", autoResultMap = true)
public class ExerciseCatalog {

    @TableId
    private Long id;

    /** 稳定业务键，如 {@code BARBELL_SQUAT}。 */
    private String code;

    private String name;

    /** 主肌群，如 {@code QUAD} | {@code CHEST}。 */
    private String muscleGroup;

    /** 所需器材，如 {@code BARBELL}。 */
    private String equipment;

    /** 动作模式，如 {@code SQUAT} | {@code HINGE} | {@code VERTICAL_PUSH}。护栏按此排除。 */
    private String pattern;

    /** 可替换分组；处方的 alternatives 必须同组。 */
    private String swapGroup;

    /** JSON 标签数组。 */
    @TableField(jdbcType = JdbcType.OTHER, typeHandler = JsonbStringTypeHandler.class)
    private String tags;

    /** 是否拉伸/恢复动作。 */
    private Boolean isStretch;
}
