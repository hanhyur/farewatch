package com.farewatch.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.farewatch.application.alert.NotificationSender;
import com.farewatch.application.collector.FareCollectionService;
import com.farewatch.application.event.FareCollectedEvent;
import com.farewatch.domain.alert.AlertRule;
import com.farewatch.domain.alert.AlertRuleRepository;
import com.farewatch.domain.alert.Notification;
import com.farewatch.domain.alert.NotificationRepository;
import com.farewatch.domain.fare.FareSnapshot;
import com.farewatch.domain.fare.FareSnapshotRepository;
import com.farewatch.domain.fare.FareStatistics;
import com.farewatch.domain.fare.FareStatisticsRepository;
import com.farewatch.domain.judgment.FareVerdict;
import com.farewatch.domain.route.Route;
import com.farewatch.domain.route.RouteRepository;
import com.farewatch.domain.shared.AirportCode;
import com.farewatch.domain.shared.DateRange;
import com.farewatch.domain.shared.EmailAddress;
import com.farewatch.domain.shared.FareVerdictKind;
import com.farewatch.domain.shared.Money;
import com.farewatch.domain.shared.NotificationChannel;
import com.farewatch.domain.shared.VerdictTrigger;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;

/**
 * 전체 이벤트 파이프라인 통합 테스트.
 *
 * <p>실제 빈 구성으로 검증:
 *
 * <ul>
 *   <li>실제 H2 + JPA + Hibernate
 *   <li>실제 {@code FareCollectionService}, {@code StatisticsUpdateHandler},
 *       {@code AlertEvaluationHandler}, {@code FareVerdictCalculator}
 *   <li>{@link NotificationSender} 만 {@code @MockBean} 으로 교체 — 발송 부수효과 차단,
 *       호출 검증
 * </ul>
 *
 * <p>전략: {@link MockFareCollector} 의 가격은 deterministic 이지만 cheap 임계와의
 * 관계는 통계 상태에 따라 달라진다. 결정성을 확보하기 위해 알림 흐름 테스트는
 * {@link ApplicationEventPublisher} 로 가격을 직접 통제한 이벤트를 발행한다.
 * 수집 흐름 테스트는 별도로 {@code collectionService.collectFor} 를 호출해서 wiring 만
 * 검증한다.
 */
@SpringBootTest
@Transactional
class FullEventPipelineIT {

    @Autowired private FareCollectionService collectionService;
    @Autowired private ApplicationEventPublisher eventPublisher;
    @Autowired private RouteRepository routeRepository;
    @Autowired private FareSnapshotRepository snapshotRepository;
    @Autowired private FareStatisticsRepository statisticsRepository;
    @Autowired private AlertRuleRepository alertRuleRepository;
    @Autowired private NotificationRepository notificationRepository;

    @MockBean private NotificationSender notificationSender;

    private Route route;
    private static final LocalDate DEPARTURE_DATE = LocalDate.of(2026, 5, 10);

    @BeforeEach
    void setUp() {
        org.mockito.Mockito.when(notificationSender.channel()).thenReturn(NotificationChannel.LOG);

        notificationRepository.deleteAll();
        alertRuleRepository.deleteAll();
        statisticsRepository.deleteAll();
        snapshotRepository.deleteAll();
        routeRepository.deleteAll();

        route =
                routeRepository.save(
                        Route.create(new AirportCode("PUS"), new AirportCode("NRT"), "KE"));
    }

    @Test
    @DisplayName("collectFor wires through to snapshot save + stats upsert")
    void collectFor_wiresThroughHandlers() {
        long snapshotsBefore = snapshotRepository.count();

        collectionService.collectFor(route, List.of(DEPARTURE_DATE));

        assertThat(snapshotRepository.count()).isEqualTo(snapshotsBefore + 1);

        FareStatistics stats =
                statisticsRepository
                        .findByRouteIdAndDepartureDate(route.getId(), DEPARTURE_DATE)
                        .orElseThrow(
                                () ->
                                        new AssertionError(
                                                "StatisticsUpdateHandler should have created a stats row"));
        assertThat(stats.getSampleCount()).isEqualTo(1);
        assertThat(stats.getAvgPrice()).isPositive();
    }

    @Test
    @DisplayName("alert flow: cheap snapshot triggers Notification + sender dispatch")
    void alertFlow_cheapSnapshot_persistsAndDispatches() {
        // Given: 30 표본 (평균 200_000, 분산 작음) → 통계 cheap 임계 = 195_000
        seedStats(200_000L, 5_000.0, 30);

        // alert rule
        AlertRule rule =
                alertRuleRepository.save(
                        AlertRule.create(
                                route.getId(),
                                new EmailAddress("user@example.com"),
                                new DateRange(
                                        DEPARTURE_DATE.minusDays(5),
                                        DEPARTURE_DATE.plusDays(5)),
                                null,
                                VerdictTrigger.CHEAP));

        // When: 명백히 cheap 인 가격으로 새 스냅샷 저장 + 이벤트 발행
        FareSnapshot cheapSnapshot =
                snapshotRepository.save(cheapSnap(150_000L));
        eventPublisher.publishEvent(new FareCollectedEvent(cheapSnapshot));

        // Then: 알림 1건 저장 + sender 1번 호출
        assertThat(notificationRepository.count()).isEqualTo(1L);
        Notification saved = notificationRepository.findAll().get(0);
        assertThat(saved.getAlertRuleId()).isEqualTo(rule.getId());
        assertThat(saved.getRouteId()).isEqualTo(route.getId());
        assertThat(saved.getDepartureDate()).isEqualTo(DEPARTURE_DATE);
        assertThat(saved.getVerdict()).isEqualTo(FareVerdictKind.CHEAP);
        assertThat(saved.getPriceAtSend().amount()).isEqualTo(150_000L);

        verify(notificationSender, times(1))
                .send(eq(rule), any(FareVerdict.Cheap.class), eq(cheapSnapshot));
    }

    @Test
    @DisplayName("alert flow: 7-day dedupe blocks second event for same (rule, date)")
    void alertFlow_duplicateBlockedWithin7Days() {
        seedStats(200_000L, 5_000.0, 30);
        alertRuleRepository.save(
                AlertRule.create(
                        route.getId(),
                        new EmailAddress("user@example.com"),
                        new DateRange(DEPARTURE_DATE.minusDays(5), DEPARTURE_DATE.plusDays(5)),
                        null,
                        VerdictTrigger.CHEAP));

        FareSnapshot first = snapshotRepository.save(cheapSnap(150_000L));
        eventPublisher.publishEvent(new FareCollectedEvent(first));

        FareSnapshot second = snapshotRepository.save(cheapSnap(140_000L));
        eventPublisher.publishEvent(new FareCollectedEvent(second));

        assertThat(notificationRepository.count())
                .as("두 번째 이벤트는 7일 중복 차단되어야 함")
                .isEqualTo(1L);
        verify(notificationSender, times(1)).send(any(), any(), any());
    }

    @Test
    @DisplayName("alert flow: rule on different date range is skipped")
    void alertFlow_outOfRangeRuleSkipped() {
        seedStats(200_000L, 5_000.0, 30);
        // 출발일이 범위 밖
        alertRuleRepository.save(
                AlertRule.create(
                        route.getId(),
                        new EmailAddress("user@example.com"),
                        new DateRange(
                                DEPARTURE_DATE.plusDays(30), DEPARTURE_DATE.plusDays(60)),
                        null,
                        VerdictTrigger.CHEAP));

        FareSnapshot cheap = snapshotRepository.save(cheapSnap(150_000L));
        eventPublisher.publishEvent(new FareCollectedEvent(cheap));

        assertThat(notificationRepository.count()).isZero();
        verify(notificationSender, org.mockito.Mockito.never()).send(any(), any(), any());
    }

    // ---- helpers ----

    private FareSnapshot cheapSnap(long price) {
        return FareSnapshot.record(route.getId(), DEPARTURE_DATE, Money.krw(price), "TEST", null);
    }

    private void seedStats(long avgPrice, double stdDev, int sampleCount) {
        statisticsRepository.save(
                FareStatistics.compute(
                        route.getId(),
                        DEPARTURE_DATE,
                        avgPrice,
                        avgPrice - 30_000L,
                        avgPrice + 30_000L,
                        stdDev,
                        sampleCount,
                        avgPrice - 10_000L,
                        avgPrice + 10_000L));
    }
}
