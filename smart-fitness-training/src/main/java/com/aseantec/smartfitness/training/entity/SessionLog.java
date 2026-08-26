package com.aseantec.smartfitness.training.entity;

import com.aseantec.smartfitness.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.OffsetDateTime;

/**
 * 一堂训练课。对应表 {@code session_log}。
 * <p>P0 仅在用户 ACCEPT 课表/减载类 advice 后创建。LLM 不得直接插入本表。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("session_log")
public class SessionLog extends BaseEntity {

    private Long athleteId;

    /** 来源处方；P0 路径非空。 */
    private Long adviceId;

    /** {@code IN_PROGRESS} | {@code COMPLETED} | {@code ABANDONED}。 */
    private String status;

    private OffsetDateTime startedAt;

    /** 完课或放弃时写入。 */
    private OffsetDateTime endedAt;

    /** 整课 RPE 1–10，完课可选。 */
    private Integer perceivedExertion;

    private String notes;
}
