package com.farewatch.infrastructure.devdata;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.farewatch.application.analyzer.StatisticsCalculator;
import com.farewatch.domain.fare.FareSnapshot;
import com.farewatch.domain.fare.FareSnapshotRepository;
import com.farewatch.domain.fare.FareStatistics;
import com.farewatch.domain.fare.FareStatisticsRepository;
import com.farewatch.domain.route.Route;
import com.farewatch.domain.route.RouteRepository;
import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DevDataInitializerTest {

    @Mock private RouteRepository routeRepository;
    @Mock private FareSnapshotRepository snapshotRepository;
    @Mock private FareStatisticsRepository statisticsRepository;

    private DevDataInitializer initializer;

    @BeforeEach
    void setUp() {
        initializer =
                new DevDataInitializer(
                        routeRepository,
                        snapshotRepository,
                        statisticsRepository,
                        new StatisticsCalculator());
    }

    @Test
    @DisplayName("seed: creates 2 routes, 40 × 5 × 2 = 400 snapshots, 10 stats rows")
    void seed_populatesExpectedShape() {
        when(routeRepository.findAll()).thenReturn(List.of());
        AtomicLong nextRouteId = new AtomicLong(1);
        when(routeRepository.save(any(Route.class)))
                .thenAnswer(
                        inv -> {
                            Route arg = inv.getArgument(0);
                            setField(Route.class, arg, "id", nextRouteId.getAndIncrement());
                            return arg;
                        });
        when(snapshotRepository.save(any(FareSnapshot.class))).thenAnswer(inv -> inv.getArgument(0));

        initializer.seed();

        verify(routeRepository, times(2)).save(any(Route.class));
        verify(snapshotRepository, times(40 * 5 * 2)).save(any(FareSnapshot.class));
        verify(statisticsRepository, times(5 * 2)).save(any(FareStatistics.class));
    }

    @Test
    @DisplayName("seed: stats sample count >= 30 (above Insufficient threshold)")
    void seed_statsAboveSampleThreshold() {
        when(routeRepository.findAll()).thenReturn(List.of());
        AtomicLong nextRouteId = new AtomicLong(1);
        when(routeRepository.save(any(Route.class)))
                .thenAnswer(
                        inv -> {
                            Route arg = inv.getArgument(0);
                            setField(Route.class, arg, "id", nextRouteId.getAndIncrement());
                            return arg;
                        });
        when(snapshotRepository.save(any(FareSnapshot.class))).thenAnswer(inv -> inv.getArgument(0));

        initializer.seed();

        ArgumentCaptor<FareStatistics> captor = ArgumentCaptor.forClass(FareStatistics.class);
        verify(statisticsRepository, times(10)).save(captor.capture());
        captor.getAllValues()
                .forEach(s -> assertThat(s.getSampleCount()).isGreaterThanOrEqualTo(30));
    }

    @Test
    @DisplayName("seed: snapshots use historical collectedAt (not all 'now')")
    void seed_snapshotsUseHistoricalCollectedAt() {
        when(routeRepository.findAll()).thenReturn(List.of());
        AtomicLong nextRouteId = new AtomicLong(1);
        when(routeRepository.save(any(Route.class)))
                .thenAnswer(
                        inv -> {
                            Route arg = inv.getArgument(0);
                            setField(Route.class, arg, "id", nextRouteId.getAndIncrement());
                            return arg;
                        });
        when(snapshotRepository.save(any(FareSnapshot.class))).thenAnswer(inv -> inv.getArgument(0));

        initializer.seed();

        ArgumentCaptor<FareSnapshot> captor = ArgumentCaptor.forClass(FareSnapshot.class);
        verify(snapshotRepository, times(400)).save(captor.capture());

        LocalDateTime cutoff = LocalDateTime.now().minusDays(1);
        long pastCount =
                captor.getAllValues().stream()
                        .filter(s -> s.getCollectedAt().isBefore(cutoff))
                        .count();
        // 시드 분포상 대다수가 어제 이전이어야 함.
        assertThat(pastCount).isGreaterThan(300L);
    }

    @Test
    @DisplayName("seed: idempotent — does nothing when routes already exist")
    void seed_idempotentWhenRoutesExist() {
        when(routeRepository.findAll())
                .thenReturn(List.of(routeWithId(1L)));

        initializer.seed();

        verify(routeRepository, never()).save(any(Route.class));
        verify(snapshotRepository, never()).save(any(FareSnapshot.class));
        verify(statisticsRepository, never()).save(any(FareStatistics.class));
    }

    private static Route routeWithId(long id) {
        Route r =
                Route.create(
                        new com.farewatch.domain.shared.AirportCode("PUS"),
                        new com.farewatch.domain.shared.AirportCode("NRT"),
                        "KE");
        setField(Route.class, r, "id", id);
        return r;
    }

    private static <T> void setField(Class<T> clazz, T instance, String name, Object value) {
        try {
            Field f = clazz.getDeclaredField(name);
            f.setAccessible(true);
            f.set(instance, value);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
