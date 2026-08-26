package com.aseantec.smartfitness.common.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.OffsetDateTime;

/**
 * 可更新档案/账号行，在 {@link BaseEntity} 上增加 {@code updated_at}。
 * <p>快照类表（{@code athletic_state}、{@code coach_event}）不要用本类。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public abstract class AuditedEntity extends BaseEntity {

    /** UTC 最后更新时间，插入与更新时由 MetaObjectHandler 填充。 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private OffsetDateTime updatedAt;
}
