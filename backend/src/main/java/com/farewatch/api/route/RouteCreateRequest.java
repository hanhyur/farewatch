package com.farewatch.api.route;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 노선 생성 요청. 출발/도착 IATA 코드 필수, 항공사 코드 선택.
 */
public record RouteCreateRequest(
        @NotBlank @Size(min = 3, max = 3) String origin,
        @NotBlank @Size(min = 3, max = 3) String destination,
        @Size(max = 10) String airlineCode) {}
