package com.farewatch.domain.fare;

import com.farewatch.domain.shared.Money;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * 특정 시점에 수집된 특정 출발일 항공권 가격의 스냅샷. Append-only.
 *
 * <p>수집기가 고빈도로 저장한다. 생성 후 어떤 필드도 변경되지 않는다 — 원본 데이터의
 * 감사(audit) 를 위해서는 변경 불가성이 필수. 삭제는 별도 아카이빙 정책으로 분리.
 *
 * <p>Aggregate root. {@code routeId} 는 단순 {@code Long} 참조 (cross-aggregate 객체
 * 참조 금지).
 *
 * <p>{@code rawData} 는 JSONB 컬럼으로 저장된다. Hibernate 6.5의
 * {@link SqlTypes#JSON} JdbcType을 사용해 PostgreSQL(JSONB)과 H2(JSON/VARCHAR) 양쪽에
 * 호환되도록 매핑.
 */
@Entity
@Table(name = "fare_snapshot")
public class FareSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "route_id", nullable = false)
    private Long routeId;

    @Column(name = "departure_date", nullable = false)
    private LocalDate departureDate;

    @Column(name = "collected_at", nullable = false)
    private LocalDateTime collectedAt;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "amount", column = @Column(name = "price", nullable = false)),
        @AttributeOverride(
                name = "currency",
                column = @Column(name = "currency", length = 3, nullable = false))
    })
    private Money price;

    @Column(name = "source", length = 50, nullable = false)
    private String source;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_data")
    private Map<String, Object> rawData;

    protected FareSnapshot() {}

    private FareSnapshot(
            Long routeId,
            LocalDate departureDate,
            LocalDateTime collectedAt,
            Money price,
            String source,
            Map<String, Object> rawData) {
        this.routeId = routeId;
        this.departureDate = departureDate;
        this.collectedAt = collectedAt;
        this.price = price;
        this.source = source;
        this.rawData = rawData;
    }

    /**
     * 새 가격 스냅샷 기록. 가격은 {@code > 0} 이어야 한다.
     *
     * @param rawData 수집기의 원본 응답 (nullable — 원본 미보관 케이스)
     */
    public static FareSnapshot record(
            Long routeId,
            LocalDate departureDate,
            Money price,
            String source,
            Map<String, Object> rawData) {
        Objects.requireNonNull(routeId, "routeId must not be null");
        Objects.requireNonNull(departureDate, "departureDate must not be null");
        Objects.requireNonNull(price, "price must not be null");
        Objects.requireNonNull(source, "source must not be null");
        if (price.amount() <= 0) {
            throw new IllegalArgumentException("price must be > 0, but was " + price.amount());
        }
        return new FareSnapshot(routeId, departureDate, LocalDateTime.now(), price, source, rawData);
    }

    public Long getId() {
        return id;
    }

    public Long getRouteId() {
        return routeId;
    }

    public LocalDate getDepartureDate() {
        return departureDate;
    }

    public LocalDateTime getCollectedAt() {
        return collectedAt;
    }

    public Money getPrice() {
        return price;
    }

    public String getSource() {
        return source;
    }

    public Map<String, Object> getRawData() {
        return rawData;
    }
}
