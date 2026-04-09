package com.farewatch.infrastructure.scheduler;

import com.farewatch.application.collector.FareCollectionService;
import com.farewatch.domain.route.Route;
import com.farewatch.domain.route.RouteRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 주기적으로 활성 노선을 순회하며 {@link FareCollectionService} 를 통해 스냅샷을 수집한다.
 *
 * <p>수집 주기 및 대상 출발일 오프셋은 {@code farewatch.collection.*} 프로퍼티로 제어한다.
 *
 * <p><b>Per-route resilience</b>: 한 노선 수집이 실패해도 전체 sweep 이 멈추지 않도록
 * 루프 안에서 예외를 삼키고 로그만 남긴다. 실패한 노선은 다음 주기에 자연스럽게 재시도된다.
 *
 * <p><b>Clock 주입</b>: "오늘" 을 테스트에서 고정하기 위해 {@link Clock} 을 DI 로 받는다.
 * 프로덕션에서는 {@code FarewatchApplication} 에서 선언한 시스템 Clock 빈이 주입된다.
 */
@Component
public class FareCollectionScheduler {

    private static final Logger log = LoggerFactory.getLogger(FareCollectionScheduler.class);

    private final RouteRepository routeRepository;
    private final FareCollectionService collectionService;
    private final List<Integer> targetDayOffsets;
    private final Clock clock;

    public FareCollectionScheduler(
            RouteRepository routeRepository,
            FareCollectionService collectionService,
            @Value("${farewatch.collection.target-day-offsets:7,14,30}")
                    List<Integer> targetDayOffsets,
            Clock clock) {
        this.routeRepository = Objects.requireNonNull(routeRepository);
        this.collectionService = Objects.requireNonNull(collectionService);
        this.targetDayOffsets = List.copyOf(targetDayOffsets);
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    /**
     * 활성 노선들에 대해 스냅샷 수집을 수행한다. 기본 주기는 6 시간
     * ({@code 21_600_000 ms}). 앱 시작 직후 과도한 부하를 피하기 위해
     * {@code initialDelay = 10_000 ms}.
     */
    @Scheduled(
            fixedDelayString = "${farewatch.collection.interval-ms:21600000}",
            initialDelay = 10_000L)
    public void sweep() {
        List<Route> routes = routeRepository.findAllByActiveTrue();
        if (routes.isEmpty()) {
            log.debug("FareCollectionScheduler: no active routes; skipping sweep");
            return;
        }

        LocalDate today = LocalDate.now(clock);
        List<LocalDate> targetDates = targetDayOffsets.stream().map(today::plusDays).toList();

        int totalSaved = 0;
        int failedRoutes = 0;
        for (Route route : routes) {
            try {
                totalSaved += collectionService.collectFor(route, targetDates);
            } catch (RuntimeException ex) {
                failedRoutes++;
                log.warn(
                        "FareCollectionScheduler: failed to collect for routeId={} ({} -> {})",
                        route.getId(),
                        route.getOrigin().value(),
                        route.getDestination().value(),
                        ex);
            }
        }
        log.info(
                "FareCollectionScheduler sweep complete: routes={}, saved={}, failedRoutes={}",
                routes.size(),
                totalSaved,
                failedRoutes);
    }
}
