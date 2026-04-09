package com.farewatch.domain.route;

import static org.assertj.core.api.Assertions.assertThat;

import com.farewatch.domain.shared.AirportCode;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest
@DisplayName("RouteRepository (@DataJpaTest)")
class RouteRepositoryTest {

    @Autowired
    private RouteRepository repository;

    private static final AirportCode PUS = new AirportCode("PUS");
    private static final AirportCode NRT = new AirportCode("NRT");
    private static final AirportCode HND = new AirportCode("HND");

    @Test
    @DisplayName("save + findById: 임베디드 VO round-trip")
    void saveAndFind() {
        Route saved = repository.save(Route.create(PUS, NRT, "KE"));

        Optional<Route> found = repository.findById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getOrigin()).isEqualTo(PUS);
        assertThat(found.get().getDestination()).isEqualTo(NRT);
        assertThat(found.get().getAirlineCode()).isEqualTo("KE");
        assertThat(found.get().isActive()).isTrue();
        assertThat(found.get().getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("findAllByActiveTrue(): 비활성 노선 제외")
    void findOnlyActive() {
        Route active1 = repository.save(Route.create(PUS, NRT, "KE"));
        Route active2 = repository.save(Route.create(PUS, HND, "OZ"));
        Route inactive = Route.create(NRT, HND, "JL");
        inactive.deactivate();
        repository.save(inactive);

        List<Route> actives = repository.findAllByActiveTrue();

        assertThat(actives).extracting(Route::getId).containsExactlyInAnyOrder(
                active1.getId(), active2.getId());
    }

    @Test
    @DisplayName("findByOriginAndDestination(): VO 기반 조회")
    void findByOriginAndDestination() {
        repository.save(Route.create(PUS, NRT, "KE"));
        repository.save(Route.create(PUS, HND, "OZ"));

        Optional<Route> found = repository.findByOriginAndDestination(PUS, NRT);

        assertThat(found).isPresent();
        assertThat(found.get().getDestination()).isEqualTo(NRT);
    }

    @Test
    @DisplayName("findByOriginAndDestination(): 없는 조합은 empty")
    void findByOriginAndDestinationMissing() {
        repository.save(Route.create(PUS, NRT, "KE"));

        Optional<Route> found = repository.findByOriginAndDestination(NRT, HND);

        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("nullable airlineCode 저장·조회")
    void nullAirlineCodeRoundTrip() {
        Route saved = repository.save(Route.create(PUS, NRT, null));

        Optional<Route> found = repository.findById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getAirlineCode()).isNull();
    }
}
