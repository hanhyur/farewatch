package com.farewatch.domain.shared;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * 항공사 IATA 공항 코드 (3글자 대문자). 예: {@code PUS}, {@code NRT}.
 *
 * <p>Route 엔티티에서 origin/destination 두 번 임베드되므로, 사용처에서
 * {@code @AttributeOverride}로 컬럼명을 구분해야 한다.
 */
@Embeddable
public record AirportCode(@Column(length = 3, nullable = false) String value) {

    private static final Pattern IATA_PATTERN = Pattern.compile("^[A-Z]{3}$");

    public AirportCode {
        Objects.requireNonNull(value, "IATA code must not be null");
        if (!IATA_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException(
                    "Invalid IATA code: '" + value + "' (must be 3 uppercase letters)");
        }
    }
}
