package com.farewatch.domain.shared;

/**
 * {@code FareVerdict} sealed interface의 DB 저장용 enum 표현.
 *
 * <p>sealed interface는 직접 영속화할 수 없으므로, 판단 결과를 {@code Notification} 등에
 * 기록할 때는 이 enum으로 변환해서 저장한다.
 */
public enum FareVerdictKind {
    CHEAP,
    FAIR,
    EXPENSIVE,
    INSUFFICIENT
}
