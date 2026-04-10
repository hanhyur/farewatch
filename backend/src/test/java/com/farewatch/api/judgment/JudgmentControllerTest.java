package com.farewatch.api.judgment;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.farewatch.api.common.GlobalExceptionHandler;
import com.farewatch.application.judgment.FareVerdictCalculator;
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
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = JudgmentController.class)
@Import({GlobalExceptionHandler.class, JudgmentControllerTest.TestConfig.class})
class JudgmentControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private RouteRepository routeRepository;
    @MockBean private FareSnapshotRepository snapshotRepository;
    @MockBean private FareStatisticsRepository statisticsRepository;

    static class TestConfig {
        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(Instant.parse("2026-04-15T00:00:00Z"), ZoneId.of("UTC"));
        }

        @Bean
        FareVerdictCalculator verdictCalculator() {
            return new FareVerdictCalculator();
        }
    }

    @Test
    void judgment_cheapVerdict_returnsCheapPayload() throws Exception {
        when(routeRepository.existsById(1L)).thenReturn(true);
        LocalDate target = LocalDate.of(2026, 5, 10);
        when(snapshotRepository.findTopByRouteIdAndDepartureDateOrderByCollectedAtDesc(
                        eq(1L), eq(target)))
                .thenReturn(Optional.of(persistedSnapshot(1L, target, 150_000L)));
        when(statisticsRepository.findByRouteIdAndDepartureDate(1L, target))
                .thenReturn(
                        Optional.of(
                                FareStatistics.compute(
                                        1L,
                                        target,
                                        200_000L,
                                        140_000L,
                                        260_000L,
                                        20_000.0,
                                        50,
                                        180_000L,
                                        220_000L)));

        mockMvc.perform(get("/api/v1/routes/1/judgment").param("departureDate", "2026-05-10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.verdict").value("CHEAP"))
                .andExpect(jsonPath("$.data.currentPrice").value(150000))
                .andExpect(jsonPath("$.data.avgPrice").value(200000))
                .andExpect(jsonPath("$.data.suggestion").value("지금 구매를 추천합니다"));
    }

    @Test
    void judgment_noStatsYet_returnsInsufficientWithCurrentPrice() throws Exception {
        when(routeRepository.existsById(1L)).thenReturn(true);
        LocalDate target = LocalDate.of(2026, 5, 10);
        when(snapshotRepository.findTopByRouteIdAndDepartureDateOrderByCollectedAtDesc(
                        eq(1L), eq(target)))
                .thenReturn(Optional.of(persistedSnapshot(1L, target, 178_000L)));
        when(statisticsRepository.findByRouteIdAndDepartureDate(1L, target))
                .thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/routes/1/judgment").param("departureDate", "2026-05-10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.verdict").value("INSUFFICIENT"))
                .andExpect(jsonPath("$.data.currentPrice").value(178000))
                .andExpect(jsonPath("$.data.sampleCount").value(0));
    }

    @Test
    void judgment_noSnapshot_returns404() throws Exception {
        when(routeRepository.existsById(1L)).thenReturn(true);
        when(snapshotRepository.findTopByRouteIdAndDepartureDateOrderByCollectedAtDesc(
                        any(), any()))
                .thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/routes/1/judgment"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void judgment_unknownRoute_returns404() throws Exception {
        when(routeRepository.existsById(99L)).thenReturn(false);

        mockMvc.perform(get("/api/v1/routes/99/judgment"))
                .andExpect(status().isNotFound());
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
