package com.farewatch.application.alert;

import com.farewatch.domain.alert.AlertRule;
import com.farewatch.domain.fare.FareSnapshot;
import com.farewatch.domain.judgment.FareVerdict;
import com.farewatch.domain.shared.NotificationChannel;

/**
 * 알림 전송 포트. 채널별 구현체는 {@code infrastructure/notification/} 아래에 둔다.
 *
 * <p>v0 구현: {@code LogNotificationSender} (콘솔/로그 출력). 향후 EMAIL/SLACK/PUSH
 * 등으로 확장 시 동일 인터페이스를 구현하면 된다.
 *
 * <p>구현체는 외부 I/O 실패에도 호출자(이벤트 핸들러)를 깨뜨리지 않도록 자체적으로
 * try/catch + 로깅 책임을 진다 — 하나의 채널 장애가 알림 평가 루프 전체를 멈추는 일이
 * 없도록 한다.
 */
public interface NotificationSender {

    /**
     * 알림을 발송한다. 구현체가 동기/비동기/비-blocking 어느 쪽인지는 채널마다 다를 수
     * 있다. 호출자는 단발 호출 후 즉시 반환된다고 가정한다.
     */
    void send(AlertRule rule, FareVerdict verdict, FareSnapshot snapshot);

    /** 이 발송기가 다루는 채널. {@code Notification.channel} 컬럼에 기록된다. */
    NotificationChannel channel();
}
