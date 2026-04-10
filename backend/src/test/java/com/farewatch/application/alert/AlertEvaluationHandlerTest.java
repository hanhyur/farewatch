package com.farewatch.application.alert;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.farewatch.application.event.FareCollectedEvent;
import com.farewatch.application.judgment.FareVerdictCalculator;
import com.farewatch.domain.alert.AlertRule;
import com.farewatch.domain.alert.AlertRuleRepository;
import com.farewatch.domain.alert.Notification;
import com.farewatch.domain.alert.NotificationRepository;
import com.farewatch.domain.fare.FareSnapshot;
import com.farewatch.domain.fare.FareStatistics;
import com.farewatch.domain.fare.FareStatisticsRepository;
import com.farewatch.domain.judgment.FareVerdict;
import com.farewatch.domain.shared.DateRange;
import com.farewatch.domain.shared.EmailAddress;
import com.farewatch.domain.shared.FareVerdictKind;
import com.farewatch.domain.shared.Money;
import com.farewatch.domain.shared.NotificationChannel;
import com.farewatch.domain.shared.VerdictTrigger;
import java.lang.reflect.Field;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
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
class AlertEvaluationHandlerTest {

    @Mock private AlertRuleRepository alertRuleRepository;
    @Mock private FareStatisticsRepository statisticsRepository;
    @Mock private NotificationRepository notificationRepository;
    @Mock private NotificationSender sender;

    private AlertEvaluationHandler handler;

    private final Clock fixedClock =
            Clock.fixed(Instant.parse("2026-04-15T09:00:00Z"), ZoneId.of("UTC"));

    @BeforeEach
    void setUp() {
        lenient().when(sender.channel()).thenReturn(NotificationChannel.LOG);
        handler =
                new AlertEvaluationHandler(
                        alertRuleRepository,
                        statisticsRepository,
                        notificationRepository,
                        new FareVerdictCalculator(),
                        sender,
                        fixedClock);
    }

    @Test
    @DisplayName("matches CHEAP rule: persists Notification, calls sender")
    void cheapVerdict_triggersCheapRule() {
        long routeId = 1L;
        LocalDate departureDate = LocalDate.of(2026, 5, 10);
        FareSnapshot snapshot = persistedSnapshot(routeId, departureDate, 150_000L);
        AlertRule rule = persistedRule(11L, routeId, VerdictTrigger.CHEAP, departureDate);

        // Cheap verdict: stats with avg=200000, stdDev=20000, n=30, current=150000 → z=-2.5
        FareStatistics stats = statsWithSamples(routeId, departureDate, 200_000L, 20_000.0, 30);
        when(alertRuleRepository.findByRouteIdAndActiveTrue(routeId)).thenReturn(List.of(rule));
        when(statisticsRepository.findByRouteIdAndDepartureDate(routeId, departureDate))
                .thenReturn(Optional.of(stats));
        when(notificationRepository.existsByAlertRuleIdAndDepartureDateAndSentAtAfter(
                        eq(11L), eq(departureDate), any(LocalDateTime.class)))
                .thenReturn(false);

        handler.on(new FareCollectedEvent(snapshot));

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        Notification saved = captor.getValue();
        assertThat(saved.getAlertRuleId()).isEqualTo(11L);
        assertThat(saved.getRouteId()).isEqualTo(routeId);
        assertThat(saved.getDepartureDate()).isEqualTo(departureDate);
        assertThat(saved.getVerdict()).isEqualTo(FareVerdictKind.CHEAP);
        assertThat(saved.getPriceAtSend().amount()).isEqualTo(150_000L);
        assertThat(saved.getChannel()).isEqualTo(NotificationChannel.LOG);
        verify(sender).send(eq(rule), any(FareVerdict.Cheap.class), eq(snapshot));
    }

    @Test
    @DisplayName("CHEAP rule: FAIR verdict does not trigger")
    void fairVerdict_doesNotTriggerCheapRule() {
        long routeId = 1L;
        LocalDate departureDate = LocalDate.of(2026, 5, 10);
        FareSnapshot snapshot = persistedSnapshot(routeId, departureDate, 200_000L);
        AlertRule rule = persistedRule(11L, routeId, VerdictTrigger.CHEAP, departureDate);

        // current = avg → Fair
        FareStatistics stats = statsWithSamples(routeId, departureDate, 200_000L, 20_000.0, 30);
        when(alertRuleRepository.findByRouteIdAndActiveTrue(routeId)).thenReturn(List.of(rule));
        when(statisticsRepository.findByRouteIdAndDepartureDate(routeId, departureDate))
                .thenReturn(Optional.of(stats));

        handler.on(new FareCollectedEvent(snapshot));

        verify(notificationRepository, never()).save(any());
        verify(sender, never()).send(any(), any(), any());
    }

    @Test
    @DisplayName("CHEAP_OR_FAIR rule: FAIR verdict triggers")
    void fairVerdict_triggersCheapOrFairRule() {
        long routeId = 1L;
        LocalDate departureDate = LocalDate.of(2026, 5, 10);
        FareSnapshot snapshot = persistedSnapshot(routeId, departureDate, 200_000L);
        AlertRule rule =
                persistedRule(12L, routeId, VerdictTrigger.CHEAP_OR_FAIR, departureDate);
        FareStatistics stats = statsWithSamples(routeId, departureDate, 200_000L, 20_000.0, 30);
        when(alertRuleRepository.findByRouteIdAndActiveTrue(routeId)).thenReturn(List.of(rule));
        when(statisticsRepository.findByRouteIdAndDepartureDate(routeId, departureDate))
                .thenReturn(Optional.of(stats));
        when(notificationRepository.existsByAlertRuleIdAndDepartureDateAndSentAtAfter(
                        eq(12L), eq(departureDate), any(LocalDateTime.class)))
                .thenReturn(false);

        handler.on(new FareCollectedEvent(snapshot));

        verify(notificationRepository).save(any());
        verify(sender).send(eq(rule), any(FareVerdict.Fair.class), eq(snapshot));
    }

    @Test
    @DisplayName("rule whose date range excludes the departure date is skipped")
    void ruleOutsideRange_skipped() {
        long routeId = 1L;
        LocalDate departureDate = LocalDate.of(2026, 5, 10);
        FareSnapshot snapshot = persistedSnapshot(routeId, departureDate, 150_000L);
        // rule covers May 1-5; departureDate May 10 is outside
        AlertRule rule =
                persistedRuleWithRange(
                        13L,
                        routeId,
                        VerdictTrigger.CHEAP,
                        LocalDate.of(2026, 5, 1),
                        LocalDate.of(2026, 5, 5));
        when(alertRuleRepository.findByRouteIdAndActiveTrue(routeId)).thenReturn(List.of(rule));

        handler.on(new FareCollectedEvent(snapshot));

        verifyNoInteractions(statisticsRepository, notificationRepository);
        verify(sender, never()).send(any(), any(), any());
    }

    @Test
    @DisplayName("duplicate within 7 days is skipped (no save, no send)")
    void duplicateWithin7Days_skipped() {
        long routeId = 1L;
        LocalDate departureDate = LocalDate.of(2026, 5, 10);
        FareSnapshot snapshot = persistedSnapshot(routeId, departureDate, 150_000L);
        AlertRule rule = persistedRule(11L, routeId, VerdictTrigger.CHEAP, departureDate);
        FareStatistics stats = statsWithSamples(routeId, departureDate, 200_000L, 20_000.0, 30);
        when(alertRuleRepository.findByRouteIdAndActiveTrue(routeId)).thenReturn(List.of(rule));
        when(statisticsRepository.findByRouteIdAndDepartureDate(routeId, departureDate))
                .thenReturn(Optional.of(stats));
        when(notificationRepository.existsByAlertRuleIdAndDepartureDateAndSentAtAfter(
                        eq(11L), eq(departureDate), any(LocalDateTime.class)))
                .thenReturn(true);

        handler.on(new FareCollectedEvent(snapshot));

        verify(notificationRepository, never()).save(any());
        verify(sender, never()).send(any(), any(), any());
    }

    @Test
    @DisplayName("Insufficient verdict (no stats yet) does not trigger")
    void noStats_skipped() {
        long routeId = 1L;
        LocalDate departureDate = LocalDate.of(2026, 5, 10);
        FareSnapshot snapshot = persistedSnapshot(routeId, departureDate, 150_000L);
        AlertRule rule = persistedRule(11L, routeId, VerdictTrigger.CHEAP, departureDate);
        when(alertRuleRepository.findByRouteIdAndActiveTrue(routeId)).thenReturn(List.of(rule));
        when(statisticsRepository.findByRouteIdAndDepartureDate(routeId, departureDate))
                .thenReturn(Optional.empty());

        handler.on(new FareCollectedEvent(snapshot));

        verify(notificationRepository, never()).save(any());
        verify(sender, never()).send(any(), any(), any());
    }

    @Test
    @DisplayName("no rules registered for route is a no-op")
    void noRules_noOp() {
        FareSnapshot snapshot = persistedSnapshot(1L, LocalDate.of(2026, 5, 10), 150_000L);
        when(alertRuleRepository.findByRouteIdAndActiveTrue(1L)).thenReturn(List.of());

        handler.on(new FareCollectedEvent(snapshot));

        verifyNoInteractions(statisticsRepository, notificationRepository);
        verify(sender, never()).send(any(), any(), any());
    }

    // ---- helpers ----

    private static FareSnapshot persistedSnapshot(long routeId, LocalDate date, long price) {
        FareSnapshot s = FareSnapshot.record(routeId, date, Money.krw(price), "MOCK", null);
        setId(FareSnapshot.class, s, 99L);
        return s;
    }

    private static AlertRule persistedRule(
            long id, long routeId, VerdictTrigger trigger, LocalDate departureDate) {
        return persistedRuleWithRange(
                id, routeId, trigger, departureDate.minusDays(1), departureDate.plusDays(1));
    }

    private static AlertRule persistedRuleWithRange(
            long id, long routeId, VerdictTrigger trigger, LocalDate from, LocalDate to) {
        AlertRule rule =
                AlertRule.create(
                        routeId,
                        new EmailAddress("user@example.com"),
                        new DateRange(from, to),
                        null,
                        trigger);
        setId(AlertRule.class, rule, id);
        return rule;
    }

    private static FareStatistics statsWithSamples(
            long routeId, LocalDate date, long avgPrice, double stdDev, int n) {
        return FareStatistics.compute(
                routeId,
                date,
                avgPrice,
                avgPrice - 30_000L,
                avgPrice + 30_000L,
                stdDev,
                n,
                avgPrice - 10_000L,
                avgPrice + 10_000L);
    }

    private static <T> void setId(Class<T> clazz, T instance, Long id) {
        try {
            Field f = clazz.getDeclaredField("id");
            f.setAccessible(true);
            f.set(instance, id);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
