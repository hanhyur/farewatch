package com.farewatch.api.fare;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.farewatch.api.common.GlobalExceptionHandler;
import com.farewatch.domain.fare.FareSnapshot;
import com.farewatch.domain.fare.FareSnapshotRepository;
import com.farewatch.domain.fare.FareStatistics;
import com.farewatch.domain.fare.FareStatisticsRepository;
import com.farewatch.domain.route.RouteRepository;
import com.farewatch.domain.shared.Money;
import java.lang.reflect.Field;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Pageable;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = FareController.class)
@Import({GlobalExceptionHandler.class, FareControllerTest.TestClockConfig.class})
class FareControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private FareSnapshotRepository snapshotRepository;
    @MockBean private FareStatisticsRepository statisticsRepository;
    @MockBean private RouteRepository routeRepository;

    static class TestClockConfig {
        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(Instant.parse("2026-04-15T00:00:00Z"), ZoneId.of("UTC"));
        }
    }

    @Test
    void fares_returnsRecentSnapshotsForExplicitDate() throws Exception {
        when(routeRepository.existsById(1L)).thenReturn(true);
        when(snapshotRepository.findByRouteIdAndDepartureDateOrderByCollectedAtDesc(
                        eq(1L), eq(LocalDate.of(2026, 5, 10)), any(Pageable.class)))
                .thenReturn(List.of(persistedSnapshot(1L, LocalDate.of(2026, 5, 10), 180_000L)));

        mockMvc.perform(get("/api/v1/routes/1/fares").param("departureDate", "2026-05-10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].price").value(180000))
                .andExpect(jsonPath("$.data[0].currency").value("KRW"));
    }

    @Test
    void fares_returnsEmptyListWhenNoSnapshots() throws Exception {
        when(routeRepository.existsById(1L)).thenReturn(true);
        when(snapshotRepository.findByRouteIdAndDepartureDateOrderByCollectedAtDesc(
                        any(), any(), any()))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/v1/routes/1/fares"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    void fares_unknownRoute_returns404() throws Exception {
        when(routeRepository.existsById(99L)).thenReturn(false);

        mockMvc.perform(get("/api/v1/routes/99/fares"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void statistics_returnsBody() throws Exception {
        when(routeRepository.existsById(1L)).thenReturn(true);
        when(statisticsRepository.findByRouteIdAndDepartureDate(1L, LocalDate.of(2026, 5, 10)))
                .thenReturn(
                        Optional.of(
                                FareStatistics.compute(
                                        1L,
                                        LocalDate.of(2026, 5, 10),
                                        200_000L,
                                        150_000L,
                                        260_000L,
                                        25_000.0,
                                        50,
                                        180_000L,
                                        220_000L)));

        mockMvc.perform(
                        get("/api/v1/routes/1/statistics").param("departureDate", "2026-05-10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.avgPrice").value(200000))
                .andExpect(jsonPath("$.data.sampleCount").value(50));
    }

    @Test
    void statistics_missing_returns404() throws Exception {
        when(routeRepository.existsById(1L)).thenReturn(true);
        when(statisticsRepository.findByRouteIdAndDepartureDate(any(), any()))
                .thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/routes/1/statistics"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    private static FareSnapshot persistedSnapshot(long routeId, LocalDate date, long price) {
        FareSnapshot s = FareSnapshot.record(routeId, date, Money.krw(price), "MOCK", null);
        try {
            Field f = FareSnapshot.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(s, 1L);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
        return s;
    }
}
