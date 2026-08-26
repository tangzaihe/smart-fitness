package com.aseantec.smartfitness.common.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.time.OffsetDateTime;

/**
 * 只追加或极少更新的行：雪花主键 + 创建时间。
 * <p>子类不要重复注释 {@code id}/{@code createdAt}。需要 {@code updated_at} 时用 {@link AuditedEntity}。
 */
@Data
public abstract class BaseEntity {

    /** 应用侧雪花 ID，非数据库自增。 */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** UTC 插入时间，MetaObjectHandler 填充。 */
    @TableField(fill = FieldFill.INSERT)
    private OffsetDateTime createdAt;
}
