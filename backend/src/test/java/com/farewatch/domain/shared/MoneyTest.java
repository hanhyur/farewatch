package com.farewatch.domain.shared;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Money VO")
class MoneyTest {

    @Test
    @DisplayName("양수 금액과 통화로 생성 가능")
    void acceptsPositiveAmount() {
        Money m = new Money(178_000L, "KRW");

        assertThat(m.amount()).isEqualTo(178_000L);
        assertThat(m.currency()).isEqualTo("KRW");
    }

    @Test
    @DisplayName("0원 허용 (amount >= 0)")
    void acceptsZero() {
        assertThat(new Money(0L, "KRW").amount()).isZero();
    }

    @Test
    @DisplayName("음수 금액 거부")
    void rejectsNegativeAmount() {
        assertThatThrownBy(() -> new Money(-1L, "KRW"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("amount");
    }

    @Test
    @DisplayName("null currency 거부")
    void rejectsNullCurrency() {
        assertThatThrownBy(() -> new Money(1000L, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Money.krw(amount) 팩토리로 KRW 화폐 생성")
    void krwFactory() {
        Money m = Money.krw(232_000L);

        assertThat(m.amount()).isEqualTo(232_000L);
        assertThat(m.currency()).isEqualTo("KRW");
    }

    @Test
    @DisplayName("같은 값의 두 Money는 equals 이다")
    void equalsByValue() {
        assertThat(Money.krw(100L)).isEqualTo(Money.krw(100L));
        assertThat(Money.krw(100L)).isNotEqualTo(Money.krw(200L));
        assertThat(new Money(100L, "KRW")).isNotEqualTo(new Money(100L, "JPY"));
    }
}
