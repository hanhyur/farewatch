package com.farewatch.application.judgment;

import com.farewatch.domain.fare.FareStatistics;
import com.farewatch.domain.judgment.FareVerdict;
import java.util.Objects;

/**
 * 현재 가격과 누적 통계를 받아 {@link FareVerdict} 중 하나를 반환하는 판정기.
 *
 * <p>순수 함수 — 외부 상태나 I/O 없음. Spring 없이 완전히 테스트 가능.
 *
 * <h2>판정 규칙</h2>
 *
 * <ol>
 *   <li>{@code sampleCount < requiredSamples} → {@link FareVerdict.Insufficient}
 *   <li>{@code stdDev == 0} → {@link FareVerdict.Fair} (통계 신호 없음)
 *   <li>{@code zScore < -cheapSigma} → {@link FareVerdict.Cheap}
 *   <li>{@code zScore > expensiveSigma} → {@link FareVerdict.Expensive}
 *   <li>그 외 → {@link FareVerdict.Fair}
 * </ol>
 *
 * <p>부등호는 <b>엄격(strict)</b>. 정확히 경계값(예: 가격이 avg − 1σ 와 동일)은 Fair
 * 로 분류되어, "확실한 신호" 만 행동을 유도한다.
 *
 * <h2>기본 임계값</h2>
 *
 * <ul>
 *   <li>{@link #DEFAULT_REQUIRED_SAMPLES} = 30 — 중심극한정리 기준 통상 최소 샘플 수
 *   <li>{@link #DEFAULT_CHEAP_SIGMA} = 1.0 — 평균보다 확실히 이하
 *   <li>{@link #DEFAULT_EXPENSIVE_SIGMA} = 0.5 — 평균보다 조금이라도 위면 경고
 * </ul>
 *
 * <p>Cheap 과 Expensive 임계값이 비대칭인 것은 의도적 — "비싸게 산 후회" 를 "싼 걸
 * 놓친 후회" 보다 더 가중치 두는 행동경제학 관찰을 반영.
 */
public class FareVerdictCalculator {

    public static final int DEFAULT_REQUIRED_SAMPLES = 30;
    public static final double DEFAULT_CHEAP_SIGMA = 1.0;
    public static final double DEFAULT_EXPENSIVE_SIGMA = 0.5;

    private final int requiredSamples;
    private final double cheapSigma;
    private final double expensiveSigma;

    public FareVerdictCalculator() {
        this(DEFAULT_REQUIRED_SAMPLES, DEFAULT_CHEAP_SIGMA, DEFAULT_EXPENSIVE_SIGMA);
    }

    public FareVerdictCalculator(int requiredSamples, double cheapSigma, double expensiveSigma) {
        if (requiredSamples <= 0) {
            throw new IllegalArgumentException(
                    "requiredSamples must be > 0, was " + requiredSamples);
        }
        if (cheapSigma <= 0) {
            throw new IllegalArgumentException("cheapSigma must be > 0, was " + cheapSigma);
        }
        if (expensiveSigma <= 0) {
            throw new IllegalArgumentException("expensiveSigma must be > 0, was " + expensiveSigma);
        }
        this.requiredSamples = requiredSamples;
        this.cheapSigma = cheapSigma;
        this.expensiveSigma = expensiveSigma;
    }

    /**
     * 현재 가격이 주어진 통계 대비 어떤 구매 판단에 해당하는지 계산한다.
     *
     * @param currentPrice 현재 항공권 가격 (원화, {@code > 0})
     * @param stats 해당 노선·출발일의 누적 가격 통계
     * @return 판정 결과 (Insufficient/Cheap/Fair/Expensive 중 하나)
     * @throws IllegalArgumentException {@code currentPrice <= 0}
     * @throws NullPointerException {@code stats == null}
     */
    public FareVerdict evaluate(long currentPrice, FareStatistics stats) {
        Objects.requireNonNull(stats, "stats must not be null");
        if (currentPrice <= 0) {
            throw new IllegalArgumentException("currentPrice must be > 0, was " + currentPrice);
        }

        if (stats.getSampleCount() < requiredSamples) {
            return new FareVerdict.Insufficient(stats.getSampleCount(), requiredSamples);
        }

        long avg = stats.getAvgPrice();
        double stdDev = stats.getStdDeviation();

        // stdDev == 0 이면 과거 분산이 0 — 통계적 신호 없음 → Fair
        // (FareVerdict.Cheap/Expensive invariant 는 zScore 가 finite 여야 함)
        if (stdDev == 0.0) {
            return new FareVerdict.Fair(currentPrice, avg);
        }

        double zScore = (currentPrice - avg) / stdDev;

        if (zScore < -cheapSigma) {
            return new FareVerdict.Cheap(currentPrice, avg, zScore);
        }
        if (zScore > expensiveSigma) {
            return new FareVerdict.Expensive(currentPrice, avg, zScore);
        }
        return new FareVerdict.Fair(currentPrice, avg);
    }
}
