package com.farewatch.domain.fare;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FareSnapshotRepository extends JpaRepository<FareSnapshot, Long> {

    /**
     * 특정 노선+출발일의 스냅샷들을 최신순으로 반환. {@code Pageable} 필수 —
     * 한 (route, departureDate) 쌍에 대해 수십~수백 행이 쌓일 수 있으므로
     * 무제한 조회는 금지.
     */
    List<FareSnapshot> findByRouteIdAndDepartureDateOrderByCollectedAtDesc(
            Long routeId, LocalDate departureDate, Pageable pageable);

    /**
     * 출발일 범위 조회 (차트 X축이 출발일인 뷰에서 사용). 범위가 넓어질수록 결과가
     * 폭발할 수 있으므로 {@code Pageable} 로 상한 강제.
     */
    List<FareSnapshot> findByRouteIdAndDepartureDateBetween(
            Long routeId, LocalDate from, LocalDate to, Pageable pageable);

    /**
     * 특정 노선+출발일의 가장 최근 스냅샷 하나 (판단 API 에서 currentPrice로 사용).
     * 이미 1건으로 제한되므로 pagination 불필요.
     */
    Optional<FareSnapshot> findTopByRouteIdAndDepartureDateOrderByCollectedAtDesc(
            Long routeId, LocalDate departureDate);

    /** 통계 계산 전 충분한 표본이 있는지 확인. 집계 count 이므로 pagination 불필요. */
    long countByRouteIdAndDepartureDate(Long routeId, LocalDate departureDate);

    /**
     * 통계 재계산 용 가격 컬럼 추출 쿼리. 엔티티 전체가 아니라 {@code price.amount}
     * 컬럼만 가져와 메모리·네트워크 부담을 최소화한다.
     *
     * <p>한 (route, departureDate) 쌍의 누적 스냅샷 수는 수집 빈도(6h) × 모니터링
     * 기간 으로 상한이 분명하므로 (현실적 최대 수천 건) 페이징 없이 전체 조회한다.
     */
    @Query(
            "SELECT s.price.amount FROM FareSnapshot s "
                    + "WHERE s.routeId = :routeId AND s.departureDate = :departureDate")
    List<Long> findPricesByRouteIdAndDepartureDate(
            @Param("routeId") Long routeId, @Param("departureDate") LocalDate departureDate);

    /** 특정 노선+출발일의 역대 최저가 스냅샷. */
    @Query(
            "SELECT s FROM FareSnapshot s "
                    + "WHERE s.routeId = :routeId AND s.departureDate = :departureDate "
                    + "ORDER BY s.price.amount ASC, s.collectedAt ASC")
    List<FareSnapshot> findAllTimeLowest(
            @Param("routeId") Long routeId,
            @Param("departureDate") LocalDate departureDate,
            Pageable pageable);

    /** 특정 노선+출발일에서 수집 시각 기준으로 기간 필터 조회. */
    @Query(
            "SELECT s FROM FareSnapshot s "
                    + "WHERE s.routeId = :routeId AND s.departureDate = :departureDate "
                    + "AND s.collectedAt >= :since "
                    + "ORDER BY s.collectedAt ASC")
    List<FareSnapshot> findByRouteIdAndDepartureDateSince(
            @Param("routeId") Long routeId,
            @Param("departureDate") LocalDate departureDate,
            @Param("since") java.time.LocalDateTime since);
}
