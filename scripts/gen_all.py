# -*- coding: utf-8 -*-
from pathlib import Path

ROOT = Path(r"E:\aseantec\agent\Smart Fitness")


def w(rel: str, text: str) -> None:
    path = ROOT / rel.replace("/", "\\")
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(text.lstrip("\n"), encoding="utf-8")
    print("W", rel)


# ===================== COMMON =====================
w("smart-fitness-common/src/main/java/com/aseantec/smartfitness/common/result/Result.java", """
package com.aseantec.smartfitness.common.result;

import com.aseantec.smartfitness.common.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> {
    private int code;
    private String message;
    private T data;

    public static <T> Result<T> ok(T data) {
        return new Result<>(ErrorCode.OK.getCode(), "ok", data);
    }

    public static Result<Void> ok() {
        return ok(null);
    }

    public static <T> Result<T> fail(ErrorCode errorCode) {
        return new Result<>(errorCode.getCode(), errorCode.getMessage(), null);
    }

    public static <T> Result<T> fail(ErrorCode errorCode, String message) {
        return new Result<>(errorCode.getCode(), message, null);
    }
}
""")

w("smart-fitness-common/src/main/java/com/aseantec/smartfitness/common/exception/ErrorCode.java", """
package com.aseantec.smartfitness.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
    OK(0, HttpStatus.OK, "ok"),
    BAD_REQUEST(1001, HttpStatus.BAD_REQUEST, "parameter validation failed"),
    SCHEMA_INVALID(1002, HttpStatus.BAD_REQUEST, "json schema mismatch"),
    UNAUTHORIZED(2001, HttpStatus.UNAUTHORIZED, "unauthorized"),
    TOKEN_EXPIRED(2002, HttpStatus.UNAUTHORIZED, "token expired"),
    LOGIN_LOCKED(2003, HttpStatus.TOO_MANY_REQUESTS, "login locked"),
    FORBIDDEN(2004, HttpStatus.FORBIDDEN, "forbidden"),
    NOT_ONBOARDED(3001, HttpStatus.CONFLICT, "onboarding incomplete"),
    SESSION_ACTIVE(3002, HttpStatus.CONFLICT, "session already in progress"),
    ADVICE_NOT_DECIDABLE(3003, HttpStatus.CONFLICT, "advice cannot be confirmed"),
    SESSION_STATE(3004, HttpStatus.CONFLICT, "session status does not allow this action"),
    QUOTA_EXCEEDED(3005, HttpStatus.TOO_MANY_REQUESTS, "daily token quota exceeded"),
    RPM_EXCEEDED(3006, HttpStatus.TOO_MANY_REQUESTS, "rpm exceeded"),
    RUN_ACTIVE(3007, HttpStatus.CONFLICT, "coach run already in progress"),
    GUARDRAIL_REJECT(3008, HttpStatus.UNPROCESSABLE_ENTITY, "guardrail rejected session prescription"),
    NOT_FOUND(4001, HttpStatus.NOT_FOUND, "resource not found"),
    SYSTEM(5001, HttpStatus.INTERNAL_SERVER_ERROR, "system error"),
    LLM_UPSTREAM(5002, HttpStatus.BAD_GATEWAY, "llm provider failed");

    private final int code;
    private final HttpStatus httpStatus;
    private final String message;

    ErrorCode(int code, HttpStatus httpStatus, String message) {
        this.code = code;
        this.httpStatus = httpStatus;
        this.message = message;
    }
}
""")

w("smart-fitness-common/src/main/java/com/aseantec/smartfitness/common/exception/BizException.java", """
package com.aseantec.smartfitness.common.exception;

import lombok.Getter;

@Getter
public class BizException extends RuntimeException {
    private final ErrorCode errorCode;

    public BizException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public BizException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
""")

w("smart-fitness-common/src/main/java/com/aseantec/smartfitness/common/context/CurrentUser.java", """
package com.aseantec.smartfitness.common.context;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CurrentUser {
    Long userId;
    Long athleteId;
    String audience;
    String tokenId;
    boolean admin;
}
""")

w("smart-fitness-common/src/main/java/com/aseantec/smartfitness/common/context/UserContext.java", """
package com.aseantec.smartfitness.common.context;

import com.aseantec.smartfitness.common.exception.BizException;
import com.aseantec.smartfitness.common.exception.ErrorCode;

public final class UserContext {
    private static final ThreadLocal<CurrentUser> HOLDER = new ThreadLocal<>();

    private UserContext() {
    }

    public static void set(CurrentUser user) {
        HOLDER.set(user);
    }

    public static CurrentUser get() {
        return HOLDER.get();
    }

    public static CurrentUser require() {
        CurrentUser user = HOLDER.get();
        if (user == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        return user;
    }

    public static Long requireAthleteId() {
        CurrentUser user = require();
        if (user.getAthleteId() == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        return user.getAthleteId();
    }

    public static void clear() {
        HOLDER.remove();
    }
}
""")

w("smart-fitness-common/src/main/java/com/aseantec/smartfitness/common/constant/RedisKeys.java", """
package com.aseantec.smartfitness.common.constant;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public final class RedisKeys {
    private static final DateTimeFormatter DAY = DateTimeFormatter.BASIC_ISO_DATE;

    private RedisKeys() {
    }

    public static String loginFail(String account) {
        return "auth:login:fail:" + account.toLowerCase();
    }

    public static String jwtBlacklist(String jti) {
        return "auth:jwt:blacklist:" + jti;
    }

    public static String systemUsage(Long athleteId, LocalDate date) {
        return "usage:sys:" + athleteId + ":" + date.format(DAY);
    }

    public static String rpm(Long athleteId, String minuteBucket) {
        return "usage:rpm:" + athleteId + ":" + minuteBucket;
    }
}
""")

w("smart-fitness-common/src/main/java/com/aseantec/smartfitness/common/entity/BaseEntity.java", """
package com.aseantec.smartfitness.common.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
public abstract class BaseEntity {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    @TableField(fill = FieldFill.INSERT)
    private OffsetDateTime createdAt;
}
""")

w("smart-fitness-common/src/main/java/com/aseantec/smartfitness/common/entity/AuditedEntity.java", """
package com.aseantec.smartfitness.common.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.OffsetDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
public abstract class AuditedEntity extends BaseEntity {
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private OffsetDateTime updatedAt;
}
""")

w("smart-fitness-common/src/main/java/com/aseantec/smartfitness/common/port/llm/LlmRequest.java", """
package com.aseantec.smartfitness.common.port.llm;

import lombok.Builder;
import lombok.Value;

import java.util.Map;

@Value
@Builder
public class LlmRequest {
    Long athleteId;
    Long runId;
    String purpose;
    String systemPrompt;
    String userPrompt;
    String model;
    Map<String, Object> context;
}
""")

w("smart-fitness-common/src/main/java/com/aseantec/smartfitness/common/port/llm/LlmCompleteEvent.java", """
package com.aseantec.smartfitness.common.port.llm;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class LlmCompleteEvent {
    String text;
    int promptTokens;
    int completionTokens;
    String usageSource;
    String keySource;
    String model;
    String provider;
}
""")

w("smart-fitness-common/src/main/java/com/aseantec/smartfitness/common/port/llm/LlmStreamListener.java", """
package com.aseantec.smartfitness.common.port.llm;

public interface LlmStreamListener {
    void onToken(String delta);

    void onComplete(LlmCompleteEvent event);

    void onError(Throwable error);
}
""")

w("smart-fitness-common/src/main/java/com/aseantec/smartfitness/common/port/llm/LlmGateway.java", """
package com.aseantec.smartfitness.common.port.llm;

public interface LlmGateway {
    void chatStream(LlmRequest request, LlmStreamListener listener);
}
""")

w("smart-fitness-common/src/main/java/com/aseantec/smartfitness/common/vo/PageResult.java", """
package com.aseantec.smartfitness.common.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageResult<T> {
    private List<T> items;
    private long total;
    private long page;
    private long size;
}
""")

print("common ok")
