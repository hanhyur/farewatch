package com.farewatch.domain.alert;

import static org.assertj.core.api.Assertions.assertThat;

import com.farewatch.domain.shared.FareVerdictKind;
import com.farewatch.domain.shared.Money;
import com.farewatch.domain.shared.NotificationChannel;
import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@DataJpaTest
@DisplayName("NotificationRepository (@DataJpaTest)")
class NotificationRepositoryTest {

    @Autowired
    private NotificationRepository repository;

    private static final Long RULE_1 = 1L;
    private static final Long RULE_2 = 2L;
    private static final Long ROUTE = 10L;
    private static final LocalDate MAY_15 = LocalDate.of(2026, 5, 15);
    private static final LocalDate MAY_16 = LocalDate.of(2026, 5, 16);

    @Test
    @DisplayName("save + findById: 임베디드 Money, enum 저장")
    void saveAndFind() {
        Notification saved = repository.save(Notification.send(
                RULE_1, ROUTE, MAY_15, FareVerdictKind.CHEAP,
                Money.krw(178_000L), NotificationChannel.LOG, "지금 사세요"));

        var found = repository.findById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getVerdict()).isEqualTo(FareVerdictKind.CHEAP);
        assertThat(found.get().getPriceAtSend()).isEqualTo(Money.krw(178_000L));
        assertThat(found.get().getChannel()).isEqualTo(NotificationChannel.LOG);
    }

    @Test
    @DisplayName("existsByAlertRuleIdAndDepartureDateAndSentAtAfter: 최근 7일 내 알림 있으면 true")
    void duplicatePreventionWithinWindow() {
        repository.save(Notification.send(
                RULE_1, ROUTE, MAY_15, FareVerdictKind.CHEAP,
                Money.krw(178_000L), NotificationChannel.LOG, "msg"));

        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        boolean exists =
                repository.existsByAlertRuleIdAndDepartureDateAndSentAtAfter(
                        RULE_1, MAY_15, sevenDaysAgo);

        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("7일 윈도우 밖 알림은 쿼리에 걸리지 않음")
    void outOfWindowExcluded() throws Exception {
        Notification n = Notification.send(
                RULE_1, ROUTE, MAY_15, FareVerdictKind.CHEAP,
                Money.krw(178_000L), NotificationChannel.LOG, "msg");
        // 리플렉션으로 sentAt을 8일 전으로 조작 (불변 엔티티라 세터 없음)
        setSentAt(n, LocalDateTime.now().minusDays(8));
        repository.save(n);

        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        boolean exists =
                repository.existsByAlertRuleIdAndDepartureDateAndSentAtAfter(
                        RULE_1, MAY_15, sevenDaysAgo);

        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("다른 ruleId/departureDate는 중복 쿼리에 걸리지 않음")
    void differentKeyNotDuplicate() {
        repository.save(Notification.send(
                RULE_1, ROUTE, MAY_15, FareVerdictKind.CHEAP,
                Money.krw(100_000L), NotificationChannel.LOG, "msg"));

        LocalDateTime recent = LocalDateTime.now().minusDays(7);

        assertThat(repository.existsByAlertRuleIdAndDepartureDateAndSentAtAfter(
                RULE_2, MAY_15, recent)).isFalse();
        assertThat(repository.existsByAlertRuleIdAndDepartureDateAndSentAtAfter(
                RULE_1, MAY_16, recent)).isFalse();
    }

    @Test
    @DisplayName("findAllByOrderBySentAtDesc: 최신순 페이징")
    void pagedLatestFirst() throws InterruptedException {
        for (int i = 0; i < 5; i++) {
            repository.save(Notification.send(
                    RULE_1, ROUTE, MAY_15, FareVerdictKind.CHEAP,
                    Money.krw(100_000L + i), NotificationChannel.LOG, "msg " + i));
            Thread.sleep(5);
        }

        Pageable pageable = PageRequest.of(0, 3);
        List<Notification> page = repository.findAllByOrderBySentAtDesc(pageable);

        assertThat(page).hasSize(3);
        // 최신이 먼저
        assertThat(page.get(0).getSentAt()).isAfterOrEqualTo(page.get(1).getSentAt());
        assertThat(page.get(1).getSentAt()).isAfterOrEqualTo(page.get(2).getSentAt());
    }

    private static void setSentAt(Notification n, LocalDateTime t) throws Exception {
        Field f = Notification.class.getDeclaredField("sentAt");
        f.setAccessible(true);
        f.set(n, t);
    }
}
