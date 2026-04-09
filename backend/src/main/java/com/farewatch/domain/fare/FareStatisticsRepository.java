package com.farewatch.domain.fare;

import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FareStatisticsRepository extends JpaRepository<FareStatistics, Long> {

    /** (routeId, departureDate) unique 키로 조회. upsert의 find 단계. */
    Optional<FareStatistics> findByRouteIdAndDepartureDate(Long routeId, LocalDate departureDate);
}
