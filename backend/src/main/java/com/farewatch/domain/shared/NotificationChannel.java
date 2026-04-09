package com.farewatch.domain.shared;

/**
 * 알림 전송 채널. v0에서는 콘솔 로그만 지원.
 *
 * <p>향후 EMAIL, SLACK, PUSH 등이 추가될 때 이 enum에 값만 추가하면 된다. 실제 전송은
 * {@code NotificationSender} 인터페이스의 구현체가 담당한다.
 */
public enum NotificationChannel {
    LOG
}
