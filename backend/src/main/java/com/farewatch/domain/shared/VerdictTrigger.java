package com.farewatch.domain.shared;

/**
 * AlertRule이 발동되는 판단 조건.
 *
 * <ul>
 *   <li>{@link #CHEAP} — "싸다"고 판단될 때만 알림
 *   <li>{@link #CHEAP_OR_FAIR} — "싸거나 적정가"일 때 알림 (더 느슨)
 * </ul>
 */
public enum VerdictTrigger {
    CHEAP,
    CHEAP_OR_FAIR
}
