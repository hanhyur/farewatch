package com.farewatch.domain.judgment;

import static org.assertj.core.api.Assertions.assertThat;

import com.farewatch.domain.shared.FareVerdictKind;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("FareVerdict sealed interface")
class FareVerdictTest {

    @Test
    @DisplayName("Cheap variant: kind() == CHEAP, zScore 포함")
    void cheapVariant() {
        FareVerdict v = new FareVerdict.Cheap(178_000L, 232_000L, -1.89);

        assertThat(v.kind()).isEqualTo(FareVerdictKind.CHEAP);
        assertThat(((FareVerdict.Cheap) v).zScore()).isEqualTo(-1.89);
    }

    @Test
    @DisplayName("Fair variant: kind() == FAIR, zScore 없음")
    void fairVariant() {
        FareVerdict v = new FareVerdict.Fair(220_000L, 232_000L);

        assertThat(v.kind()).isEqualTo(FareVerdictKind.FAIR);
    }

    @Test
    @DisplayName("Expensive variant: kind() == EXPENSIVE, zScore 포함")
    void expensiveVariant() {
        FareVerdict v = new FareVerdict.Expensive(260_000L, 232_000L, 0.98);

        assertThat(v.kind()).isEqualTo(FareVerdictKind.EXPENSIVE);
    }

    @Test
    @DisplayName("Insufficient variant: kind() == INSUFFICIENT, 샘플 수 포함")
    void insufficientVariant() {
        FareVerdict v = new FareVerdict.Insufficient(12, 30);

        assertThat(v.kind()).isEqualTo(FareVerdictKind.INSUFFICIENT);
    }

    @Test
    @DisplayName("Java 21 exhaustive switch로 모든 variant 처리")
    void exhaustiveSwitch() {
        FareVerdict[] verdicts = {
                new FareVerdict.Cheap(100, 200, -2.0),
                new FareVerdict.Fair(200, 200),
                new FareVerdict.Expensive(300, 200, 1.5),
                new FareVerdict.Insufficient(10, 30)
        };

        for (FareVerdict v : verdicts) {
            String label = switch (v) {
                case FareVerdict.Cheap c -> "cheap";
                case FareVerdict.Fair f -> "fair";
                case FareVerdict.Expensive e -> "expensive";
                case FareVerdict.Insufficient i -> "insufficient";
            };
            assertThat(label).isNotNull();
        }
    }
}
