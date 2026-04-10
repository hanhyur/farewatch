package com.farewatch.api.common;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 웹 레이어 공통 설정. CORS 만 정의한다.
 *
 * <p>v0 단계에서는 로컬 개발 호스트({@code http://localhost:3000}) 한 곳만 허용한다.
 * 운영 도메인 추가 시에는 {@code allowedOriginPatterns} 를 환경 프로퍼티로 분리한다.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins("http://localhost:3000")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(false)
                .maxAge(3600);
    }
}
