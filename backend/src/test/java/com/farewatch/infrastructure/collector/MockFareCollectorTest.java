package com.farewatch.infrastructure.collector;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.farewatch.domain.fare.FareSnapshot;
import com.farewatch.domain.route.Route;
import com.farewatch.domain.shared.AirportCode;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MockFareCollectorTest {

    private MockFareCollector collector;
    private Route pusToNrt;

    @BeforeEach
    void setUp() {
        collector = new MockFareCollector();
        pusToNrt = Route.create(new AirportCode("PUS"), new AirportCode("NRT"), "KE");
        setRouteId(pusToNrt, 1L);
    }

    @Test
    @DisplayName("sourceName is MOCK")
    void sourceName_isMock() {
        assertThat(collector.sourceName()).isEqualTo("MOCK");
    }

    @Test
    @DisplayName("fetchFares returns exactly one snapshot per call")
    void fetchFares_returnsSingleSnapshot() {
        List<FareSnapshot> snapshots = collector.fetchFares(pusToNrt, LocalDate.of(2026, 5, 10));

        assertThat(snapshots).hasSize(1);
    }

    @Test
    @DisplayName("fetchFares snapshot carries routeId, departureDate, source, and positive price")
    void fetchFares_snapshotAttributes() {
        LocalDate departure = LocalDate.of(2026, 5, 10);

        FareSnapshot snapshot = collector.fetchFares(pusToNrt, departure).get(0);

        assertThat(snapshot.getRouteId()).isEqualTo(1L);
        assertThat(snapshot.getDepartureDate()).isEqualTo(departure);
        assertThat(snapshot.getSource()).isEqualTo("MOCK");
        assertThat(snapshot.getPrice().amount()).isPositive();
        assertThat(snapshot.getPrice().currency()).isEqualTo("KRW");
    }

    @Test
    @DisplayName("fetchFares is deterministic within the same minute")
    void fetchFares_deterministicWithinSameMinute() {
        LocalDate departure = LocalDate.of(2026, 5, 10);

        long firstPrice = collector.fetchFares(pusToNrt, departure).get(0).getPrice().amount();
        long secondPrice = collector.fetchFares(pusToNrt, departure).get(0).getPrice().amount();

        // 같은 분 내에서 호출하면 동일 가격
        assertThat(firstPrice).isEqualTo(secondPrice);
    }

    @Test
    @DisplayName("fetchFares produces different prices across departure dates")
    void fetchFares_variesAcrossDates() {
        long d1 = collector.fetchFares(pusToNrt, LocalDate.of(2026, 5, 10)).get(0).getPrice().amount();
        long d2 = collector.fetchFares(pusToNrt, LocalDate.of(2026, 5, 11)).get(0).getPrice().amount();
        long d3 = collector.fetchFares(pusToNrt, LocalDate.of(2026, 5, 12)).get(0).getPrice().amount();

        assertThat(List.of(d1, d2, d3)).doesNotHaveDuplicates();
    }

    @Test
    @DisplayName("fetchFares produces different prices across routes")
    void fetchFares_variesAcrossRoutes() {
        Route other = Route.create(new AirportCode("PUS"), new AirportCode("HND"), "KE");
        setRouteId(other, 2L);
        LocalDate departure = LocalDate.of(2026, 5, 10);

        long a = collector.fetchFares(pusToNrt, departure).get(0).getPrice().amount();
        long b = collector.fetchFares(other, departure).get(0).getPrice().amount();

        assertThat(a).isNotEqualTo(b);
    }

    @Test
    @DisplayName("fetchFares price stays inside the configured band [80_000, 400_000]")
    void fetchFares_priceBand() {
        // sample 60 days to exercise the seed space
        LocalDate base = LocalDate.of(2026, 5, 1);
        for (int i = 0; i < 60; i++) {
            long price = collector.fetchFares(pusToNrt, base.plusDays(i)).get(0).getPrice().amount();
            assertThat(price).isBetween(80_000L, 400_000L);
        }
    }

    @Test
    @DisplayName("fetchFares rejects null route")
    void fetchFares_rejectsNullRoute() {
        assertThatThrownBy(() -> collector.fetchFares(null, LocalDate.of(2026, 5, 10)))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("fetchFares rejects null targetDate")
    void fetchFares_rejectsNullDate() {
        assertThatThrownBy(() -> collector.fetchFares(pusToNrt, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("fetchFares rejects route without persisted id")
    void fetchFares_rejectsRouteWithoutId() {
        Route transient_ = Route.create(new AirportCode("PUS"), new AirportCode("KIX"), "KE");

        assertThatThrownBy(() -> collector.fetchFares(transient_, LocalDate.of(2026, 5, 10)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("route id");
    }

    /**
     * Route.id 는 JPA 가 채우는 필드이므로 테스트에서는 reflection 으로 주입한다.
     * 실제 영속 환경에서는 이 helper 불필요.
     */
    private static void setRouteId(Route route, Long id) {
        try {
            var field = Route.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(route, id);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
