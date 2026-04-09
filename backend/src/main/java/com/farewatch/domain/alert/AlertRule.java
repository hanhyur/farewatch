package com.farewatch.domain.alert;

import com.farewatch.domain.shared.DateRange;
import com.farewatch.domain.shared.EmailAddress;
import com.farewatch.domain.shared.Money;
import com.farewatch.domain.shared.VerdictTrigger;
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
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 사용자가 등록한 알림 규칙. "이 노선의 이 출발일 범위에 대해 이런 판단이 나오면
 * 알려달라" 는 형태.
 *
 * <p>Aggregate root. {@code routeId} 는 단순 Long 참조.
 *
 * <p>삭제는 {@link #deactivate()} 로 구현하는 soft delete. 이유: {@code Notification}
 * 이 이 규칙을 FK 참조하므로, 하드 삭제 시 이력 손실 또는 cascade 필요. Soft delete 가
 * 더 안전하고 감사 가능.
 *
 * <p>nullable embedded {@code targetPrice}: Hibernate는 embeddable의 모든 필드가 null
 * 일 때만 전체를 null로 저장한다. 즉 amount + currency 모두 null이어야 함.
 */
@Entity
@Table(name = "alert_rule")
public class AlertRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "route_id", nullable = false)
    private Long routeId;

    @Embedded
    @AttributeOverride(
            name = "value",
            column = @Column(name = "user_identifier", nullable = false))
    private EmailAddress userIdentifier;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(
                name = "from",
                column = @Column(name = "departure_date_from", nullable = false)),
        @AttributeOverride(
                name = "to",
                column = @Column(name = "departure_date_to", nullable = false))
    })
    private DateRange departureRange;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "amount", column = @Column(name = "target_price")),
        @AttributeOverride(
                name = "currency",
                column = @Column(name = "target_price_currency", length = 3))
    })
    private Money targetPrice; // nullable

    @Enumerated(EnumType.STRING)
    @Column(name = "verdict_trigger", length = 20, nullable = false)
    private VerdictTrigger verdictTrigger;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected AlertRule() {}

    private AlertRule(
            Long routeId,
            EmailAddress userIdentifier,
            DateRange departureRange,
            Money targetPrice,
            VerdictTrigger verdictTrigger,
            boolean active,
            LocalDateTime createdAt) {
        this.routeId = routeId;
        this.userIdentifier = userIdentifier;
        this.departureRange = departureRange;
        this.targetPrice = targetPrice;
        this.verdictTrigger = verdictTrigger;
        this.active = active;
        this.createdAt = createdAt;
    }

    public static AlertRule create(
            Long routeId,
            EmailAddress userIdentifier,
            DateRange departureRange,
            Money targetPrice, // nullable
            VerdictTrigger verdictTrigger) {
        Objects.requireNonNull(routeId, "routeId must not be null");
        Objects.requireNonNull(userIdentifier, "userIdentifier must not be null");
        Objects.requireNonNull(departureRange, "departureRange must not be null");
        Objects.requireNonNull(verdictTrigger, "verdictTrigger must not be null");
        return new AlertRule(
                routeId, userIdentifier, departureRange, targetPrice, verdictTrigger, true, LocalDateTime.now());
    }

    public void activate() {
        this.active = true;
    }

    public void deactivate() {
        this.active = false;
    }

    public Long getId() {
        return id;
    }

    public Long getRouteId() {
        return routeId;
    }

    public EmailAddress getUserIdentifier() {
        return userIdentifier;
    }

    public DateRange getDepartureRange() {
        return departureRange;
    }

    public Money getTargetPrice() {
        return targetPrice;
    }

    public VerdictTrigger getVerdictTrigger() {
        return verdictTrigger;
    }

    public boolean isActive() {
        return active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
