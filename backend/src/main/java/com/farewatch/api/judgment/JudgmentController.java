package com.farewatch.api.judgment;

import com.farewatch.api.common.ApiResponse;
import com.farewatch.api.common.ResourceNotFoundException;
import com.farewatch.application.judgment.FareVerdictCalculator;
import com.farewatch.domain.fare.FareSnapshot;
import com.farewatch.domain.fare.FareSnapshotRepository;
import com.farewatch.domain.fare.FareStatistics;
import com.farewatch.domain.fare.FareStatisticsRepository;
import com.farewatch.domain.judgment.FareVerdict;
import com.farewatch.domain.route.RouteRepository;
import com.farewatch.domain.shared.FareVerdictKind;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 노선 판단 결과 조회. 프론트엔드 메인 카드 + 노선 상세 화면이 호출하는 핵심 read 경로.
 *
 * <p>read-model 어그리게이터이므로 별도 application 서비스 없이 컨트롤러가 직접 도메인
 * 컴포넌트({@link FareVerdictCalculator}) 와 repository 를 합성한다.
 *
 * <ul>
 *   <li>입력: routeId path + (선택) {@code departureDate} 쿼리. 미지정 시 "오늘 + 7일"
 *       기본값 — 스케줄러의 가장 가까운 수집 오프셋과 일치.
 *   <li>스냅샷이 없으면 404 (애초에 데이터가 없으므로 판단 불가)
 *   <li>통계가 아직 없으면 Insufficient + sample=0 응답
 * </ul>
 */
@Tag(
        name = "Judgment",
        description =
                "**핵심 엔드포인트**: 현재 가격이 통계 대비 어떤 구매 판단(CHEAP/FAIR/EXPENSIVE/INSUFFICIENT)에 해당하는지 반환")
@RestController
@RequestMapping("/api/v1/routes/{id}/judgment")
public class JudgmentController {

    static final int DEFAULT_DEPARTURE_OFFSET_DAYS = 7;

    private final RouteRepository routeRepository;
    private final FareSnapshotRepository snapshotRepository;
    private final FareStatisticsRepository statisticsRepository;
    private final FareVerdictCalculator verdictCalculator;
    private final Clock clock;

    public JudgmentController(
            RouteRepository routeRepository,
            FareSnapshotRepository snapshotRepository,
            FareStatisticsRepository statisticsRepository,
            FareVerdictCalculator verdictCalculator,
            Clock clock) {
        this.routeRepository = routeRepository;
        this.snapshotRepository = snapshotRepository;
        this.statisticsRepository = statisticsRepository;
        this.verdictCalculator = verdictCalculator;
        this.clock = clock;
    }

    @Operation(
            summary = "구매 판단",
            description =
                    """
                    노선의 현재 가격에 대한 판단 결과 (CHEAP/FAIR/EXPENSIVE/INSUFFICIENT) 와
                    추천 메시지를 반환한다.

                    - 표본이 30개 미만 → INSUFFICIENT
                    - 현재가 < 평균 - 1σ → CHEAP (구매 추천)
                    - 현재가 > 평균 + 0.5σ → EXPENSIVE (대기 추천)
                    - 그 외 → FAIR

                    `departureDate` 미지정 시 today + 7일 사용.
                    """)
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "노선 또는 가격 스냅샷이 없음")
    })
    @GetMapping
    public ApiResponse<JudgmentResponse> judgment(
            @Parameter(description = "노선 ID") @PathVariable Long id,
            @Parameter(description = "출발일 (YYYY-MM-DD). 생략 시 오늘 + 7일")
                    @RequestParam(required = false)
                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate departureDate) {
        if (!routeRepository.existsById(id)) {
            throw new ResourceNotFoundException("route not found: " + id);
        }
        LocalDate target =
                (departureDate != null)
                        ? departureDate
                        : LocalDate.now(clock).plusDays(DEFAULT_DEPARTURE_OFFSET_DAYS);

        FareSnapshot snapshot =
                snapshotRepository
                        .findTopByRouteIdAndDepartureDateOrderByCollectedAtDesc(id, target)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "no fare snapshot for routeId="
                                                        + id
                                                        + ", departureDate="
                                                        + target));
        long currentPrice = snapshot.getPrice().amount();

        Optional<FareStatistics> statsOpt =
                statisticsRepository.findByRouteIdAndDepartureDate(id, target);
        if (statsOpt.isEmpty()) {
            return ApiResponse.ok(
                    new JudgmentResponse(
                            id,
                            target,
                            FareVerdictKind.INSUFFICIENT,
                            currentPrice,
                            null,
                            null,
                            null,
                            null,
                            null,
                            0,
                            "데이터 부족",
                            snapshot.getCollectedAt()));
        }

        FareStatistics stats = statsOpt.get();
        FareVerdict verdict = verdictCalculator.evaluate(currentPrice, stats);
        return ApiResponse.ok(toResponse(id, target, currentPrice, stats, verdict));
    }

    private static JudgmentResponse toResponse(
            Long routeId,
            LocalDate target,
            long currentPrice,
            FareStatistics stats,
            FareVerdict verdict) {
        return switch (verdict) {
            case FareVerdict.Cheap c ->
                    new JudgmentResponse(
                            routeId,
                            target,
                            FareVerdictKind.CHEAP,
                            currentPrice,
                            c.avgPrice(),
                            stats.getMinPrice(),
                            stats.getMaxPrice(),
                            stats.getStdDeviation(),
                            c.zScore(),
                            stats.getSampleCount(),
                            "지금 구매를 추천합니다",
                            stats.getCalculatedAt());
            case FareVerdict.Fair f ->
                    new JudgmentResponse(
                            routeId,
                            target,
                            FareVerdictKind.FAIR,
                            currentPrice,
                            f.avgPrice(),
                            stats.getMinPrice(),
                            stats.getMaxPrice(),
                            stats.getStdDeviation(),
                            null,
                            stats.getSampleCount(),
                            "적정가입니다",
                            stats.getCalculatedAt());
            case FareVerdict.Expensive e ->
                    new JudgmentResponse(
                            routeId,
                            target,
                            FareVerdictKind.EXPENSIVE,
                            currentPrice,
                            e.avgPrice(),
                            stats.getMinPrice(),
                            stats.getMaxPrice(),
                            stats.getStdDeviation(),
                            e.zScore(),
                            stats.getSampleCount(),
                            "더 기다리는 것을 추천합니다",
                            stats.getCalculatedAt());
            case FareVerdict.Insufficient i ->
                    new JudgmentResponse(
                            routeId,
                            target,
                            FareVerdictKind.INSUFFICIENT,
                            currentPrice,
                            stats.getAvgPrice(),
                            stats.getMinPrice(),
                            stats.getMaxPrice(),
                            stats.getStdDeviation(),
                            null,
                            i.sampleCount(),
                            "데이터 부족 — 표본 "
                                    + i.sampleCount()
                                    + "/"
                                    + i.requiredCount(),
                            stats.getCalculatedAt());
        };
    }
}
