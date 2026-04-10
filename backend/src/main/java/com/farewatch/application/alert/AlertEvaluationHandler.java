package com.farewatch.application.alert;

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
import com.farewatch.domain.shared.FareVerdictKind;
import com.farewatch.domain.shared.VerdictTrigger;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@link FareCollectedEvent} 가 발행되면 그 노선의 활성 {@link AlertRule} 를 순회하며
 * 알림 조건을 평가하고, 조건에 맞으면 {@link Notification} 을 영속화한 뒤
 * {@link NotificationSender} 로 발송한다.
 *
 * <h2>알림 발동 조건</h2>
 *
 * <ol>
 *   <li>알림 규칙이 active 이고, {@code rule.departureRange} 가 스냅샷의 출발일을 포함
 *   <li>현재 가격에 대한 {@link FareVerdict} 가 {@link VerdictTrigger} 와 매칭
 *       <ul>
 *         <li>{@code CHEAP}: Cheap 만 매칭
 *         <li>{@code CHEAP_OR_FAIR}: Cheap 또는 Fair 매칭
 *       </ul>
 *   <li>동일 {@code (alertRuleId, departureDate)} 조합으로 7일 이내 발송 이력 없음
 * </ol>
 *
 * <h2>스킵 케이스</h2>
 *
 * <ul>
 *   <li>해당 노선에 등록된 active 규칙이 없음 → no-op
 *   <li>{@link FareStatistics} 가 아직 없음 → 통계 없이 판정 불가, skip
 *   <li>판정이 {@link FareVerdict.Insufficient} 또는 {@link FareVerdict.Expensive} → 알림
 *       대상 아님
 *   <li>중복 발송 윈도우 (7일) 내 → skip
 * </ul>
 */
@Component
public class AlertEvaluationHandler {

    /** 같은 (alertRule, departureDate) 조합의 재발송 차단 윈도우. */
    static final int DUPLICATE_WINDOW_DAYS = 7;

    private static final Logger log = LoggerFactory.getLogger(AlertEvaluationHandler.class);

    private final AlertRuleRepository alertRuleRepository;
    private final FareStatisticsRepository statisticsRepository;
    private final NotificationRepository notificationRepository;
    private final FareVerdictCalculator verdictCalculator;
    private final List<NotificationSender> senders;
    private final Clock clock;

    public AlertEvaluationHandler(
            AlertRuleRepository alertRuleRepository,
            FareStatisticsRepository statisticsRepository,
            NotificationRepository notificationRepository,
            FareVerdictCalculator verdictCalculator,
            List<NotificationSender> senders,
            Clock clock) {
        this.alertRuleRepository = Objects.requireNonNull(alertRuleRepository);
        this.statisticsRepository = Objects.requireNonNull(statisticsRepository);
        this.notificationRepository = Objects.requireNonNull(notificationRepository);
        this.verdictCalculator = Objects.requireNonNull(verdictCalculator);
        this.senders = Objects.requireNonNull(senders);
        this.clock = Objects.requireNonNull(clock);
    }

    @EventListener
    @Transactional
    public void on(FareCollectedEvent event) {
        FareSnapshot snapshot = event.snapshot();
        Long routeId = snapshot.getRouteId();
        LocalDate departureDate = snapshot.getDepartureDate();

        List<AlertRule> rules = alertRuleRepository.findByRouteIdAndActiveTrue(routeId);
        // 출발일 범위에 들어오는 규칙만 추림 — 통계/판정 비용을 아끼기 위해 미리 필터링.
        List<AlertRule> applicable =
                rules.stream()
                        .filter(r -> r.getDepartureRange().contains(departureDate))
                        .toList();
        if (applicable.isEmpty()) {
            return;
        }

        Optional<FareStatistics> stats =
                statisticsRepository.findByRouteIdAndDepartureDate(routeId, departureDate);
        if (stats.isEmpty()) {
            // 통계 핸들러보다 알림 핸들러가 먼저 실행되거나, 첫 수집이라 stats 가 아직
            // 없는 케이스. 다음 수집 사이클에서 자연스럽게 처리됨.
            log.debug(
                    "AlertEvaluationHandler: no stats yet for routeId={}, departureDate={}; skipping",
                    routeId,
                    departureDate);
            return;
        }

        FareVerdict verdict =
                verdictCalculator.evaluate(snapshot.getPrice().amount(), stats.get());

        for (AlertRule rule : applicable) {
            if (!triggerMatches(rule.getVerdictTrigger(), verdict)) {
                continue;
            }
            if (isDuplicate(rule.getId(), departureDate)) {
                log.debug(
                        "AlertEvaluationHandler: duplicate alert window for ruleId={}, departureDate={}; skipping",
                        rule.getId(),
                        departureDate);
                continue;
            }

            FareVerdictKind kind = verdict.kind();
            String message = buildMessage(verdict);
            for (NotificationSender sender : senders) {
                Notification notification =
                        Notification.send(
                                rule.getId(),
                                routeId,
                                departureDate,
                                kind,
                                snapshot.getPrice(),
                                sender.channel(),
                                message);
                notificationRepository.save(notification);
                sender.send(rule, verdict, snapshot);
            }
        }
    }

    private static boolean triggerMatches(VerdictTrigger trigger, FareVerdict verdict) {
        return switch (trigger) {
            case CHEAP -> verdict instanceof FareVerdict.Cheap;
            case CHEAP_OR_FAIR ->
                    verdict instanceof FareVerdict.Cheap || verdict instanceof FareVerdict.Fair;
        };
    }

    private boolean isDuplicate(Long alertRuleId, LocalDate departureDate) {
        LocalDateTime since = LocalDateTime.now(clock).minusDays(DUPLICATE_WINDOW_DAYS);
        return notificationRepository.existsByAlertRuleIdAndDepartureDateAndSentAtAfter(
                alertRuleId, departureDate, since);
    }

    private static String buildMessage(FareVerdict verdict) {
        return switch (verdict) {
            case FareVerdict.Cheap c ->
                    "지금 구매를 추천합니다 — 평균 "
                            + c.avgPrice()
                            + "원 대비 현재 "
                            + c.currentPrice()
                            + "원";
            case FareVerdict.Fair f ->
                    "적정가입니다 — 평균 "
                            + f.avgPrice()
                            + "원, 현재 "
                            + f.currentPrice()
                            + "원";
            case FareVerdict.Expensive e ->
                    "더 기다리는 것을 추천합니다 — 평균 "
                            + e.avgPrice()
                            + "원 대비 현재 "
                            + e.currentPrice()
                            + "원";
            case FareVerdict.Insufficient i ->
                    "데이터 부족 — 표본 " + i.sampleCount() + "/" + i.requiredCount();
        };
    }
}
