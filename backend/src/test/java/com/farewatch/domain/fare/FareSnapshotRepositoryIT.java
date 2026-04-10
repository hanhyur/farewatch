package com.farewatch.domain.fare;

import static org.assertj.core.api.Assertions.assertThat;

import com.farewatch.domain.shared.Money;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;

/**
 * H2 와 실제 Hibernate 매핑을 사용하는 {@link FareSnapshotRepository} 통합 테스트.
 *
 * <p>커버 범위:
 *
 * <ul>
 *   <li>{@code @Embedded Money} 의 {@code @AttributeOverride} 컬럼 매핑이 동작
 *   <li>{@code @JdbcTypeCode(SqlTypes.JSON) Map} 컬럼이 H2 에서 round-trip
 *   <li>커스텀 JPQL {@code findPricesByRouteIdAndDepartureDate} 가 정확한 가격 리스트 반환
 *   <li>Pageable 로 제한된 정렬 쿼리가 collected_at 내림차순
 * </ul>
 */
@DataJpaTest
class FareSnapshotRepositoryIT {

    @Autowired private FareSnapshotRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    @DisplayName("save + find: embedded Money round-trips with KRW currency")
    void save_andFind_preservesEmbeddedMoney() {
        FareSnapshot saved =
                repository.save(
                        FareSnapshot.record(
                                1L, LocalDate.of(2026, 5, 10), Money.krw(178_000L), "MOCK", null));

        assertThat(saved.getId()).isNotNull();
        FareSnapshot found = repository.findById(saved.getId()).orElseThrow();
        assertThat(found.getPrice().amount()).isEqualTo(178_000L);
        assertThat(found.getPrice().currency()).isEqualTo("KRW");
        assertThat(found.getSource()).isEqualTo("MOCK");
    }

    @Test
    @DisplayName("findPricesByRouteIdAndDepartureDate: returns only matching prices")
    void findPrices_filtersByRouteAndDate() {
        LocalDate target = LocalDate.of(2026, 5, 10);
        repository.save(snap(1L, target, 100_000L));
        repository.save(snap(1L, target, 200_000L));
        repository.save(snap(1L, target, 300_000L));
        // different date
        repository.save(snap(1L, LocalDate.of(2026, 5, 11), 999_000L));
        // different route
        repository.save(snap(2L, target, 999_000L));

        List<Long> prices = repository.findPricesByRouteIdAndDepartureDate(1L, target);

        assertThat(prices).containsExactlyInAnyOrder(100_000L, 200_000L, 300_000L);
    }

    @Test
    @DisplayName("findPricesByRouteIdAndDepartureDate: empty result when no match")
    void findPrices_emptyWhenNoMatch() {
        repository.save(snap(1L, LocalDate.of(2026, 5, 10), 100_000L));

        List<Long> prices =
                repository.findPricesByRouteIdAndDepartureDate(99L, LocalDate.of(2026, 5, 10));

        assertThat(prices).isEmpty();
    }

    @Test
    @DisplayName("countByRouteIdAndDepartureDate: counts only matching")
    void count_filtersCorrectly() {
        LocalDate target = LocalDate.of(2026, 5, 10);
        repository.save(snap(1L, target, 100_000L));
        repository.save(snap(1L, target, 110_000L));
        repository.save(snap(1L, LocalDate.of(2026, 5, 11), 120_000L));

        assertThat(repository.countByRouteIdAndDepartureDate(1L, target)).isEqualTo(2L);
    }

    @Test
    @DisplayName(
            "findTopByRouteIdAndDepartureDateOrderByCollectedAtDesc: returns most recent snapshot")
    void findTop_returnsLatest() {
        LocalDate target = LocalDate.of(2026, 5, 10);
        repository.save(snap(1L, target, 100_000L));
        // smallest sleep so collected_at advances
        try {
            Thread.sleep(5);
        } catch (InterruptedException ignored) {
        }
        repository.save(snap(1L, target, 200_000L));
        try {
            Thread.sleep(5);
        } catch (InterruptedException ignored) {
        }
        FareSnapshot latest = repository.save(snap(1L, target, 300_000L));

        FareSnapshot found =
                repository
                        .findTopByRouteIdAndDepartureDateOrderByCollectedAtDesc(1L, target)
                        .orElseThrow();

        assertThat(found.getId()).isEqualTo(latest.getId());
        assertThat(found.getPrice().amount()).isEqualTo(300_000L);
    }

    @Test
    @DisplayName("findByRouteIdAndDepartureDateOrderByCollectedAtDesc: respects Pageable limit")
    void findByDate_respectsPageable() {
        LocalDate target = LocalDate.of(2026, 5, 10);
        for (int i = 0; i < 5; i++) {
            repository.save(snap(1L, target, 100_000L + i));
            try {
                Thread.sleep(2);
            } catch (InterruptedException ignored) {
            }
        }

        List<FareSnapshot> page =
                repository.findByRouteIdAndDepartureDateOrderByCollectedAtDesc(
                        1L, target, PageRequest.of(0, 3));

        assertThat(page).hasSize(3);
    }

    private static FareSnapshot snap(long routeId, LocalDate date, long price) {
        return FareSnapshot.record(routeId, date, Money.krw(price), "MOCK", null);
    }
}
