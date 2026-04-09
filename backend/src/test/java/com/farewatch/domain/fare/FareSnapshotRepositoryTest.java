package com.farewatch.domain.fare;

import static org.assertj.core.api.Assertions.assertThat;

import com.farewatch.domain.shared.Money;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest
@DisplayName("FareSnapshotRepository (@DataJpaTest)")
class FareSnapshotRepositoryTest {

    @Autowired
    private FareSnapshotRepository repository;

    private static final Long ROUTE_1 = 1L;
    private static final Long ROUTE_2 = 2L;
    private static final LocalDate MAY_15 = LocalDate.of(2026, 5, 15);
    private static final LocalDate MAY_16 = LocalDate.of(2026, 5, 16);
    private static final LocalDate MAY_20 = LocalDate.of(2026, 5, 20);

    @Test
    @DisplayName("save + findById: Money 임베디드 + JSONB rawData round-trip")
    void jsonbRoundTrip() {
        Map<String, Object> raw = Map.of(
                "airline", "KE",
                "flightNumber", "KE123",
                "durationMinutes", 140);

        FareSnapshot saved = repository.save(
                FareSnapshot.record(ROUTE_1, MAY_15, Money.krw(178_000L), "MOCK", raw));

        Optional<FareSnapshot> found = repository.findById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getPrice()).isEqualTo(Money.krw(178_000L));
        assertThat(found.get().getRawData())
                .containsEntry("airline", "KE")
                .containsEntry("flightNumber", "KE123")
                .containsKey("durationMinutes");
    }

    @Test
    @DisplayName("null rawData 저장·조회")
    void nullRawData() {
        FareSnapshot saved = repository.save(
                FareSnapshot.record(ROUTE_1, MAY_15, Money.krw(200_000L), "MOCK", null));

        Optional<FareSnapshot> found = repository.findById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getRawData()).isNull();
    }

    @Test
    @DisplayName("findByRouteIdAndDepartureDateOrderByCollectedAtDesc: 최신순 정렬")
    void findLatestFirst() throws InterruptedException {
        FareSnapshot first = repository.save(
                FareSnapshot.record(ROUTE_1, MAY_15, Money.krw(200_000L), "MOCK", Map.of()));
        Thread.sleep(10); // 서로 다른 collectedAt 확보
        FareSnapshot second = repository.save(
                FareSnapshot.record(ROUTE_1, MAY_15, Money.krw(180_000L), "MOCK", Map.of()));

        List<FareSnapshot> results =
                repository.findByRouteIdAndDepartureDateOrderByCollectedAtDesc(ROUTE_1, MAY_15);

        assertThat(results).extracting(FareSnapshot::getId)
                .containsExactly(second.getId(), first.getId());
    }

    @Test
    @DisplayName("findByRouteIdAndDepartureDateBetween: 출발일 범위 조회 (차트용)")
    void findByDateRange() {
        repository.save(FareSnapshot.record(ROUTE_1, MAY_15, Money.krw(180_000L), "MOCK", Map.of()));
        repository.save(FareSnapshot.record(ROUTE_1, MAY_16, Money.krw(190_000L), "MOCK", Map.of()));
        repository.save(FareSnapshot.record(ROUTE_1, MAY_20, Money.krw(210_000L), "MOCK", Map.of()));

        List<FareSnapshot> results =
                repository.findByRouteIdAndDepartureDateBetween(ROUTE_1, MAY_15, MAY_16);

        assertThat(results).hasSize(2);
    }

    @Test
    @DisplayName("findTopByRouteIdAndDepartureDateOrderByCollectedAtDesc: 가장 최근 가격만")
    void findMostRecent() throws InterruptedException {
        repository.save(FareSnapshot.record(ROUTE_1, MAY_15, Money.krw(200_000L), "MOCK", Map.of()));
        Thread.sleep(10);
        FareSnapshot latest = repository.save(
                FareSnapshot.record(ROUTE_1, MAY_15, Money.krw(180_000L), "MOCK", Map.of()));

        Optional<FareSnapshot> result = repository
                .findTopByRouteIdAndDepartureDateOrderByCollectedAtDesc(ROUTE_1, MAY_15);

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(latest.getId());
    }

    @Test
    @DisplayName("countByRouteIdAndDepartureDate: 표본 수 세기 (통계 계산 전)")
    void countSamples() {
        repository.save(FareSnapshot.record(ROUTE_1, MAY_15, Money.krw(180_000L), "MOCK", Map.of()));
        repository.save(FareSnapshot.record(ROUTE_1, MAY_15, Money.krw(185_000L), "MOCK", Map.of()));
        repository.save(FareSnapshot.record(ROUTE_1, MAY_15, Money.krw(190_000L), "MOCK", Map.of()));
        repository.save(FareSnapshot.record(ROUTE_2, MAY_15, Money.krw(210_000L), "MOCK", Map.of()));

        long count = repository.countByRouteIdAndDepartureDate(ROUTE_1, MAY_15);

        assertThat(count).isEqualTo(3);
    }
}
