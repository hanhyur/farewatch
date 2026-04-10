package com.farewatch.application.analyzer;

import com.farewatch.application.event.FareCollectedEvent;
import com.farewatch.domain.fare.FareSnapshot;
import com.farewatch.domain.fare.FareSnapshotRepository;
import com.farewatch.domain.fare.FareStatistics;
import com.farewatch.domain.fare.FareStatisticsRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@link FareCollectedEvent} 가 발행되면 해당 (routeId, departureDate) 의 누적 가격으로
 * {@link FareStatistics} 를 재계산해 upsert 한다.
 *
 * <p><b>upsert 의미</b>: 기존 행이 있으면 {@link FareStatistics#recompute} 로 in-place
 * 갱신 (JPA dirty checking 으로 flush). 없으면 {@link FareStatistics#compute} 로 새 행
 * 생성 후 save.
 *
 * <p><b>트랜잭션 경계</b>: 핸들러 메서드 단위로 {@link Transactional}. 같은 트랜잭션
 * 내에서 dirty checking 이 동작해야 기존 stats 의 recompute 가 자동 flush 된다.
 *
 * <p><b>실패 격리</b>: 통계 갱신 실패가 수집 트랜잭션을 롤백시키지 않도록, Phase 5 후속에
 * 서 {@code @TransactionalEventListener(phase = AFTER_COMMIT)} 로 옮길 수 있다. 현재는
 * 단순 {@link EventListener} — 핵심 흐름이 단일 노드에서 동기 처리되는 v0 단계.
 */
@Component
public class StatisticsUpdateHandler {

    private static final Logger log = LoggerFactory.getLogger(StatisticsUpdateHandler.class);

    private final FareSnapshotRepository snapshotRepository;
    private final FareStatisticsRepository statisticsRepository;
    private final StatisticsCalculator calculator;

    public StatisticsUpdateHandler(
            FareSnapshotRepository snapshotRepository,
            FareStatisticsRepository statisticsRepository,
            StatisticsCalculator calculator) {
        this.snapshotRepository = snapshotRepository;
        this.statisticsRepository = statisticsRepository;
        this.calculator = calculator;
    }

    @EventListener
    @Transactional
    public void on(FareCollectedEvent event) {
        FareSnapshot snapshot = event.snapshot();
        Long routeId = snapshot.getRouteId();
        LocalDate departureDate = snapshot.getDepartureDate();

        List<Long> prices =
                snapshotRepository.findPricesByRouteIdAndDepartureDate(routeId, departureDate);
        if (prices.isEmpty()) {
            // 방금 저장한 스냅샷이 보이지 않는 케이스 — 이론상 동일 트랜잭션 안에서 발생
            // 불가능하지만, 트랜잭션 경계 변경 시 race 가 생길 수 있어 방어적으로 skip.
            log.warn(
                    "StatisticsUpdateHandler: empty price list for routeId={}, departureDate={}; skipping",
                    routeId,
                    departureDate);
            return;
        }

        StatisticsCalculator.Result r = calculator.compute(prices);

        Optional<FareStatistics> existing =
                statisticsRepository.findByRouteIdAndDepartureDate(routeId, departureDate);
        if (existing.isPresent()) {
            existing.get()
                    .recompute(
                            r.avgPrice(),
                            r.minPrice(),
                            r.maxPrice(),
                            r.stdDeviation(),
                            r.sampleCount(),
                            r.p25Price(),
                            r.p75Price());
        } else {
            FareStatistics fresh =
                    FareStatistics.compute(
                            routeId,
                            departureDate,
                            r.avgPrice(),
                            r.minPrice(),
                            r.maxPrice(),
                            r.stdDeviation(),
                            r.sampleCount(),
                            r.p25Price(),
                            r.p75Price());
            statisticsRepository.save(fresh);
        }
    }
}
