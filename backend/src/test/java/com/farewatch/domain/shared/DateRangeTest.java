package com.farewatch.domain.shared;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("DateRange VO")
class DateRangeTest {

    private static final LocalDate D1 = LocalDate.of(2026, 5, 1);
    private static final LocalDate D2 = LocalDate.of(2026, 5, 10);

    @Test
    @DisplayName("from <= to 를 수락")
    void acceptsValidRange() {
        DateRange range = new DateRange(D1, D2);

        assertThat(range.from()).isEqualTo(D1);
        assertThat(range.to()).isEqualTo(D2);
    }

    @Test
    @DisplayName("from == to 동일한 하루 범위 허용")
    void acceptsSameDayRange() {
        DateRange range = new DateRange(D1, D1);

        assertThat(range.from()).isEqualTo(range.to());
    }

    @Test
    @DisplayName("역전된 범위 (to < from) 거부")
    void rejectsInvertedRange() {
        assertThatThrownBy(() -> new DateRange(D2, D1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("from");
    }

    @Test
    @DisplayName("null from/to 거부")
    void rejectsNull() {
        assertThatThrownBy(() -> new DateRange(null, D2)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new DateRange(D1, null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("contains(): 경계 포함 (inclusive)")
    void containsInclusive() {
        DateRange range = new DateRange(D1, D2);

        assertThat(range.contains(D1)).isTrue();
        assertThat(range.contains(D2)).isTrue();
        assertThat(range.contains(D1.plusDays(5))).isTrue();
        assertThat(range.contains(D1.minusDays(1))).isFalse();
        assertThat(range.contains(D2.plusDays(1))).isFalse();
    }
}
