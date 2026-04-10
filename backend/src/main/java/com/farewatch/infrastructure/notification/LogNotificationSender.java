package com.farewatch.infrastructure.notification;

import com.farewatch.application.alert.NotificationSender;
import com.farewatch.domain.alert.AlertRule;
import com.farewatch.domain.fare.FareSnapshot;
import com.farewatch.domain.judgment.FareVerdict;
import com.farewatch.domain.shared.NotificationChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 콘솔/로그 기반 {@link NotificationSender} 구현체. v0 단계에서 외부 채널 (이메일/슬랙)
 * 도입 전까지의 기본값.
 *
 * <p>발송 액션 자체가 단순 로그이므로 실패할 수 없다. 향후 채널 구현체는 자체 try/catch
 * + 로깅을 두어 예외가 호출자에게 전파되지 않도록 해야 한다.
 */
@Component
public class LogNotificationSender implements NotificationSender {

    private static final Logger log = LoggerFactory.getLogger(LogNotificationSender.class);

    @Override
    public void send(AlertRule rule, FareVerdict verdict, FareSnapshot snapshot) {
        log.info(
                "[ALERT] user={} routeId={} departureDate={} verdict={} price={} KRW",
                rule.getUserIdentifier().value(),
                rule.getRouteId(),
                snapshot.getDepartureDate(),
                verdict.kind(),
                snapshot.getPrice().amount());
    }

    @Override
    public NotificationChannel channel() {
        return NotificationChannel.LOG;
    }
}
