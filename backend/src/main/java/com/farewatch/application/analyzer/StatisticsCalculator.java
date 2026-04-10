package com.farewatch.application.analyzer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Component;

/**
 * 가격 리스트로부터 {@link Result} 통계 값을 계산하는 순수 함수 컴포넌트.
 *
 * <p>외부 상태 없음 — 입력 리스트만으로 결정된다. 도메인 외부 의존성이 없으므로 단위
 * 테스트가 자유롭다. Spring 빈으로 등록되지만 어떤 컨테이너 기능도 사용하지 않는다.
 *
 * <p><b>표준편차 정의</b>: 모집단(population) 표준편차를 사용한다. 우리가 가진
 * 가격 리스트는 "특정 (route, departureDate) 조합의 누적 관측 전체" 이므로 표본
 * 추정치가 아닌 모집단 그 자체로 보는 것이 자연스럽다. {@code FareVerdictCalculator}
 * 가 zScore 를 같은 정의로 해석하므로 일관성이 유지된다.
 *
 * <p><b>백분위수 정의</b>: nearest-rank 방법 (NIST 권장 단순 정의). 정렬된 표본
 * {@code x[1..n]} 에 대해 p% 백분위수는 인덱스 {@code ceil(p/100 × n)} 의 값. 작은
 * 표본에서도 안정적이고, 보간(interpolation) 방법보다 구현·검증이 단순하다.
 */
@Component
public class StatisticsCalculator {

    /**
     * 통계 계산 결과. 모든 가격 단위는 원화 정수.
     *
     * <p>{@code stdDeviation} 만 double — z-score 계산을 위한 실수 값. 나머지 가격은
     * {@code long} 으로 통일.
     */
    public record Result(
            int sampleCount,
            long avgPrice,
            long minPrice,
            long maxPrice,
            double stdDeviation,
            long p25Price,
            long p75Price) {}

    /**
     * 가격 리스트로부터 통계를 계산한다.
     *
     * @param prices 원화 가격 리스트 (모두 {@code > 0})
     * @throws NullPointerException prices == null
     * @throws IllegalArgumentException 빈 리스트이거나 음수/0 가격을 포함
     */
    public Result compute(List<Long> prices) {
        Objects.requireNonNull(prices, "prices must not be null");
        if (prices.isEmpty()) {
            throw new IllegalArgumentException("prices must not be empty");
        }

        // 입력 무결성: 가격은 항상 양수.
        for (Long p : prices) {
            if (p == null || p <= 0) {
                throw new IllegalArgumentException("prices must be positive, found " + p);
            }
        }

        // 정렬은 min/max/percentile 모두에 필요. 입력 리스트를 변경하지 않도록 복사.
        List<Long> sorted = new ArrayList<>(prices);
        Collections.sort(sorted);

        int n = sorted.size();
        long min = sorted.get(0);
        long max = sorted.get(n - 1);

        // 합산은 long 누적 — 항공권 가격(<10^7) × 샘플 수가 long 범위를 초과할 일은 없음.
        long sum = 0L;
        for (long p : sorted) {
            sum += p;
        }
        long avg = sum / n;

        // 모집단 분산: Σ(x - mean)² / n. mean 은 double 로 정확하게 사용.
        double mean = (double) sum / n;
        double sqDiffSum = 0.0;
        for (long p : sorted) {
            double d = p - mean;
            sqDiffSum += d * d;
        }
        double stdDev = Math.sqrt(sqDiffSum / n);

        long p25 = nearestRank(sorted, 25);
        long p75 = nearestRank(sorted, 75);

        return new Result(n, avg, min, max, stdDev, p25, p75);
    }

    /**
     * Nearest-rank percentile. {@code rank = ceil(p/100 × n)}, 1-based 인덱스를 0-based
     * 로 변환해 반환한다.
     */
    private static long nearestRank(List<Long> sorted, int percentile) {
        int n = sorted.size();
        int rank = (int) Math.ceil((percentile / 100.0) * n);
        if (rank < 1) {
            rank = 1;
        }
        if (rank > n) {
            rank = n;
        }
        return sorted.get(rank - 1);
    }
}
