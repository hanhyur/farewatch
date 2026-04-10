package com.farewatch.api.fare;

import com.farewatch.domain.fare.FareSnapshot;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 단일 가격 스냅샷 응답. 차트 라인 한 점에 해당. */
public record FareSnapshotResponse(
        Long id,
        Long routeId,
        LocalDate departureDate,
        LocalDateTime collectedAt,
        long price,
        String currency,
        String source) {

    public static FareSnapshotResponse from(FareSnapshot s) {
        return new FareSnapshotResponse(
                s.getId(),
                s.getRouteId(),
                s.getDepartureDate(),
                s.getCollectedAt(),
                s.getPrice().amount(),
                s.getPrice().currency(),
                s.getSource());
    }
}
