package com.farewatch.domain.fare;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.farewatch.domain.shared.Money;
import java.time.LocalDate;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("FareSnapshot 엔티티 (단위)")
class FareSnapshotTest {

    private static final Long ROUTE_ID = 1L;
    private static final LocalDate DEP = LocalDate.of(2026, 5, 15);

    @Test
    @DisplayName("유효한 가격으로 record() 팩토리 생성")
    void createWithValidPrice() {
        FareSnapshot snap = FareSnapshot.record(
                ROUTE_ID, DEP, Money.krw(178_000L), "MOCK", Map.of("airline", "KE"));

        assertThat(snap.getRouteId()).isEqualTo(ROUTE_ID);
        assertThat(snap.getDepartureDate()).isEqualTo(DEP);
        assertThat(snap.getPrice()).isEqualTo(Money.krw(178_000L));
        assertThat(snap.getSource()).isEqualTo("MOCK");
        assertThat(snap.getCollectedAt()).isNotNull();
        assertThat(snap.getRawData()).containsEntry("airline", "KE");
    }

    @Test
    @DisplayName("null rawData 허용 (원본 데이터 미보관 케이스)")
    void nullRawDataAllowed() {
        FareSnapshot snap = FareSnapshot.record(
                ROUTE_ID, DEP, Money.krw(200_000L), "MOCK", null);

        assertThat(snap.getRawData()).isNull();
    }

    @Test
    @DisplayName("0원 가격 거부 (무료 항공권은 수집 대상 아님)")
    void rejectsZeroPrice() {
        assertThatThrownBy(() -> FareSnapshot.record(
                ROUTE_ID, DEP, Money.krw(0L), "MOCK", Map.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("price");
    }

    @Test
    @DisplayName("null routeId 거부")
    void rejectsNullRouteId() {
        assertThatThrownBy(() -> FareSnapshot.record(
                null, DEP, Money.krw(100_000L), "MOCK", Map.of()))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("null departureDate 거부")
    void rejectsNullDepartureDate() {
        assertThatThrownBy(() -> FareSnapshot.record(
                ROUTE_ID, null, Money.krw(100_000L), "MOCK", Map.of()))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("null source 거부")
    void rejectsNullSource() {
        assertThatThrownBy(() -> FareSnapshot.record(
                ROUTE_ID, DEP, Money.krw(100_000L), null, Map.of()))
                .isInstanceOf(NullPointerException.class);
    }
}
