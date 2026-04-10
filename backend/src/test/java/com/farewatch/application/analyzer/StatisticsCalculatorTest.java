package com.farewatch.application.analyzer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class StatisticsCalculatorTest {

    private final StatisticsCalculator calculator = new StatisticsCalculator();

    @Test
    @DisplayName("compute: single sample yields zero stdDev and equal min/avg/max")
    void compute_singleSample() {
        StatisticsCalculator.Result r = calculator.compute(List.of(180_000L));

        assertThat(r.sampleCount()).isEqualTo(1);
        assertThat(r.avgPrice()).isEqualTo(180_000L);
        assertThat(r.minPrice()).isEqualTo(180_000L);
        assertThat(r.maxPrice()).isEqualTo(180_000L);
        assertThat(r.stdDeviation()).isEqualTo(0.0);
        assertThat(r.p25Price()).isEqualTo(180_000L);
        assertThat(r.p75Price()).isEqualTo(180_000L);
    }

    @Test
    @DisplayName("compute: multi-sample yields correct avg/min/max and population stdDev")
    void compute_multipleSamples() {
        // values: 100, 200, 300, 400, 500
        // avg = 300, var (population) = ((200^2 + 100^2 + 0 + 100^2 + 200^2)/5) = 20000
        // stdDev = sqrt(20000) ≈ 141.421
        StatisticsCalculator.Result r =
                calculator.compute(List.of(100L, 200L, 300L, 400L, 500L));

        assertThat(r.sampleCount()).isEqualTo(5);
        assertThat(r.avgPrice()).isEqualTo(300L);
        assertThat(r.minPrice()).isEqualTo(100L);
        assertThat(r.maxPrice()).isEqualTo(500L);
        assertThat(r.stdDeviation()).isCloseTo(141.4213, within(0.01));
    }

    @Test
    @DisplayName("compute: percentiles use nearest-rank method on sorted values")
    void compute_percentiles() {
        StatisticsCalculator.Result r =
                calculator.compute(List.of(10L, 20L, 30L, 40L, 50L, 60L, 70L, 80L));

        // sorted = [10,20,30,40,50,60,70,80], n=8
        // p25 (nearest-rank): ceil(0.25 * 8) = 2 → index 1 → 20
        // p75: ceil(0.75 * 8) = 6 → index 5 → 60
        assertThat(r.p25Price()).isEqualTo(20L);
        assertThat(r.p75Price()).isEqualTo(60L);
    }

    @Test
    @DisplayName("compute: unsorted input is sorted internally")
    void compute_unsortedInput() {
        StatisticsCalculator.Result r =
                calculator.compute(List.of(500L, 100L, 300L, 200L, 400L));

        assertThat(r.minPrice()).isEqualTo(100L);
        assertThat(r.maxPrice()).isEqualTo(500L);
        assertThat(r.avgPrice()).isEqualTo(300L);
    }

    @Test
    @DisplayName("compute: rejects empty list")
    void compute_emptyList() {
        assertThatThrownBy(() -> calculator.compute(List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("empty");
    }

    @Test
    @DisplayName("compute: rejects null")
    void compute_null() {
        assertThatThrownBy(() -> calculator.compute(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("compute: rejects non-positive prices")
    void compute_nonPositivePrice() {
        assertThatThrownBy(() -> calculator.compute(List.of(100L, 0L, 200L)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("positive");
    }

    private static org.assertj.core.data.Offset<Double> within(double v) {
        return org.assertj.core.data.Offset.offset(v);
    }
}
