package com.farewatch.application.collector;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.farewatch.application.event.FareCollectedEvent;
import com.farewatch.domain.fare.FareSnapshot;
import com.farewatch.domain.fare.FareSnapshotRepository;
import com.farewatch.domain.route.Route;
import com.farewatch.domain.shared.AirportCode;
import com.farewatch.domain.shared.Money;
import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class FareCollectionServiceTest {

    @Mock private FareCollector collector;
    @Mock private FareSnapshotRepository snapshotRepository;
    @Mock private ApplicationEventPublisher eventPublisher;

    private FareCollectionService service;
    private Route route;

    @BeforeEach
    void setUp() {
        service = new FareCollectionService(collector, snapshotRepository, eventPublisher);
        route = Route.create(new AirportCode("PUS"), new AirportCode("NRT"), "KE");
        setRouteId(route, 1L);
    }

    @Test
    @DisplayName("collectFor: fetches, saves each snapshot, and publishes an event per saved snapshot")
    void collectFor_savesAndPublishesPerSnapshot() {
        LocalDate d1 = LocalDate.of(2026, 5, 10);
        LocalDate d2 = LocalDate.of(2026, 5, 11);
        FareSnapshot s1 = buildSnapshot(1L, d1);
        FareSnapshot s2 = buildSnapshot(1L, d2);

        when(collector.fetchFares(route, d1)).thenReturn(List.of(s1));
        when(collector.fetchFares(route, d2)).thenReturn(List.of(s2));
        AtomicLong idSeq = new AtomicLong(100L);
        when(snapshotRepository.save(any(FareSnapshot.class)))
                .thenAnswer(
                        inv -> {
                            FareSnapshot arg = inv.getArgument(0);
                            setSnapshotId(arg, idSeq.getAndIncrement());
                            return arg;
                        });

        int saved = service.collectFor(route, List.of(d1, d2));

        assertThat(saved).isEqualTo(2);
        verify(snapshotRepository).save(s1);
        verify(snapshotRepository).save(s2);

        ArgumentCaptor<FareCollectedEvent> events =
                ArgumentCaptor.forClass(FareCollectedEvent.class);
        verify(eventPublisher, org.mockito.Mockito.times(2)).publishEvent(events.capture());
        assertThat(events.getAllValues())
                .extracting(e -> e.snapshot().getId())
                .containsExactly(100L, 101L);
    }

    @Test
    @DisplayName("collectFor: empty date list is a no-op")
    void collectFor_emptyDates_noOp() {
        int saved = service.collectFor(route, List.of());

        assertThat(saved).isZero();
        verifyNoInteractions(collector, snapshotRepository, eventPublisher);
    }

    @Test
    @DisplayName("collectFor: when collector returns empty, skip save and publish for that date")
    void collectFor_collectorReturnsEmpty_skipsSave() {
        LocalDate d = LocalDate.of(2026, 5, 10);
        when(collector.fetchFares(route, d)).thenReturn(List.of());

        int saved = service.collectFor(route, List.of(d));

        assertThat(saved).isZero();
        verify(snapshotRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any(Object.class));
    }

    @Test
    @DisplayName("collectFor: publishes event only after successful save (event requires id)")
    void collectFor_publishesAfterSave() {
        LocalDate d = LocalDate.of(2026, 5, 10);
        FareSnapshot s = buildSnapshot(1L, d);
        when(collector.fetchFares(route, d)).thenReturn(List.of(s));
        when(snapshotRepository.save(s))
                .thenAnswer(
                        inv -> {
                            setSnapshotId(s, 42L);
                            return s;
                        });

        service.collectFor(route, List.of(d));

        ArgumentCaptor<FareCollectedEvent> event =
                ArgumentCaptor.forClass(FareCollectedEvent.class);
        verify(eventPublisher).publishEvent(event.capture());
        assertThat(event.getValue().snapshot().getId()).isEqualTo(42L);
    }

    @Test
    @DisplayName("collectFor: rejects null route")
    void collectFor_nullRoute() {
        assertThatThrownBy(() -> service.collectFor(null, List.of(LocalDate.now())))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("collectFor: rejects null date list")
    void collectFor_nullDates() {
        assertThatThrownBy(() -> service.collectFor(route, null))
                .isInstanceOf(NullPointerException.class);
    }

    private static FareSnapshot buildSnapshot(long routeId, LocalDate date) {
        return FareSnapshot.record(routeId, date, Money.krw(180_000L), "MOCK", null);
    }

    private static void setRouteId(Route route, Long id) {
        try {
            Field f = Route.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(route, id);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void setSnapshotId(FareSnapshot snapshot, Long id) {
        try {
            Field f = FareSnapshot.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(snapshot, id);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
