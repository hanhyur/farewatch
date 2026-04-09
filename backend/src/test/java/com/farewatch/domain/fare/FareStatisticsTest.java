package com.farewatch.domain.fare;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("FareStatistics 엔티티 (단위)")
class FareStatisticsTest {

    private static final Long ROUTE_ID = 1L;
    private static final LocalDate DEP = LocalDate.of(2026, 5, 15);

    @Test
    @DisplayName("유효한 통계로 compute() 팩토리 생성")
    void createWithValidStats() {
        FareStatistics stats = FareStatistics.compute(
                ROUTE_ID, DEP, 232_000L, 154_000L, 310_000L, 28_500.0, 45, 200_000L, 260_000L);

        assertThat(stats.getRouteId()).isEqualTo(ROUTE_ID);
        assertThat(stats.getDepartureDate()).isEqualTo(DEP);
        assertThat(stats.getAvgPrice()).isEqualTo(232_000L);
        assertThat(stats.getMinPrice()).isEqualTo(154_000L);
        assertThat(stats.getMaxPrice()).isEqualTo(310_000L);
        assertThat(stats.getStdDeviation()).isEqualTo(28_500.0);
        assertThat(stats.getSampleCount()).isEqualTo(45);
        assertThat(stats.getP25Price()).isEqualTo(200_000L);
        assertThat(stats.getP75Price()).isEqualTo(260_000L);
        assertThat(stats.getCalculatedAt()).isNotNull();
    }

    @Test
    @DisplayName("nullable p25/p75 허용")
    void nullablePercentiles() {
        FareStatistics stats = FareStatistics.compute(
                ROUTE_ID, DEP, 232_000L, 154_000L, 310_000L, 28_500.0, 45, null, null);

        assertThat(stats.getP25Price()).isNull();
        assertThat(stats.getP75Price()).isNull();
    }

    @Test
    @DisplayName("min > avg 거부 (min <= avg <= max invariant)")
    void rejectsMinGreaterThanAvg() {
        assertThatThrownBy(() -> FareStatistics.compute(
                ROUTE_ID, DEP, 100_000L, 200_000L, 300_000L, 10.0, 10, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("min");
    }

    @Test
    @DisplayName("avg > max 거부")
    void rejectsAvgGreaterThanMax() {
        assertThatThrownBy(() -> FareStatistics.compute(
                ROUTE_ID, DEP, 500_000L, 100_000L, 300_000L, 10.0, 10, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("max");
    }

    @Test
    @DisplayName("음수 sampleCount 거부")
    void rejectsNegativeSampleCount() {
        assertThatThrownBy(() -> FareStatistics.compute(
                ROUTE_ID, DEP, 200_000L, 100_000L, 300_000L, 10.0, -1, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("sample");
    }

    @Test
    @DisplayName("음수 stdDeviation 거부")
    void rejectsNegativeStdDev() {
        assertThatThrownBy(() -> FareStatistics.compute(
                ROUTE_ID, DEP, 200_000L, 100_000L, 300_000L, -1.0, 10, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("stdDeviation");
    }

    @Test
    @DisplayName("NaN stdDeviation 거부 (통계 계산 버그 방어)")
    void rejectsNaNStdDev() {
        assertThatThrownBy(() -> FareStatistics.compute(
                ROUTE_ID, DEP, 200_000L, 100_000L, 300_000L, Double.NaN, 10, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("finite");
    }

    @Test
    @DisplayName("Infinity stdDeviation 거부")
    void rejectsInfiniteStdDev() {
        assertThatThrownBy(() -> FareStatistics.compute(
                ROUTE_ID, DEP, 200_000L, 100_000L, 300_000L, Double.POSITIVE_INFINITY, 10, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("finite");
    }

    @Test
    @DisplayName("0원 avgPrice 거부 (의미 없는 평균)")
    void rejectsZeroAvg() {
        assertThatThrownBy(() -> FareStatistics.compute(
                ROUTE_ID, DEP, 0L, 0L, 0L, 0.0, 10, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("avgPrice");
    }

    @Test
    @DisplayName("recompute(): 기존 행의 값을 갱신하고 calculatedAt 을 새로 설정")
    void recomputeUpdatesFields() throws InterruptedException {
        FareStatistics stats = FareStatistics.compute(
                ROUTE_ID, DEP, 200_000L, 150_000L, 250_000L, 20_000.0, 30, null, null);
        java.time.LocalDateTime firstCalc = stats.getCalculatedAt();
        Thread.sleep(5);

        stats.recompute(210_000L, 160_000L, 260_000L, 21_000.0, 35, 190_000L, 240_000L);

        assertThat(stats.getAvgPrice()).isEqualTo(210_000L);
        assertThat(stats.getSampleCount()).isEqualTo(35);
        assertThat(stats.getP25Price()).isEqualTo(190_000L);
        assertThat(stats.getCalculatedAt()).isAfter(firstCalc);
    }
}
