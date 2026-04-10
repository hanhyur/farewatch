package com.farewatch.infrastructure.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.farewatch.application.collector.FareCollectionService;
import com.farewatch.domain.route.Route;
import com.farewatch.domain.route.RouteRepository;
import com.farewatch.domain.shared.AirportCode;
import java.lang.reflect.Field;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FareCollectionSchedulerTest {

    @Mock private RouteRepository routeRepository;
    @Mock private FareCollectionService collectionService;

    private FareCollectionScheduler scheduler;

    // Fixed "today" = 2026-05-01
    private final Clock fixedClock =
            Clock.fixed(
                    Instant.parse("2026-05-01T09:00:00Z"), ZoneId.of("UTC"));

    @BeforeEach
    void setUp() {
        scheduler =
                new FareCollectionScheduler(
                        routeRepository, collectionService, List.of(7, 14, 30), fixedClock);
    }

    @Test
    @DisplayName("sweep: iterates active routes and calls collection service with offset dates")
    void sweep_collectsForAllActiveRoutes() {
        Route r1 = routeWithId(1L, "PUS", "NRT");
        Route r2 = routeWithId(2L, "PUS", "HND");
        when(routeRepository.findAllByActiveTrue()).thenReturn(List.of(r1, r2));

        scheduler.sweep();

        LocalDate today = LocalDate.of(2026, 5, 1);
        List<LocalDate> expectedDates =
                List.of(today.plusDays(7), today.plusDays(14), today.plusDays(30));

        ArgumentCaptor<Route> routes = ArgumentCaptor.forClass(Route.class);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<LocalDate>> dates = ArgumentCaptor.forClass(List.class);
        verify(collectionService, org.mockito.Mockito.times(2))
                .collectFor(routes.capture(), dates.capture());

        assertThat(routes.getAllValues()).containsExactly(r1, r2);
        assertThat(dates.getAllValues().get(0)).isEqualTo(expectedDates);
        assertThat(dates.getAllValues().get(1)).isEqualTo(expectedDates);
    }

    @Test
    @DisplayName("sweep: one failing route must not halt the sweep for the rest")
    void sweep_isolatesPerRouteFailure() {
        Route r1 = routeWithId(1L, "PUS", "NRT");
        Route r2 = routeWithId(2L, "PUS", "HND");
        when(routeRepository.findAllByActiveTrue()).thenReturn(List.of(r1, r2));
        doThrow(new RuntimeException("boom"))
                .when(collectionService)
                .collectFor(eq(r1), any());

        scheduler.sweep();

        verify(collectionService).collectFor(eq(r1), any());
        verify(collectionService).collectFor(eq(r2), any());
    }

    @Test
    @DisplayName("sweep: no active routes is a no-op")
    void sweep_noActiveRoutes() {
        when(routeRepository.findAllByActiveTrue()).thenReturn(List.of());

        scheduler.sweep();

        verify(collectionService, org.mockito.Mockito.never()).collectFor(any(), any());
    }

    private static Route routeWithId(long id, String origin, String destination) {
        Route route = Route.create(new AirportCode(origin), new AirportCode(destination), "KE");
        try {
            Field f = Route.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(route, id);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
        return route;
    }
}
