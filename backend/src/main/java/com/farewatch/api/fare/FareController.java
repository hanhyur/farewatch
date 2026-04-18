package com.farewatch.api.fare;

import com.farewatch.api.common.ApiResponse;
import com.farewatch.api.common.ResourceNotFoundException;
import com.farewatch.domain.fare.FareSnapshotRepository;
import com.farewatch.domain.fare.FareStatisticsRepository;
import com.farewatch.domain.route.RouteRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 가격 히스토리 + 통계 컨트롤러.
 *
 * <ul>
 *   <li>{@code GET /api/v1/routes/{id}/fares} — 특정 출발일의 최근 스냅샷 목록 (차트용)
 *   <li>{@code GET /api/v1/routes/{id}/statistics} — 특정 출발일의 누적 통계
 * </ul>
 *
 * <p>두 엔드포인트 모두 {@code departureDate} 쿼리 파라미터가 선택사항이며, 미지정 시
 * "오늘 + 7일" 을 기본값으로 사용한다 — {@link com.farewatch.infrastructure.scheduler
 * .FareCollectionScheduler} 의 가장 짧은 수집 오프셋과 일치.
 */
@Tag(name = "Fares", description = "노선별 가격 히스토리 + 통계 조회")
@RestController
@RequestMapping("/api/v1/routes/{id}")
public class FareController {

    /** 차트 한 점당 너무 많은 데이터 방지용 기본 페이지 크기. */
    static final int DEFAULT_FARE_LIMIT = 100;
    static final int MAX_FARE_LIMIT = 500;
    /** 통계가 없을 때 채택하는 기본 출발일 오프셋 — 스케줄러 가장 가까운 수집일. */
    static final int DEFAULT_DEPARTURE_OFFSET_DAYS = 7;

    private final FareSnapshotRepository snapshotRepository;
    private final FareStatisticsRepository statisticsRepository;
    private final RouteRepository routeRepository;
    private final Clock clock;

    public FareController(
            FareSnapshotRepository snapshotRepository,
            FareStatisticsRepository statisticsRepository,
            RouteRepository routeRepository,
            Clock clock) {
        this.snapshotRepository = snapshotRepository;
        this.statisticsRepository = statisticsRepository;
        this.routeRepository = routeRepository;
        this.clock = clock;
    }

    @Operation(
            summary = "가격 히스토리",
            description =
                    "특정 출발일에 대한 최근 가격 스냅샷을 collected_at 내림차순으로 반환한다. "
                            + "차트의 시계열 데이터로 사용. departureDate 미지정 시 today + 7일 사용.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "노선을 찾을 수 없음")
    })
    @GetMapping("/fares")
    public ApiResponse<List<FareSnapshotResponse>> fares(
            @Parameter(description = "노선 ID") @PathVariable Long id,
            @Parameter(description = "출발일 (YYYY-MM-DD). 생략 시 오늘 + 7일")
                    @RequestParam(required = false)
                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate departureDate,
            @Parameter(description = "조회 기간 (일). 7/30/180/365. 생략 시 limit 기반 조회")
                    @RequestParam(required = false)
                    Integer days,
            @Parameter(description = "최대 결과 수 (기본 100, 최대 500). days 지정 시 무시됨")
                    @RequestParam(required = false)
                    Integer limit) {
        ensureRouteExists(id);
        LocalDate target = (departureDate != null) ? departureDate : defaultDepartureDate();

        List<FareSnapshotResponse> body;
        if (days != null && days > 0) {
            LocalDateTime since = LocalDateTime.now(clock).minusDays(days);
            body = snapshotRepository
                    .findByRouteIdAndDepartureDateSince(id, target, since)
                    .stream()
                    .map(FareSnapshotResponse::from)
                    .toList();
        } else {
            int pageSize = clampLimit(limit);
            body = snapshotRepository
                    .findByRouteIdAndDepartureDateOrderByCollectedAtDesc(
                            id, target, PageRequest.of(0, pageSize))
                    .stream()
                    .map(FareSnapshotResponse::from)
                    .toList();
        }
        return ApiResponse.ok(body);
    }

    @Operation(
            summary = "누적 통계",
            description = "특정 출발일에 대한 평균/최소/최대/표준편차/표본수/사분위수 통계 조회.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "노선 또는 통계가 없음")
    })
    @GetMapping("/statistics")
    public ApiResponse<FareStatisticsResponse> statistics(
            @Parameter(description = "노선 ID") @PathVariable Long id,
            @Parameter(description = "출발일 (YYYY-MM-DD). 생략 시 오늘 + 7일")
                    @RequestParam(required = false)
                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate departureDate) {
        ensureRouteExists(id);
        LocalDate target = (departureDate != null) ? departureDate : defaultDepartureDate();

        FareStatisticsResponse stats = statisticsRepository
                .findByRouteIdAndDepartureDate(id, target)
                .map(FareStatisticsResponse::from)
                .orElseThrow(
                        () ->
                                new ResourceNotFoundException(
                                        "statistics not found for routeId="
                                                + id
                                                + ", departureDate="
                                                + target));

        // 역대 최저가 채우기
        var lowest = snapshotRepository.findAllTimeLowest(id, target, PageRequest.of(0, 1));
        if (!lowest.isEmpty()) {
            var snap = lowest.getFirst();
            stats = stats.withAllTimeLow(snap.getPrice().amount(), snap.getCollectedAt());
        }
        return ApiResponse.ok(stats);
    }

    private void ensureRouteExists(Long routeId) {
        if (!routeRepository.existsById(routeId)) {
            throw new ResourceNotFoundException("route not found: " + routeId);
        }
    }

    private LocalDate defaultDepartureDate() {
        return LocalDate.now(clock).plusDays(DEFAULT_DEPARTURE_OFFSET_DAYS);
    }

    private static int clampLimit(Integer raw) {
        if (raw == null || raw <= 0) {
            return DEFAULT_FARE_LIMIT;
        }
        return Math.min(raw, MAX_FARE_LIMIT);
    }
}
