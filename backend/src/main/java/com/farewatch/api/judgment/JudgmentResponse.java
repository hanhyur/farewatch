package com.farewatch.api.judgment;

import com.farewatch.domain.shared.FareVerdictKind;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 노선의 "지금 사야 하는가" 판단 결과 응답. 프론트엔드 메인 카드와 노선 상세 화면이 동일
 * payload 를 사용한다.
 *
 * <p>Insufficient 케이스에서는 통계가 없거나 가격 정보가 부족할 수 있어 일부 필드가 null
 * 일 수 있다. 클라이언트는 verdict 값을 먼저 분기 처리하는 것이 안전.
 */
public record JudgmentResponse(
        Long routeId,
        LocalDate departureDate,
        FareVerdictKind verdict,
        Long currentPrice,
        Long avgPrice,
        Long minPrice,
        Long maxPrice,
        Double stdDeviation,
        Double zScore,
        Integer sampleCount,
        String suggestion,
        LocalDateTime calculatedAt) {}
