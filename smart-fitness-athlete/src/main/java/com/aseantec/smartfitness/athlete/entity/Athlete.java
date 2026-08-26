package com.aseantec.smartfitness.athlete.entity;

import com.aseantec.smartfitness.common.entity.AuditedEntity;
import com.aseantec.smartfitness.infra.mybatis.JsonbStringTypeHandler;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.ibatis.type.JdbcType;

import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * 运动员档案，业务主体。对应表 {@code athlete}。
 * <p>{@code onboardedAt} 为空表示尚未完成 {@code PUT /v1/athlete/me}，教练回合会拒绝。
 * 器材/偏好在 JSON 列；伤痛在 {@link AthleteConstraint}。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "athlete", autoResultMap = true)
public class Athlete extends AuditedEntity {

    /** {@code app_user.id}，UNIQUE。 */
    private Long userId;

    /** 展示名。 */
    private String displayName;

    /** {@code MALE} | {@code FEMALE} | {@code OTHER}，可空。 */
    private String sex;

    private LocalDate birthDate;

    /** 身高厘米。 */
    private java.math.BigDecimal heightCm;

    /** 训练目标，如 {@code HYPERTROPHY} | {@code FAT_LOSS} | {@code STRENGTH}。 */
    private String goal;

    /** 每周可练下限（分钟），Retrieve 与排课用。 */
    private Integer weeklyMin;

    /** JSON 数组：可用器材编码，如 {@code ["BARBELL","DUMBBELL"]}。 */
    @TableField(jdbcType = JdbcType.OTHER, typeHandler = JsonbStringTypeHandler.class)
    private String equipment;

    /** JSON 对象：{@code liked}/{@code disliked}/{@code never} 动作编码列表。 */
    @TableField(jdbcType = JdbcType.OTHER, typeHandler = JsonbStringTypeHandler.class)
    private String preferences;

    /** 首次完成建档的时间；空则未 onboard。 */
    private OffsetDateTime onboardedAt;
}
