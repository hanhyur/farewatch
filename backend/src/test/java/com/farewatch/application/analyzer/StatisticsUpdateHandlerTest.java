package com.farewatch.application.analyzer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.farewatch.application.event.FareCollectedEvent;
import com.farewatch.domain.fare.FareSnapshot;
import com.farewatch.domain.fare.FareSnapshotRepository;
import com.farewatch.domain.fare.FareStatistics;
import com.farewatch.domain.fare.FareStatisticsRepository;
import com.farewatch.domain.shared.Money;
import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StatisticsUpdateHandlerTest {

    @Mock private FareSnapshotRepository snapshotRepository;
    @Mock private FareStatisticsRepository statisticsRepository;

    private StatisticsUpdateHandler handler;

    @BeforeEach
    void setUp() {
        handler =
                new StatisticsUpdateHandler(
                        snapshotRepository, statisticsRepository, new StatisticsCalculator());
    }

    @Test
    @DisplayName("on event: when no existing stats, computes fresh and saves")
    void onEvent_insertsNewStats() {
        FareSnapshot snapshot = persistedSnapshot(1L, LocalDate.of(2026, 5, 10));
        when(snapshotRepository.findPricesByRouteIdAndDepartureDate(1L, LocalDate.of(2026, 5, 10)))
                .thenReturn(List.of(100L, 200L, 300L, 400L, 500L));
        when(statisticsRepository.findByRouteIdAndDepartureDate(1L, LocalDate.of(2026, 5, 10)))
                .thenReturn(Optional.empty());

        handler.on(new FareCollectedEvent(snapshot));

        ArgumentCaptor<FareStatistics> captor = ArgumentCaptor.forClass(FareStatistics.class);
        verify(statisticsRepository).save(captor.capture());
        FareStatistics saved = captor.getValue();
        assertThat(saved.getRouteId()).isEqualTo(1L);
        assertThat(saved.getDepartureDate()).isEqualTo(LocalDate.of(2026, 5, 10));
        assertThat(saved.getAvgPrice()).isEqualTo(300L);
        assertThat(saved.getMinPrice()).isEqualTo(100L);
        assertThat(saved.getMaxPrice()).isEqualTo(500L);
        assertThat(saved.getSampleCount()).isEqualTo(5);
    }

    @Test
    @DisplayName("on event: when existing stats present, recompute on the same row (no save call)")
    void onEvent_updatesExisting() {
        FareSnapshot snapshot = persistedSnapshot(1L, LocalDate.of(2026, 5, 10));
        FareStatistics existing =
                FareStatistics.compute(
                        1L, LocalDate.of(2026, 5, 10), 250L, 100L, 400L, 80.0, 4, 150L, 350L);
        when(snapshotRepository.findPricesByRouteIdAndDepartureDate(1L, LocalDate.of(2026, 5, 10)))
                .thenReturn(List.of(100L, 200L, 300L, 400L, 500L));
        when(statisticsRepository.findByRouteIdAndDepartureDate(1L, LocalDate.of(2026, 5, 10)))
                .thenReturn(Optional.of(existing));

        handler.on(new FareCollectedEvent(snapshot));

        // recompute mutates the managed entity; JPA dirty checking flushes it.
        // No explicit save call is required for an attached entity.
        verify(statisticsRepository, never()).save(any());
        assertThat(existing.getAvgPrice()).isEqualTo(300L);
        assertThat(existing.getSampleCount()).isEqualTo(5);
    }

    @Test
    @DisplayName("on event: empty price list (race condition) is a no-op")
    void onEvent_emptyPrices_noOp() {
        FareSnapshot snapshot = persistedSnapshot(1L, LocalDate.of(2026, 5, 10));
        when(snapshotRepository.findPricesByRouteIdAndDepartureDate(1L, LocalDate.of(2026, 5, 10)))
                .thenReturn(List.of());

        handler.on(new FareCollectedEvent(snapshot));

        verify(statisticsRepository, never()).save(any());
    }

    private static FareSnapshot persistedSnapshot(long routeId, LocalDate date) {
        FareSnapshot s = FareSnapshot.record(routeId, date, Money.krw(180_000L), "MOCK", null);
        try {
            Field f = FareSnapshot.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(s, 99L);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
        return s;
    }
}
