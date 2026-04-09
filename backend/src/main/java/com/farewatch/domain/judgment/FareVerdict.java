package com.farewatch.domain.judgment;

import com.farewatch.domain.shared.FareVerdictKind;

/**
 * 현재 가격이 통계와 비교했을 때 어떤 구매 판단에 해당하는지.
 *
 * <p>sealed interface로 가능한 판단 종류를 4개로 제한한다. 외부에서 새 variant를 추가할
 * 수 없으므로, 새로운 판단이 필요해지면 반드시 이 파일을 수정해야 한다 — pattern
 * matching switch가 컴파일 에러를 유발해 누락을 방지한다.
 *
 * <p>이 인터페이스는 DB에 직접 영속화되지 않는다. 저장이 필요한 경우
 * {@link FareVerdictKind} enum으로 변환해서 기록한다.
 */
public sealed interface FareVerdict
        permits FareVerdict.Cheap,
                FareVerdict.Fair,
                FareVerdict.Expensive,
                FareVerdict.Insufficient {

    FareVerdictKind kind();

    /** "지금 사세요" — 평균보다 1σ 이상 저렴. */
    record Cheap(long currentPrice, long avgPrice, double zScore) implements FareVerdict {
        public Cheap {
            if (currentPrice <= 0 || avgPrice <= 0) {
                throw new IllegalArgumentException("prices must be > 0");
            }
            if (!Double.isFinite(zScore)) {
                throw new IllegalArgumentException("zScore must be finite, was " + zScore);
            }
        }

        @Override
        public FareVerdictKind kind() {
            return FareVerdictKind.CHEAP;
        }
    }

    /** "적정가" — 평균 ±0.5σ 밴드 안. */
    record Fair(long currentPrice, long avgPrice) implements FareVerdict {
        public Fair {
            if (currentPrice <= 0 || avgPrice <= 0) {
                throw new IllegalArgumentException("prices must be > 0");
            }
        }

        @Override
        public FareVerdictKind kind() {
            return FareVerdictKind.FAIR;
        }
    }

    /** "더 기다리세요" — 평균보다 0.5σ 이상 비쌈. */
    record Expensive(long currentPrice, long avgPrice, double zScore) implements FareVerdict {
        public Expensive {
            if (currentPrice <= 0 || avgPrice <= 0) {
                throw new IllegalArgumentException("prices must be > 0");
            }
            if (!Double.isFinite(zScore)) {
                throw new IllegalArgumentException("zScore must be finite, was " + zScore);
            }
        }

        @Override
        public FareVerdictKind kind() {
            return FareVerdictKind.EXPENSIVE;
        }
    }

    /** "데이터 부족" — 통계적으로 유의미한 표본 수 미달. */
    record Insufficient(int sampleCount, int requiredCount) implements FareVerdict {
        public Insufficient {
            if (sampleCount < 0) {
                throw new IllegalArgumentException("sampleCount must be >= 0, was " + sampleCount);
            }
            if (requiredCount <= 0) {
                throw new IllegalArgumentException("requiredCount must be > 0, was " + requiredCount);
            }
        }

        @Override
        public FareVerdictKind kind() {
            return FareVerdictKind.INSUFFICIENT;
        }
    }
}
