package com.farewatch.application.judgment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
            FareVerdict.Insufficient insufficient = (FareVerdict.Insufficient) result;
            assertThat(insufficient.sampleCount()).isEqualTo(10);
            assertThat(insufficient.requiredCount()).isEqualTo(30);
            assertThat(result.kind()).isEqualTo(FareVerdictKind.INSUFFICIENT);
        }

        @Test
        @DisplayName("currentPrice << avg − 1σ → Cheap (zScore < -1.0)")
        void cheapWhenWellBelowOneSigma() {
            // avg=200k, stdDev=20k → -1σ 경계 = 180k
            FareVerdict result = calculator.evaluate(170_000L, defaultStats());

            assertThat(result).isInstanceOf(FareVerdict.Cheap.class);
            FareVerdict.Cheap cheap = (FareVerdict.Cheap) result;
            assertThat(cheap.currentPrice()).isEqualTo(170_000L);
            assertThat(cheap.avgPrice()).isEqualTo(200_000L);
            assertThat(cheap.zScore()).isEqualTo(-1.5);
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
            // avg=200k, stdDev=20k → +0.5σ 경계 = 210k
            FareVerdict result = calculator.evaluate(230_000L, defaultStats());

            assertThat(result).isInstanceOf(FareVerdict.Expensive.class);
            FareVerdict.Expensive expensive = (FareVerdict.Expensive) result;
            assertThat(expensive.currentPrice()).isEqualTo(230_000L);
            assertThat(expensive.avgPrice()).isEqualTo(200_000L);
            assertThat(expensive.zScore()).isEqualTo(1.5);
            assertThat(result.kind()).isEqualTo(FareVerdictKind.EXPENSIVE);
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // Boundary conditions (strict inequality 검증)
    // ─────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("경계값 (strict 부등호)")
    class Boundary {

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
        @DisplayName("stdDev == 0 + price != avg → Fair (통계 신호 없음)")
        void zeroStdDevWithDifferentPriceIsFair() {
            FareStatistics s = stats(200_000L, 200_000L, 200_000L, 0.0, 40);

            // price 가 50% 싸도, 분산이 0이라 통계적 신호 없음 → Fair
            FareVerdict cheap = calculator.evaluate(100_000L, s);
            FareVerdict expensive = calculator.evaluate(400_000L, s);

            assertThat(cheap).isInstanceOf(FareVerdict.Fair.class);
            assertThat(expensive).isInstanceOf(FareVerdict.Fair.class);
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
            FareVerdict at180 = strict.evaluate(170_000L, defaultStats()); // zScore=-1.5
            FareVerdict at160 = strict.evaluate(159_000L, defaultStats()); // zScore≈-2.05

            assertThat(at180).isInstanceOf(FareVerdict.Fair.class);
            assertThat(at160).isInstanceOf(FareVerdict.Cheap.class);
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
