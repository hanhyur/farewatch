package com.farewatch.api.common;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI 메타데이터. springdoc 가 자동 생성하는 스펙에 제목/버전/설명을 덮어씌운다.
 *
 * <p>Swagger UI: {@code /swagger-ui.html} <br>
 * Raw OpenAPI JSON: {@code /v3/api-docs}
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI farewatchOpenAPI() {
        return new OpenAPI()
                .info(
                        new Info()
                                .title("FareWatch API")
                                .version("v1")
                                .description(
                                        """
                                        항공권 가격 모니터링 + 구매 시점 판단 백엔드.

                                        ## 핵심 흐름
                                        1. 스케줄러가 주기적으로 노선별 가격 스냅샷 수집
                                        2. 누적 통계 자동 재계산
                                        3. Rule Engine 이 현재 가격을 평균 ± σ 구간으로 판정
                                        4. 사용자 알림 규칙과 매칭되면 발송 (7일 중복 차단)

                                        모든 응답은 공통 envelope 형태:
                                        ```json
                                        { "success": true, "data": { ... }, "timestamp": "..." }
                                        ```
                                        """)
                                .contact(
                                        new Contact()
                                                .name("FareWatch")
                                                .url("https://github.com/hanhyur/farewatch"))
                                .license(new License().name("MIT")))
                .servers(
                        List.of(
                                new Server()
                                        .url("http://localhost:8080")
                                        .description("Local development")));
    }
}
