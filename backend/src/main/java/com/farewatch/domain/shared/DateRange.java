package com.farewatch.domain.shared;

import jakarta.persistence.Embeddable;
import java.time.LocalDate;
import java.util.Objects;

/**
 * 날짜 범위 (inclusive). 알림 규칙의 출발일 범위 등에 사용.
 *
 * <p>{@code from} 과 {@code to} 모두 포함된다. {@code from == to} 이면 하루짜리 범위.
 */
@Embeddable
public record DateRange(LocalDate from, LocalDate to) {

    public DateRange {
        Objects.requireNonNull(from, "from must not be null");
        Objects.requireNonNull(to, "to must not be null");
        if (to.isBefore(from)) {
            throw new IllegalArgumentException(
                    "to (" + to + ") must be >= from (" + from + ")");
        }
    }

    public boolean contains(LocalDate date) {
        Objects.requireNonNull(date);
        return !date.isBefore(from) && !date.isAfter(to);
    }
}
