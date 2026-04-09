package com.farewatch.domain.alert;

import com.farewatch.domain.shared.FareVerdictKind;
import com.farewatch.domain.shared.Money;
import com.farewatch.domain.shared.NotificationChannel;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 발송된 알림 이력. Append-only.
 *
 * <p>Aggregate root. 생성 후 불변. 중복 방지(동일 {@code (alertRuleId, departureDate)}
 * 조합에 대해 7일 이내 재발송 금지)는 애플리케이션 레벨에서
 * {@link NotificationRepository#existsByAlertRuleIdAndDepartureDateAndSentAtAfter}
 * 쿼리로 처리한다 — DB 제약으로 표현하지 않는 이유는 시간 윈도우 조건을 unique
 * constraint로 표현하기 어렵기 때문.
 */
@Entity
@Table(name = "notification")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "alert_rule_id", nullable = false)
    private Long alertRuleId;

    @Column(name = "route_id", nullable = false)
    private Long routeId;

    @Column(name = "departure_date", nullable = false)
    private LocalDate departureDate;

    @Column(name = "sent_at", nullable = false)
    private LocalDateTime sentAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "verdict", length = 20, nullable = false)
    private FareVerdictKind verdict;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "amount", column = @Column(name = "price_at_send", nullable = false)),
        @AttributeOverride(
                name = "currency",
                column = @Column(name = "price_at_send_currency", length = 3, nullable = false))
    })
    private Money priceAtSend;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", length = 20, nullable = false)
    private NotificationChannel channel;

    @Column(name = "message", columnDefinition = "TEXT")
    private String message;

    protected Notification() {}

    private Notification(
            Long alertRuleId,
            Long routeId,
            LocalDate departureDate,
            LocalDateTime sentAt,
            FareVerdictKind verdict,
            Money priceAtSend,
            NotificationChannel channel,
            String message) {
        this.alertRuleId = alertRuleId;
        this.routeId = routeId;
        this.departureDate = departureDate;
        this.sentAt = sentAt;
        this.verdict = verdict;
        this.priceAtSend = priceAtSend;
        this.channel = channel;
        this.message = message;
    }

    public static Notification send(
            Long alertRuleId,
            Long routeId,
            LocalDate departureDate,
            FareVerdictKind verdict,
            Money priceAtSend,
            NotificationChannel channel,
            String message) {
        Objects.requireNonNull(alertRuleId, "alertRuleId must not be null");
        Objects.requireNonNull(routeId, "routeId must not be null");
        Objects.requireNonNull(departureDate, "departureDate must not be null");
        Objects.requireNonNull(verdict, "verdict must not be null");
        Objects.requireNonNull(priceAtSend, "priceAtSend must not be null");
        Objects.requireNonNull(channel, "channel must not be null");
        // priceAtSend.amount() > 0 은 Money 생성자가 이미 강제.
        return new Notification(
                alertRuleId, routeId, departureDate, LocalDateTime.now(),
                verdict, priceAtSend, channel, message);
    }

    public Long getId() {
        return id;
    }

    public Long getAlertRuleId() {
        return alertRuleId;
    }

    public Long getRouteId() {
        return routeId;
    }

    public LocalDate getDepartureDate() {
        return departureDate;
    }

    public LocalDateTime getSentAt() {
        return sentAt;
    }

    public FareVerdictKind getVerdict() {
        return verdict;
    }

    public Money getPriceAtSend() {
        return priceAtSend;
    }

    public NotificationChannel getChannel() {
        return channel;
    }

    public String getMessage() {
        return message;
    }
}
