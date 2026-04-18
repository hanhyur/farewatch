package com.farewatch.api.fare;

import com.farewatch.domain.fare.FareStatistics;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 누적 통계 응답. 역대 최저가 정보 포함. */
public record FareStatisticsResponse(
        Long routeId,
        LocalDate departureDate,
        long avgPrice,
        long minPrice,
        long maxPrice,
        double stdDeviation,
        int sampleCount,
        Long p25Price,
        Long p75Price,
        LocalDateTime calculatedAt,
        Long allTimeLowPrice,
        LocalDateTime allTimeLowDate) {

    public static FareStatisticsResponse from(FareStatistics s) {
        return new FareStatisticsResponse(
                s.getRouteId(),
                s.getDepartureDate(),
                s.getAvgPrice(),
                s.getMinPrice(),
                s.getMaxPrice(),
                s.getStdDeviation(),
                s.getSampleCount(),
                s.getP25Price(),
                s.getP75Price(),
                s.getCalculatedAt(),
                null,
                null);
    }

    public FareStatisticsResponse withAllTimeLow(Long price, LocalDateTime date) {
        return new FareStatisticsResponse(
                routeId, departureDate, avgPrice, minPrice, maxPrice,
                stdDeviation, sampleCount, p25Price, p75Price, calculatedAt,
                price, date);
    }
}
