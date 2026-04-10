package com.farewatch.application.collector;

import com.farewatch.domain.fare.FareSnapshot;
import com.farewatch.domain.route.Route;
import java.time.LocalDate;
import java.util.List;

/**
 * 외부 소스로부터 특정 노선의 항공권 가격을 수집하는 포트.
 *
 * <p>구현체는 {@code infrastructure/collector/} 아래에 위치한다. 현재 구현:
 * {@code MockFareCollector}. 향후 실제 크롤러/공식 API 구현체로 교체 예정.
 *
 * <p>반환된 {@link FareSnapshot} 은 아직 영속화되지 않은 상태. 영속화와 이벤트 발행은
 * {@code FareCollectionService} 가 담당한다.
 */
public interface FareCollector {

    /**
     * 주어진 노선과 출발일에 대한 스냅샷 목록을 수집한다. 한 호출에서 여러 건을 반환할 수
     * 있으나, 현재 Mock 구현은 호출당 1건을 반환한다.
     *
     * @param route 수집 대상 노선 (non-null)
     * @param targetDate 출발일 (non-null)
     * @return 영속화되지 않은 스냅샷 목록. 빈 리스트 반환 가능(수집 실패 등).
     */
    List<FareSnapshot> fetchFares(Route route, LocalDate targetDate);

    /** 이 수집기의 식별자. {@code FareSnapshot.source} 컬럼에 기록된다. */
    String sourceName();
}
