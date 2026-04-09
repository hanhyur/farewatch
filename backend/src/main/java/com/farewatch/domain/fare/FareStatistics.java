package com.farewatch.domain.fare;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 특정 노선 + 출발일 조합에 대해 누적 스냅샷으로부터 계산된 가격 통계.
 *
 * <p>Aggregate root. {@code (routeId, departureDate)} 쌍당 한 행만 존재하도록 unique
 * 제약을 둔다. 새 스냅샷이 수집되면 기존 행을 {@link #recompute} 로 갱신 (upsert).
 *
 * <p>통계 값(avgPrice 등)은 {@code Money} 대신 plain {@code long} 을 사용한다. 이유:
 * 5개 가격 컬럼마다 {@code @AttributeOverrides} 를 반복하는 보일러플레이트를 피하고,
 * 통계는 "전송 가능한 금액" 이 아니라 "계산 결과" 이기 때문.
 *
 * <p>Invariants: {@code minPrice <= avgPrice <= maxPrice}, {@code stdDeviation >= 0},
 * {@code sampleCount >= 0}.
 */
@Entity
@Table(
        name = "fare_statistics",
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uk_fare_statistics_route_departure",
                    columnNames = {"route_id", "departure_date"})
        })
public class FareStatistics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "route_id", nullable = false)
    private Long routeId;

    @Column(name = "departure_date", nullable = false)
    private LocalDate departureDate;

    @Column(name = "calculated_at", nullable = false)
    private LocalDateTime calculatedAt;

    @Column(name = "avg_price", nullable = false)
    private long avgPrice;

    @Column(name = "min_price", nullable = false)
    private long minPrice;

    @Column(name = "max_price", nullable = false)
    private long maxPrice;

    @Column(name = "std_deviation", nullable = false)
    private double stdDeviation;

    @Column(name = "sample_count", nullable = false)
    private int sampleCount;

    @Column(name = "p25_price")
    private Long p25Price;

    @Column(name = "p75_price")
    private Long p75Price;

    protected FareStatistics() {}

    private FareStatistics(
            Long routeId,
            LocalDate departureDate,
            LocalDateTime calculatedAt,
            long avgPrice,
            long minPrice,
            long maxPrice,
            double stdDeviation,
            int sampleCount,
            Long p25Price,
            Long p75Price) {
        this.routeId = routeId;
        this.departureDate = departureDate;
        this.calculatedAt = calculatedAt;
        this.avgPrice = avgPrice;
        this.minPrice = minPrice;
        this.maxPrice = maxPrice;
        this.stdDeviation = stdDeviation;
        this.sampleCount = sampleCount;
        this.p25Price = p25Price;
        this.p75Price = p75Price;
    }

    public static FareStatistics compute(
            Long routeId,
            LocalDate departureDate,
            long avgPrice,
            long minPrice,
            long maxPrice,
            double stdDeviation,
            int sampleCount,
            Long p25Price,
            Long p75Price) {
        Objects.requireNonNull(routeId, "routeId must not be null");
        Objects.requireNonNull(departureDate, "departureDate must not be null");
        validateInvariants(avgPrice, minPrice, maxPrice, stdDeviation, sampleCount);
        return new FareStatistics(
                routeId,
                departureDate,
                LocalDateTime.now(),
                avgPrice,
                minPrice,
                maxPrice,
                stdDeviation,
                sampleCount,
                p25Price,
                p75Price);
    }

    /**
     * 기존 통계 행의 값을 새 계산 결과로 덮어쓴다 (upsert의 update 쪽). JPA dirty
     * checking으로 flush 시 자동 반영.
     */
    public void recompute(
            long avgPrice,
            long minPrice,
            long maxPrice,
            double stdDeviation,
            int sampleCount,
            Long p25Price,
            Long p75Price) {
        validateInvariants(avgPrice, minPrice, maxPrice, stdDeviation, sampleCount);
        this.avgPrice = avgPrice;
        this.minPrice = minPrice;
        this.maxPrice = maxPrice;
        this.stdDeviation = stdDeviation;
        this.sampleCount = sampleCount;
        this.p25Price = p25Price;
        this.p75Price = p75Price;
        this.calculatedAt = LocalDateTime.now();
    }

    private static void validateInvariants(
            long avg, long min, long max, double stdDev, int sampleCount) {
        if (min > avg) {
            throw new IllegalArgumentException("min (" + min + ") must be <= avg (" + avg + ")");
        }
        if (avg > max) {
            throw new IllegalArgumentException("avg (" + avg + ") must be <= max (" + max + ")");
        }
        if (stdDev < 0) {
            throw new IllegalArgumentException("stdDeviation must be >= 0, was " + stdDev);
        }
        if (sampleCount < 0) {
            throw new IllegalArgumentException("sampleCount must be >= 0, was " + sampleCount);
        }
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

    public LocalDateTime getCalculatedAt() {
        return calculatedAt;
    }

    public long getAvgPrice() {
        return avgPrice;
    }

    public long getMinPrice() {
        return minPrice;
    }

    public long getMaxPrice() {
        return maxPrice;
    }

    public double getStdDeviation() {
        return stdDeviation;
    }

    public int getSampleCount() {
        return sampleCount;
    }

    public Long getP25Price() {
        return p25Price;
    }

    public Long getP75Price() {
        return p75Price;
    }
}
