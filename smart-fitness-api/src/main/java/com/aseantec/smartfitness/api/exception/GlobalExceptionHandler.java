package com.aseantec.smartfitness.api.exception;

import com.aseantec.smartfitness.common.exception.BizException;
import com.aseantec.smartfitness.common.exception.ErrorCode;
import com.aseantec.smartfitness.common.result.Result;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BizException.class)
    public ResponseEntity<Result<Void>> handleBiz(BizException ex) {
        ErrorCode code = ex.getErrorCode();
        return ResponseEntity.status(code.getHttpStatus())
                .contentType(MediaType.APPLICATION_JSON)
                .body(Result.fail(code, ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<Void>> handleValid(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .filter(msg -> msg != null && !msg.isBlank())
                .collect(Collectors.joining("; "));
        if (message.isBlank()) {
            message = ErrorCode.BAD_REQUEST.getMessage();
        }
        return ResponseEntity.badRequest()
                .contentType(MediaType.APPLICATION_JSON)
                .body(Result.fail(ErrorCode.BAD_REQUEST, message));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Result<Void>> handleConstraint(ConstraintViolationException ex) {
        return ResponseEntity.badRequest()
                .contentType(MediaType.APPLICATION_JSON)
                .body(Result.fail(ErrorCode.BAD_REQUEST, ex.getMessage()));
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<Result<Void>> handleDataAccess(DataAccessException ex) {
        log.error("unhandled db error", ex);
        Throwable root = ex.getMostSpecificCause();
        String detail = root != null && root.getMessage() != null ? root.getMessage() : ex.getMessage();
        return ResponseEntity.internalServerError()
                .contentType(MediaType.APPLICATION_JSON)
                .body(Result.fail(ErrorCode.SYSTEM, detail));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleUnknown(Exception ex) {
        log.error("unhandled error", ex);
        return ResponseEntity.internalServerError()
                .contentType(MediaType.APPLICATION_JSON)
                .body(Result.fail(ErrorCode.SYSTEM, ex.getMessage()));
    }
}
