package com.farewatch.domain.route;

import com.farewatch.domain.shared.AirportCode;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RouteRepository extends JpaRepository<Route, Long> {

    List<Route> findAllByActiveTrue();

    Optional<Route> findByOriginAndDestination(AirportCode origin, AirportCode destination);
}
