package com.aseantec.smartfitness.auth.entity;

import com.aseantec.smartfitness.common.entity.AuditedEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * App 登录身份。对应表 {@code app_user}，与 {@code athlete} 1:1。
 * <p>JWT subject 为本表 {@code id}；训练/教练数据一律挂在 athlete 上，不挂本表。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("app_user")
public class AppUser extends AuditedEntity {

    /** 登录邮箱，全局唯一，入库前小写。 */
    private String email;

    /** 可选手机号；非空时唯一。 */
    private String phone;

    /** BCrypt 哈希，禁止日志打印。 */
    private String passwordHash;

    /** {@code ACTIVE} 可登录；其它值拒绝发令牌。 */
    private String status;
}
