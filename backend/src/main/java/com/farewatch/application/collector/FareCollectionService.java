package com.farewatch.application.collector;

import com.farewatch.application.event.FareCollectedEvent;
import com.farewatch.domain.fare.FareSnapshot;
import com.farewatch.domain.fare.FareSnapshotRepository;
import com.farewatch.domain.route.Route;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 하나의 노선에 대해 여러 출발일의 스냅샷을 {@link FareCollector} 로부터 가져와 영속화하고
 * {@link FareCollectedEvent} 를 발행하는 애플리케이션 서비스.
 *
 * <p>스케줄러({@code FareCollectionScheduler}) 가 활성 노선 순회 시 호출하는 핵심 진입점.
 * 이벤트 핸들러(통계 재계산, 알림 평가)는 Phase 5 에서 연결된다.
 *
 * <p><b>트랜잭션 경계</b>: 날짜별로 독립된 저장 + 발행 단위가 되도록 각 스냅샷 저장을
 * 개별 단위로 처리한다. 현재는 메서드 단위 {@link Transactional} 하나로 묶어두고,
 * 날짜별 실패 격리가 필요해지면 per-date tx 로 분리한다 (Phase 5 후속).
 */
@Service
public class FareCollectionService {

    private final FareCollector collector;
    private final FareSnapshotRepository snapshotRepository;
    private final ApplicationEventPublisher eventPublisher;

    public FareCollectionService(
            FareCollector collector,
            FareSnapshotRepository snapshotRepository,
            ApplicationEventPublisher eventPublisher) {
        this.collector = collector;
        this.snapshotRepository = snapshotRepository;
        this.eventPublisher = eventPublisher;
    }

    /**
     * 주어진 노선에 대해 {@code targetDates} 각각의 스냅샷을 수집·저장·이벤트 발행한다.
     *
     * @return 실제로 영속화된 스냅샷 개수
     */
    @Transactional
    public int collectFor(Route route, List<LocalDate> targetDates) {
        Objects.requireNonNull(route, "route must not be null");
        Objects.requireNonNull(targetDates, "targetDates must not be null");
        if (targetDates.isEmpty()) {
            return 0;
        }

        int savedCount = 0;
        for (LocalDate date : targetDates) {
            List<FareSnapshot> fetched = collector.fetchFares(route, date);
            if (fetched == null || fetched.isEmpty()) {
                continue;
            }
            for (FareSnapshot snapshot : fetched) {
                FareSnapshot persisted = snapshotRepository.save(snapshot);
                eventPublisher.publishEvent(new FareCollectedEvent(persisted));
                savedCount++;
            }
        }
        return savedCount;
    }
}
