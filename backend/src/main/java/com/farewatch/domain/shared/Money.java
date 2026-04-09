package com.farewatch.domain.shared;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.Objects;

/**
 * 화폐 값. 정수 기반 저장 (원화는 소수점이 없음).
 *
 * <p>v0에서는 통화 코드를 문자열로만 보관하고, 실제 환전/통화 변환은 다루지 않는다.
 * 통계 엔티티({@code FareStatistics})는 여러 가격 컬럼의 embeddable 반복을 피하기 위해
 * plain {@code long}을 사용하고, 단일 가격 필드만 {@code Money}로 감싼다.
 *
 * <p><b>Invariant:</b> {@code amount > 0}, {@code currency} 는 non-null + non-blank +
 * 정확히 3글자 ISO 코드. FareWatch 도메인에서 Money는 항상 "실제 거래 가능한 금액"을
 * 의미하며, 0원이나 음수 금액은 어떤 소비자에게도 의미가 없다. 따라서 VO 자체에서
 * strict positive를 강제한다 (이전에는 {@code >= 0} 허용 후 소비자가 중복 검증).
 */
@Embeddable
public record Money(
        @Column(nullable = false) long amount,
        @Column(length = 3, nullable = false) String currency) {

    public Money {
        if (amount <= 0) {
            throw new IllegalArgumentException("amount must be > 0, but was " + amount);
        }
        Objects.requireNonNull(currency, "currency must not be null");
        if (currency.length() != 3 || currency.isBlank()) {
            throw new IllegalArgumentException(
                    "currency must be a 3-char ISO code, but was '" + currency + "'");
        }
    }

    public static Money krw(long amount) {
        return new Money(amount, "KRW");
    }
}
