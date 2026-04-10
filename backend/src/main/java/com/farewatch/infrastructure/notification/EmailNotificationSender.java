package com.farewatch.infrastructure.notification;

import com.farewatch.application.alert.NotificationSender;
import com.farewatch.domain.alert.AlertRule;
import com.farewatch.domain.fare.FareSnapshot;
import com.farewatch.domain.judgment.FareVerdict;
import com.farewatch.domain.shared.NotificationChannel;
import java.text.NumberFormat;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/**
 * SMTP 이메일 알림 발송기.
 *
 * <p>{@code spring.mail.host} 프로퍼티가 설정되어 있을 때만 활성화된다. 미설정 시
 * {@link LogNotificationSender} 만 동작한다.
 *
 * <p>발송 실패 시 예외를 삼키고 로그만 남긴다 — 이메일 서버 장애가 알림 평가 루프 전체를
 * 멈추는 일이 없도록 한다.
 */
@Component
@ConditionalOnProperty("spring.mail.host")
public class EmailNotificationSender implements NotificationSender {

    private static final Logger log = LoggerFactory.getLogger(EmailNotificationSender.class);
    private static final NumberFormat KRW_FORMAT = NumberFormat.getNumberInstance(Locale.KOREA);

    private final JavaMailSender mailSender;

    public EmailNotificationSender(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public void send(AlertRule rule, FareVerdict verdict, FareSnapshot snapshot) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(rule.getUserIdentifier().value());
            message.setSubject(buildSubject(verdict, snapshot));
            message.setText(buildBody(verdict, snapshot));

            mailSender.send(message);
            log.info(
                    "[EMAIL] sent to {} for routeId={}, departureDate={}, verdict={}",
                    rule.getUserIdentifier().value(),
                    rule.getRouteId(),
                    snapshot.getDepartureDate(),
                    verdict.kind());
        } catch (MailException e) {
            log.warn(
                    "[EMAIL] failed to send to {}: {}",
                    rule.getUserIdentifier().value(),
                    e.getMessage());
        }
    }

    @Override
    public NotificationChannel channel() {
        return NotificationChannel.EMAIL;
    }

    private static String buildSubject(FareVerdict verdict, FareSnapshot snapshot) {
        return switch (verdict) {
            case FareVerdict.Cheap c ->
                    "[FareWatch] 특가 알림 — "
                            + KRW_FORMAT.format(c.currentPrice())
                            + "원 ("
                            + snapshot.getDepartureDate()
                            + " 출발)";
            case FareVerdict.Fair f ->
                    "[FareWatch] 적정가 알림 — "
                            + KRW_FORMAT.format(f.currentPrice())
                            + "원 ("
                            + snapshot.getDepartureDate()
                            + " 출발)";
            case FareVerdict.Expensive e ->
                    "[FareWatch] 가격 상승 — "
                            + KRW_FORMAT.format(e.currentPrice())
                            + "원 ("
                            + snapshot.getDepartureDate()
                            + " 출발)";
            case FareVerdict.Insufficient i ->
                    "[FareWatch] 데이터 부족 ("
                            + snapshot.getDepartureDate()
                            + " 출발)";
        };
    }

    private static String buildBody(FareVerdict verdict, FareSnapshot snapshot) {
        return switch (verdict) {
            case FareVerdict.Cheap c ->
                    "지금 구매를 추천합니다!\n\n"
                            + "출발일: "
                            + snapshot.getDepartureDate()
                            + "\n"
                            + "현재 가격: "
                            + KRW_FORMAT.format(c.currentPrice())
                            + "원\n"
                            + "평균 가격: "
                            + KRW_FORMAT.format(c.avgPrice())
                            + "원\n"
                            + "z-score: "
                            + String.format("%.2f", c.zScore())
                            + "\n\n"
                            + "평균보다 확실히 저렴한 가격이에요.";
            case FareVerdict.Fair f ->
                    "적정가입니다.\n\n"
                            + "출발일: "
                            + snapshot.getDepartureDate()
                            + "\n"
                            + "현재 가격: "
                            + KRW_FORMAT.format(f.currentPrice())
                            + "원\n"
                            + "평균 가격: "
                            + KRW_FORMAT.format(f.avgPrice())
                            + "원";
            case FareVerdict.Expensive e ->
                    "현재 가격이 평균보다 비쌉니다. 좀 더 기다려보세요.\n\n"
                            + "출발일: "
                            + snapshot.getDepartureDate()
                            + "\n"
                            + "현재 가격: "
                            + KRW_FORMAT.format(e.currentPrice())
                            + "원\n"
                            + "평균 가격: "
                            + KRW_FORMAT.format(e.avgPrice())
                            + "원";
            case FareVerdict.Insufficient i ->
                    "아직 데이터가 부족합니다.\n표본: "
                            + i.sampleCount()
                            + " / "
                            + i.requiredCount()
                            + "개";
        };
    }
}
