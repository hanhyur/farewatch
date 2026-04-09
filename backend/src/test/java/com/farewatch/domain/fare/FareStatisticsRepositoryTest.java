package com.farewatch.domain.fare;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@DataJpaTest
@EnableJpaRepositories(basePackageClasses = FareStatisticsRepository.class)
@DisplayName("FareStatisticsRepository (@DataJpaTest)")
class FareStatisticsRepositoryTest {

    @Autowired
    private FareStatisticsRepository repository;

    private static final Long ROUTE_1 = 1L;
    private static final Long ROUTE_2 = 2L;
    private static final LocalDate MAY_15 = LocalDate.of(2026, 5, 15);
    private static final LocalDate MAY_16 = LocalDate.of(2026, 5, 16);

    @Test
    @DisplayName("save + findByRouteIdAndDepartureDate: 단일 행 저장·조회")
    void saveAndFind() {
        FareStatistics saved = repository.save(FareStatistics.compute(
                ROUTE_1, MAY_15, 232_000L, 154_000L, 310_000L, 28_500.0, 45, 200_000L, 260_000L));

        Optional<FareStatistics> found =
                repository.findByRouteIdAndDepartureDate(ROUTE_1, MAY_15);

        assertThat(found).isPresent();
        assertThat(found.get().getAvgPrice()).isEqualTo(232_000L);
        assertThat(found.get().getSampleCount()).isEqualTo(45);
        assertThat(found.get().getP25Price()).isEqualTo(200_000L);
    }

    @Test
    @DisplayName("같은 (routeId, departureDate) 에 두 행 저장 시 unique 제약 위반")
    void uniqueConstraintOnRouteAndDeparture() {
        repository.save(FareStatistics.compute(
                ROUTE_1, MAY_15, 200_000L, 150_000L, 250_000L, 20_000.0, 30, null, null));

        FareStatistics duplicate = FareStatistics.compute(
                ROUTE_1, MAY_15, 210_000L, 160_000L, 260_000L, 21_000.0, 31, null, null);

        assertThatThrownBy(() -> {
            repository.saveAndFlush(duplicate);
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("다른 routeId 또는 departureDate는 unique 제약에 걸리지 않음")
    void uniqueAllowsDifferentKey() {
        repository.save(FareStatistics.compute(
                ROUTE_1, MAY_15, 200_000L, 150_000L, 250_000L, 20_000.0, 30, null, null));
        repository.save(FareStatistics.compute(
                ROUTE_1, MAY_16, 210_000L, 160_000L, 260_000L, 21_000.0, 30, null, null));
        repository.save(FareStatistics.compute(
                ROUTE_2, MAY_15, 220_000L, 170_000L, 270_000L, 22_000.0, 30, null, null));

        assertThat(repository.findByRouteIdAndDepartureDate(ROUTE_1, MAY_15)).isPresent();
        assertThat(repository.findByRouteIdAndDepartureDate(ROUTE_1, MAY_16)).isPresent();
        assertThat(repository.findByRouteIdAndDepartureDate(ROUTE_2, MAY_15)).isPresent();
    }

    @Test
    @DisplayName("recompute() 후 저장: 같은 행 update (dirty checking)")
    void recomputeUpdatesExistingRow() {
        FareStatistics saved = repository.save(FareStatistics.compute(
                ROUTE_1, MAY_15, 200_000L, 150_000L, 250_000L, 20_000.0, 30, null, null));
        Long id = saved.getId();

        saved.recompute(210_000L, 160_000L, 260_000L, 21_000.0, 31, 190_000L, 240_000L);
        repository.saveAndFlush(saved);

        FareStatistics found = repository.findById(id).orElseThrow();
        assertThat(found.getAvgPrice()).isEqualTo(210_000L);
        assertThat(found.getSampleCount()).isEqualTo(31);
        assertThat(found.getP25Price()).isEqualTo(190_000L);
        assertThat(repository.count()).isEqualTo(1L);
    }
}
