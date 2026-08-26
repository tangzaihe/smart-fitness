package com.aseantec.smartfitness.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 与 08 契约对齐的业务错误码。{@code code} 为响应 JSON 的数字码。
 */
@Getter
public enum ErrorCode {
    OK(0, HttpStatus.OK, "ok"),
    BAD_REQUEST(1001, HttpStatus.BAD_REQUEST, "parameter validation failed"),
    SCHEMA_INVALID(1002, HttpStatus.BAD_REQUEST, "json schema mismatch"),
    UNAUTHORIZED(2001, HttpStatus.UNAUTHORIZED, "unauthorized"),
    /** access token 过期，客户端应走 refresh。 */
    TOKEN_EXPIRED(2002, HttpStatus.UNAUTHORIZED, "token expired"),
    /** 登录失败次数过多。 */
    LOGIN_LOCKED(2003, HttpStatus.TOO_MANY_REQUESTS, "login locked"),
    FORBIDDEN(2004, HttpStatus.FORBIDDEN, "forbidden"),
    /** 未完成 PUT /v1/athlete/me。 */
    NOT_ONBOARDED(3001, HttpStatus.CONFLICT, "onboarding incomplete"),
    /** 已有 IN_PROGRESS 课次。 */
    SESSION_ACTIVE(3002, HttpStatus.CONFLICT, "session already in progress"),
    /** advice 非 PENDING（重复 ACCEPT 由 Service 幂等处理，不抛本码）。 */
    ADVICE_NOT_DECIDABLE(3003, HttpStatus.CONFLICT, "advice cannot be confirmed"),
    SESSION_STATE(3004, HttpStatus.CONFLICT, "session status does not allow this action"),
    QUOTA_EXCEEDED(3005, HttpStatus.TOO_MANY_REQUESTS, "daily token quota exceeded"),
    RPM_EXCEEDED(3006, HttpStatus.TOO_MANY_REQUESTS, "rpm exceeded"),
    /** 已有 RUNNING 的 coach_run。 */
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
