package com.farewatch.application.event;

import com.farewatch.domain.fare.FareSnapshot;
import java.util.Objects;

/**
 * 새 {@link FareSnapshot} 이 영속화된 직후 발행되는 도메인 이벤트.
 *
 * <p>Phase 4 에서는 발행 포인트만 자리 잡아 두고, 실제 핸들러(통계 재계산, 알림 평가)는
 * Phase 5 에서 연결한다. 그 전까지는 핸들러 없이 발행되더라도 Spring
 * {@code ApplicationEventPublisher} 가 무해하게 처리한다.
 *
 * <p>불변 이벤트. 저장된 스냅샷의 ID 는 non-null 이다.
 */
public record FareCollectedEvent(FareSnapshot snapshot) {

    public FareCollectedEvent {
        Objects.requireNonNull(snapshot, "snapshot must not be null");
        if (snapshot.getId() == null) {
            throw new IllegalArgumentException(
                    "FareCollectedEvent requires a persisted snapshot (id must not be null)");
        }
    }
}
