package com.farewatch.api.common;

/**
 * 요청한 리소스가 존재하지 않을 때 발생. {@link GlobalExceptionHandler} 가 404 로 매핑.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
