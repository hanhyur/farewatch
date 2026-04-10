package com.farewatch.api.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;

/**
 * 공통 API 응답 envelope. 모든 컨트롤러 응답은 이 형태로 감싼다.
 *
 * <p>{@code success=true} 인 응답은 {@code data} 만 채워지고 {@code error} 는 null —
 * Jackson 직렬화 시 {@code null} 필드는 생략되어 클라이언트가 보는 JSON 은 더 깔끔해진다
 * ({@link JsonInclude.Include#NON_NULL}).
 *
 * <p>{@code timestamp} 는 응답 생성 시각 (UTC ISO-8601). 디버깅·로그 상관용.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(boolean success, T data, String error, Instant timestamp) {

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, null, Instant.now());
    }

    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(false, null, message, Instant.now());
    }
}
