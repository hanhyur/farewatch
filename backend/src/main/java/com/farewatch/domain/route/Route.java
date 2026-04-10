package com.farewatch.domain.route;

import com.farewatch.domain.shared.AirportCode;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 모니터링 대상 항공 노선. 출발/도착 공항 + 선택적 항공사 코드.
 *
 * <p>Aggregate root. 다른 엔티티({@code FareSnapshot}, {@code AlertRule} 등)는 이
 * Route를 {@code Long routeId}로만 참조한다 — 객체 참조 대신 ID 참조로 aggregate
 * boundary를 명확히 유지.
 *
 * <p>상태 전이는 {@link #activate()} / {@link #deactivate()} 두 메서드로만 가능. 다른
 * 필드(origin, destination, airlineCode, createdAt)는 생성 후 절대 변경되지 않는다.
 */
@Entity
@Table(name = "route")
public class Route {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Embedded
    @AttributeOverride(
            name = "value",
            column = @Column(name = "origin_iata", length = 3, nullable = false))
    private AirportCode origin;

    @Embedded
    @AttributeOverride(
            name = "value",
            column = @Column(name = "destination_iata", length = 3, nullable = false))
    private AirportCode destination;

    @Column(name = "airline_code", length = 10)
    private String airlineCode;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /** JPA 전용. 애플리케이션 코드에서 직접 호출 금지. */
    protected Route() {}

    private Route(
            AirportCode origin,
            AirportCode destination,
            String airlineCode,
            boolean active,
            LocalDateTime createdAt) {
        this.origin = origin;
        this.destination = destination;
        this.airlineCode = airlineCode;
        this.active = active;
        this.createdAt = createdAt;
    }

    /**
     * 새 노선 생성. {@code origin} 과 {@code destination} 이 같으면 거부.
     *
     * @param airlineCode 항공사 IATA 코드 (nullable — 특정 항공사에 국한하지 않는 경우 null)
     */
    public static Route create(AirportCode origin, AirportCode destination, String airlineCode) {
        Objects.requireNonNull(origin, "origin must not be null");
        Objects.requireNonNull(destination, "destination must not be null");
        if (origin.equals(destination)) {
            throw new IllegalArgumentException(
                    "origin and destination must differ, but both were " + origin.value());
        }
        return new Route(origin, destination, airlineCode, true, LocalDateTime.now());
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

    public AirportCode getOrigin() {
        return origin;
    }

    public AirportCode getDestination() {
        return destination;
    }

    public String getAirlineCode() {
        return airlineCode;
    }

    public boolean isActive() {
        return active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
