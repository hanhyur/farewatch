package com.farewatch.domain.shared;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("AirportCode VO")
class AirportCodeTest {

    @Test
    @DisplayName("유효한 3글자 대문자 IATA 코드를 수락한다")
    void acceptsValidIataCode() {
        AirportCode code = new AirportCode("PUS");

        assertThat(code.value()).isEqualTo("PUS");
    }

    @Test
    @DisplayName("두 인스턴스가 동일한 값이면 equals 이다 (record 기본 동작)")
    void equalsByValue() {
        assertThat(new AirportCode("NRT")).isEqualTo(new AirportCode("NRT"));
        assertThat(new AirportCode("NRT")).isNotEqualTo(new AirportCode("HND"));
    }

    @ParameterizedTest(name = "잘못된 IATA 코드 거부: \"{0}\"")
    @ValueSource(strings = {
            "pus",      // 소문자
            "PU",       // 2글자
            "PUSS",     // 4글자
            "P U",      // 공백 포함
            "123",      // 숫자
            "PU1",      // 숫자 포함
            ""          // 빈 문자열
    })
    void rejectsInvalidFormat(String invalid) {
        assertThatThrownBy(() -> new AirportCode(invalid))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("IATA");
    }

    @ParameterizedTest
    @NullSource
    @DisplayName("null 값 거부")
    void rejectsNull(String nullValue) {
        assertThatThrownBy(() -> new AirportCode(nullValue))
                .isInstanceOf(NullPointerException.class);
    }
}
