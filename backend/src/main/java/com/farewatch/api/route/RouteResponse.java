package com.farewatch.api.route;

import com.farewatch.domain.route.Route;

/**
 * 노선 목록/상세 API 응답. 도메인 {@code Route} 를 외부 노출에 적합한 형태로 평탄화.
 */
public record RouteResponse(
        Long id,
        String origin,
        String destination,
        String airlineCode,
        boolean active) {

    public static RouteResponse from(Route route) {
        return new RouteResponse(
                route.getId(),
                route.getOrigin().value(),
                route.getDestination().value(),
                route.getAirlineCode(),
                route.isActive());
    }
}
