package com.farewatch.application.judgment;

import com.farewatch.domain.fare.FareStatistics;
import com.farewatch.domain.judgment.FareVerdict;
import java.util.Objects;

/**
 * 현재 가격과 누적 통계를 받아 {@link FareVerdict} 중 하나를 반환하는 판정기.
 *
 * <p>순수 함수 — 외부 상태나 I/O 없음. Spring 없이 완전히 테스트 가능.
 *
 * <p><b>Thread-safety:</b> 모든 필드가 {@code final} + primitive 이므로 인스턴스는
 * 완전한 불변 객체. 여러 스레드에서 동일 인스턴스를 공유해도 안전하다. Spring 컨테
 * 이너에서 싱글톤 빈으로 등록해 사용하는 것을 권장.
 *
 * <h2>판정 규칙</h2>
 *
 * <ol>
 *   <li>{@code sampleCount < requiredSamples} → {@link FareVerdict.Insufficient}
 *   <li>{@code stdDev < MIN_MEANINGFUL_STD_DEV} → {@link FareVerdict.Fair}
 *       (통계 신호 없음 / 수치 노이즈 차단)
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

    /**
     * 통계적으로 의미 있는 표준편차의 최소값 (단위: 원).
     *
     * <p>KRW 가격은 정수로 저장되므로 {@code stdDev < 1.0} 은 사실상 "모든 샘플이
     * 동일" 과 같다. 이보다 작은 값은 부동소수점 계산 노이즈 (예: SQL 집계 결과의
     * 반올림 오차) 거나 samples 가 모두 같은 경우로 간주되어 "통계 신호 없음" 으로
     * 처리된다. 정확한 {@code 0.0} 비교 대신 epsilon 가드를 쓰는 이유.
     */
    public static final double MIN_MEANINGFUL_STD_DEV = 1.0;

    private final int requiredSamples;
    private final double cheapSigma;
    private final double expensiveSigma;

    /**
     * 기본 임계값으로 생성.
     *
     * <p><b>Spring {@code @ConfigurationProperties} 주의</b>: 이 클래스를 설정
     * 프로퍼티로 바인딩하려면 3-인자 생성자에 {@code @ConstructorBinding} 을
     * 붙여서 사용해야 한다. 이 no-arg 생성자는 Spring 바인딩에 사용되지 않는다
     * (final 필드라 바인딩 후 값 세팅 불가). 직접 생성용.
     */
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
     * <p>구현 노트: 현재 항공권 가격은 원화 정수이고, 현실적으로 수백만 원을 넘지
     * 않는다. {@code currentPrice} 가 ~10^12 이상이 되면 double 연산에서 정밀도
     * 손실이 발생할 수 있지만, 그런 값은 이 시스템의 정상 입력 범위가 아니다.
     *
     * @param currentPrice 현재 항공권 가격 (원화, {@code > 0}, 현실적 최대값 수백만 원)
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

        // 분산이 통계적으로 의미 없는 수준(< 1원)이면 신호 없음 → Fair.
        // FareStatistics 가 NaN/Infinity 를 거부하므로 여기서는 finite 는 전제.
        // epsilon 비교를 쓰는 이유는 실수 연산 노이즈(1e-15 같은 값)로 인한 오탐 방지.
        if (stdDev < MIN_MEANINGFUL_STD_DEV) {
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
