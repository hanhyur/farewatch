package com.farewatch.api.fare;

import com.farewatch.api.common.ApiResponse;
import com.farewatch.api.common.ResourceNotFoundException;
import com.farewatch.domain.fare.FareSnapshotRepository;
import com.farewatch.domain.fare.FareStatisticsRepository;
import com.farewatch.domain.route.RouteRepository;
import java.time.Clock;
import java.time.LocalDate;
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

    @GetMapping("/fares")
    public ApiResponse<List<FareSnapshotResponse>> fares(
            @PathVariable Long id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate departureDate,
            @RequestParam(required = false) Integer limit) {
        ensureRouteExists(id);
        LocalDate target = (departureDate != null) ? departureDate : defaultDepartureDate();
        int pageSize = clampLimit(limit);

        List<FareSnapshotResponse> body =
                snapshotRepository
                        .findByRouteIdAndDepartureDateOrderByCollectedAtDesc(
                                id, target, PageRequest.of(0, pageSize))
                        .stream()
                        .map(FareSnapshotResponse::from)
                        .toList();
        return ApiResponse.ok(body);
    }

    @GetMapping("/statistics")
    public ApiResponse<FareStatisticsResponse> statistics(
            @PathVariable Long id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate departureDate) {
        ensureRouteExists(id);
        LocalDate target = (departureDate != null) ? departureDate : defaultDepartureDate();

        return statisticsRepository
                .findByRouteIdAndDepartureDate(id, target)
                .map(FareStatisticsResponse::from)
                .map(ApiResponse::ok)
                .orElseThrow(
                        () ->
                                new ResourceNotFoundException(
                                        "statistics not found for routeId="
                                                + id
                                                + ", departureDate="
                                                + target));
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
