package com.farewatch.api.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 전역 예외 핸들러. 모든 컨트롤러에서 발생한 예외를 {@link ApiResponse#error} envelope 로
 * 변환한다.
 *
 * <p><b>로그 정책</b>: 4xx 는 warn, 5xx 는 error. 4xx 는 클라이언트의 잘못된 요청이 흔하
 * 므로 stack trace 없이 메시지만 남기고, 5xx 는 진단을 위해 stack trace 포함.
 *
 * <p><b>메시지 정책</b>: 검증/도메인 예외({@link IllegalArgumentException},
 * {@link ResourceNotFoundException}) 는 메시지를 그대로 노출 — 도메인 메시지가 안전한
 * 검증 결과이고 사용자 친화적이기 때문. 그 외 일반 {@link Exception} 은 내부 메시지를
 * 노출하지 않고 generic 메시지로 응답.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(ResourceNotFoundException ex) {
        log.warn("404 Not Found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadRequest(IllegalArgumentException ex) {
        log.warn("400 Bad Request: {}", ex.getMessage());
        return ResponseEntity.badRequest().body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
        String message =
                ex.getBindingResult().getFieldErrors().stream()
                        .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                        .reduce((a, b) -> a + ", " + b)
                        .orElse("validation failed");
        log.warn("400 Validation: {}", message);
        return ResponseEntity.badRequest().body(ApiResponse.error(message));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneric(Exception ex) {
        log.error("500 Internal Server Error", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("internal server error"));
    }
}
