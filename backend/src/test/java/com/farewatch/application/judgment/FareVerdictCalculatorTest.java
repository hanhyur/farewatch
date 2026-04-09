package com.farewatch.application.judgment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import com.farewatch.domain.fare.FareStatistics;
import com.farewatch.domain.judgment.FareVerdict;
import com.farewatch.domain.shared.FareVerdictKind;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("FareVerdictCalculator (application/judgment)")
class FareVerdictCalculatorTest {

    private final FareVerdictCalculator calculator = new FareVerdictCalculator();

    private static final Long ROUTE_ID = 1L;
    private static final LocalDate DEP = LocalDate.of(2026, 5, 15);

    /** avg=200k, stdDev=20k, sampleCount=40 — Cheap 임계값은 180k, Expensive 임계값은 210k. */
    private static FareStatistics stats(long avg, long min, long max, double stdDev, int sampleCount) {
        return FareStatistics.compute(ROUTE_ID, DEP, avg, min, max, stdDev, sampleCount, null, null);
    }

    private static FareStatistics defaultStats() {
        return stats(200_000L, 150_000L, 260_000L, 20_000.0, 40);
    }

    // ─────────────────────────────────────────────────────────────────
    // Happy path
    // ─────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("기본 판정 로직")
    class HappyPath {

        @Test
        @DisplayName("sampleCount < 30 → Insufficient")
        void insufficientWhenFewSamples() {
            FareStatistics s = stats(200_000L, 150_000L, 260_000L, 20_000.0, 10);

            FareVerdict result = calculator.evaluate(178_000L, s);

            assertThat(result).isInstanceOf(FareVerdict.Insufficient.class);
            if (result instanceof FareVerdict.Insufficient insufficient) {
                assertThat(insufficient.sampleCount()).isEqualTo(10);
                assertThat(insufficient.requiredCount()).isEqualTo(30);
            }
            assertThat(result.kind()).isEqualTo(FareVerdictKind.INSUFFICIENT);
        }

        @Test
        @DisplayName("currentPrice << avg − 1σ → Cheap (zScore < -1.0)")
        void cheapWhenWellBelowOneSigma() {
            // avg=200k, stdDev=20k → -1σ 경계 = 180k, 170k 는 확실히 그 아래
            FareVerdict result = calculator.evaluate(170_000L, defaultStats());

            assertThat(result).isInstanceOf(FareVerdict.Cheap.class);
            if (result instanceof FareVerdict.Cheap cheap) {
                assertThat(cheap.currentPrice()).isEqualTo(170_000L);
                assertThat(cheap.avgPrice()).isEqualTo(200_000L);
                assertThat(cheap.zScore()).isCloseTo(-1.5, within(1e-9));
            }
            assertThat(result.kind()).isEqualTo(FareVerdictKind.CHEAP);
        }

        @Test
        @DisplayName("currentPrice ≈ avg → Fair")
        void fairAtAverage() {
            FareVerdict result = calculator.evaluate(200_000L, defaultStats());

            assertThat(result).isInstanceOf(FareVerdict.Fair.class);
            assertThat(result.kind()).isEqualTo(FareVerdictKind.FAIR);
        }

        @Test
        @DisplayName("currentPrice >> avg + 0.5σ → Expensive (zScore > +0.5)")
        void expensiveWhenWellAboveHalfSigma() {
            // avg=200k, stdDev=20k → +0.5σ 경계 = 210k, 230k 는 확실히 그 위
            FareVerdict result = calculator.evaluate(230_000L, defaultStats());

            assertThat(result).isInstanceOf(FareVerdict.Expensive.class);
            if (result instanceof FareVerdict.Expensive expensive) {
                assertThat(expensive.currentPrice()).isEqualTo(230_000L);
                assertThat(expensive.avgPrice()).isEqualTo(200_000L);
                assertThat(expensive.zScore()).isCloseTo(1.5, within(1e-9));
            }
            assertThat(result.kind()).isEqualTo(FareVerdictKind.EXPENSIVE);
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // Boundary conditions (strict inequality 검증)
    // ─────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("경계값 (strict 부등호)")
    class Boundary {

        // 참고: 이 테스트들의 가격/stdDev 조합 (20_000, 20_001 등) 은 모두 IEEE 754
        // double 에 정확히 표현되는 정수 값이므로 부동소수점 반올림이 결과에 영향을
        // 주지 않는다. stdDev 를 2의 거듭제곱이 아닌 값 (예: 3_333) 으로 바꿀 때는
        // 경계값을 다시 계산해야 한다.

        @Test
        @DisplayName("price = avg − 1σ 정확히 → Fair (Cheap 아님, strict <)")
        void exactlyMinusOneSigmaIsFair() {
            // avg=200k, stdDev=20k → avg−1σ = 180k
            FareVerdict result = calculator.evaluate(180_000L, defaultStats());

            assertThat(result).isInstanceOf(FareVerdict.Fair.class);
        }

        @Test
        @DisplayName("price = avg − 1σ − 1 → Cheap")
        void justBelowMinusOneSigmaIsCheap() {
            FareVerdict result = calculator.evaluate(179_999L, defaultStats());

            assertThat(result).isInstanceOf(FareVerdict.Cheap.class);
        }

        @Test
        @DisplayName("price = avg + 0.5σ 정확히 → Fair (Expensive 아님, strict >)")
        void exactlyPlusHalfSigmaIsFair() {
            // avg=200k, stdDev=20k → avg+0.5σ = 210k
            FareVerdict result = calculator.evaluate(210_000L, defaultStats());

            assertThat(result).isInstanceOf(FareVerdict.Fair.class);
        }

        @Test
        @DisplayName("price = avg + 0.5σ + 1 → Expensive")
        void justAbovePlusHalfSigmaIsExpensive() {
            FareVerdict result = calculator.evaluate(210_001L, defaultStats());

            assertThat(result).isInstanceOf(FareVerdict.Expensive.class);
        }

        @Test
        @DisplayName("sampleCount = 29 → Insufficient")
        void sampleCount29IsInsufficient() {
            FareStatistics s = stats(200_000L, 150_000L, 260_000L, 20_000.0, 29);

            FareVerdict result = calculator.evaluate(170_000L, s);

            assertThat(result).isInstanceOf(FareVerdict.Insufficient.class);
        }

        @Test
        @DisplayName("sampleCount = 30 정확히 → Insufficient 아님 (통계 평가 진행)")
        void sampleCount30IsEnough() {
            FareStatistics s = stats(200_000L, 150_000L, 260_000L, 20_000.0, 30);

            FareVerdict result = calculator.evaluate(170_000L, s);

            assertThat(result).isInstanceOf(FareVerdict.Cheap.class);
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // Edge cases
    // ─────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("엣지 케이스")
    class EdgeCases {

        @Test
        @DisplayName("stdDev == 0 + price == avg → Fair")
        void zeroStdDevAtAverage() {
            FareStatistics s = stats(200_000L, 200_000L, 200_000L, 0.0, 40);

            FareVerdict result = calculator.evaluate(200_000L, s);

            assertThat(result).isInstanceOf(FareVerdict.Fair.class);
        }

        @Test
        @DisplayName("stdDev == 0 + price 50% 하락 → Fair (통계 신호 없음)")
        void zeroStdDevWith50PercentCheaperIsFair() {
            FareStatistics s = stats(200_000L, 200_000L, 200_000L, 0.0, 40);

            FareVerdict result = calculator.evaluate(100_000L, s);

            assertThat(result).isInstanceOf(FareVerdict.Fair.class);
        }

        @Test
        @DisplayName("stdDev == 0 + price 100% 상승 → Fair (통계 신호 없음)")
        void zeroStdDevWith100PercentExpensiveIsFair() {
            FareStatistics s = stats(200_000L, 200_000L, 200_000L, 0.0, 40);

            FareVerdict result = calculator.evaluate(400_000L, s);

            assertThat(result).isInstanceOf(FareVerdict.Fair.class);
        }

        @Test
        @DisplayName("stdDev 가 0보다 크지만 MIN_MEANINGFUL_STD_DEV 미만 → Fair (노이즈 차단)")
        void subEpsilonStdDevIsFair() {
            // stdDev = 0.5 원. 실질적으로 변동 없음. epsilon 가드가 작동해야 함.
            // 정확한 0.0 비교였으면 zScore 가 천문학적 수가 돼서 Cheap 오탐 발생.
            FareStatistics s = stats(200_000L, 200_000L, 200_000L, 0.5, 40);

            FareVerdict result = calculator.evaluate(100_000L, s);

            assertThat(result).isInstanceOf(FareVerdict.Fair.class);
        }

        @Test
        @DisplayName("currentPrice = 0 거부")
        void rejectsZeroPrice() {
            assertThatThrownBy(() -> calculator.evaluate(0L, defaultStats()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("currentPrice");
        }

        @Test
        @DisplayName("currentPrice < 0 거부")
        void rejectsNegativePrice() {
            assertThatThrownBy(() -> calculator.evaluate(-1L, defaultStats()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("currentPrice");
        }

        @Test
        @DisplayName("FareStatistics null 거부")
        void rejectsNullStats() {
            assertThatThrownBy(() -> calculator.evaluate(200_000L, null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // Custom thresholds
    // ─────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("커스텀 임계값")
    class CustomThresholds {

        @Test
        @DisplayName("requiredSamples=10 으로 생성 시 10 샘플로 판정 진행")
        void customRequiredSamples() {
            FareVerdictCalculator custom = new FareVerdictCalculator(10, 1.0, 0.5);
            FareStatistics s = stats(200_000L, 150_000L, 260_000L, 20_000.0, 10);

            FareVerdict result = custom.evaluate(170_000L, s);

            assertThat(result).isInstanceOf(FareVerdict.Cheap.class);
        }

        @Test
        @DisplayName("cheapSigma=2.0 으로 더 엄격한 기준 → -1σ 가격은 Fair")
        void stricterCheapThreshold() {
            FareVerdictCalculator strict = new FareVerdictCalculator(30, 2.0, 0.5);

            // avg=200k, stdDev=20k → -1σ=180k, -2σ=160k
            FareVerdict at170 = strict.evaluate(170_000L, defaultStats()); // zScore=-1.5
            FareVerdict at159 = strict.evaluate(159_000L, defaultStats()); // zScore≈-2.05

            assertThat(at170).isInstanceOf(FareVerdict.Fair.class);
            assertThat(at159).isInstanceOf(FareVerdict.Cheap.class);
        }

        @Test
        @DisplayName("expensiveSigma=1.5 으로 더 엄격한 기준 → +1σ 가격은 Fair")
        void stricterExpensiveThreshold() {
            FareVerdictCalculator strict = new FareVerdictCalculator(30, 1.0, 1.5);

            // avg=200k, stdDev=20k → +0.5σ=210k, +1σ=220k, +1.5σ=230k
            FareVerdict at220 = strict.evaluate(220_000L, defaultStats()); // zScore=1.0
            FareVerdict at231 = strict.evaluate(231_000L, defaultStats()); // zScore≈1.55

            assertThat(at220).isInstanceOf(FareVerdict.Fair.class);
            assertThat(at231).isInstanceOf(FareVerdict.Expensive.class);
        }

        @Test
        @DisplayName("requiredSamples <= 0 거부")
        void rejectsInvalidRequiredSamples() {
            assertThatThrownBy(() -> new FareVerdictCalculator(0, 1.0, 0.5))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> new FareVerdictCalculator(-5, 1.0, 0.5))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("cheapSigma/expensiveSigma <= 0 거부")
        void rejectsInvalidSigma() {
            assertThatThrownBy(() -> new FareVerdictCalculator(30, 0.0, 0.5))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> new FareVerdictCalculator(30, 1.0, -1.0))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
