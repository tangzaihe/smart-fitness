package com.aseantec.smartfitness.athlete.entity;

import com.aseantec.smartfitness.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 每日主观恢复输入。对应表 {@code wellness_log}。
 * <p>每人每天一行（UNIQUE athlete+date）。准备度 v1 的睡眠/疲劳来源；无当日记录可用昨日。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wellness_log")
public class WellnessLog extends BaseEntity {

    private Long athleteId;

    /** 业务日（用户本地日历日，P0 按服务端日期）。 */
    private LocalDate logDate;

    /** 睡眠小时。 */
    private BigDecimal sleepHours;

    /** 主观疲劳 1–10，越高越累。 */
    private Integer subjectiveFatigue;
}
