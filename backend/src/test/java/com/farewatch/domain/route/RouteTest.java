package com.farewatch.domain.route;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.farewatch.domain.shared.AirportCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Route 엔티티 (단위)")
class RouteTest {

    private static final AirportCode PUS = new AirportCode("PUS");
    private static final AirportCode NRT = new AirportCode("NRT");

    @Test
    @DisplayName("유효한 origin/destination으로 생성")
    void createWithValidAirports() {
        Route route = Route.create(PUS, NRT, "KE");

        assertThat(route.getOrigin()).isEqualTo(PUS);
        assertThat(route.getDestination()).isEqualTo(NRT);
        assertThat(route.getAirlineCode()).isEqualTo("KE");
        assertThat(route.isActive()).isTrue();
        assertThat(route.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("airlineCode 는 null 허용")
    void airlineCodeNullable() {
        Route route = Route.create(PUS, NRT, null);

        assertThat(route.getAirlineCode()).isNull();
    }

    @Test
    @DisplayName("origin == destination 거부")
    void rejectsSameOriginAndDestination() {
        assertThatThrownBy(() -> Route.create(PUS, PUS, "KE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("origin");
    }

    @Test
    @DisplayName("deactivate() → isActive == false")
    void deactivate() {
        Route route = Route.create(PUS, NRT, "KE");

        route.deactivate();

        assertThat(route.isActive()).isFalse();
    }

    @Test
    @DisplayName("activate() → isActive == true")
    void activate() {
        Route route = Route.create(PUS, NRT, "KE");
        route.deactivate();

        route.activate();

        assertThat(route.isActive()).isTrue();
    }

    @Test
    @DisplayName("createdAt 은 생성 후 변경되지 않는다")
    void createdAtIsImmutable() {
        Route route = Route.create(PUS, NRT, "KE");
        java.time.LocalDateTime before = route.getCreatedAt();

        route.deactivate();
        route.activate();

        assertThat(route.getCreatedAt()).isEqualTo(before);
    }
}
