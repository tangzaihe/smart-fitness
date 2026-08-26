package com.aseantec.smartfitness.coach.entity;

import com.aseantec.smartfitness.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户对处方的一次决定审计。对应表 {@code decision_event}。
 * <p>ACCEPT 幂等：已 ACCEPTED 再 ACCEPT 不新建本行以外的课次。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("decision_event")
public class DecisionEvent extends BaseEntity {

    private Long adviceId;
    private Long athleteId;

    /** {@code ACCEPT} | {@code REST} | {@code REJECT}。 */
    private String action;

    /** 用户备注，可空。 */
    private String note;
}
