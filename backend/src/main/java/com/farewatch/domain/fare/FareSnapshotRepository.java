package com.farewatch.domain.fare;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FareSnapshotRepository extends JpaRepository<FareSnapshot, Long> {

    /** 특정 노선+출발일의 스냅샷들을 최신순으로 반환 (가격 추이 차트). */
    List<FareSnapshot> findByRouteIdAndDepartureDateOrderByCollectedAtDesc(
            Long routeId, LocalDate departureDate);

    /** 출발일 범위 조회 (차트 X축이 출발일인 뷰에서 사용). */
    List<FareSnapshot> findByRouteIdAndDepartureDateBetween(
            Long routeId, LocalDate from, LocalDate to);

    /** 특정 노선+출발일의 가장 최근 스냅샷 하나 (판단 API 에서 currentPrice로 사용). */
    Optional<FareSnapshot> findTopByRouteIdAndDepartureDateOrderByCollectedAtDesc(
            Long routeId, LocalDate departureDate);

    /** 통계 계산 전 충분한 표본이 있는지 확인. */
    long countByRouteIdAndDepartureDate(Long routeId, LocalDate departureDate);
}
