package com.farewatch.domain.alert;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.farewatch.domain.shared.FareVerdictKind;
import com.farewatch.domain.shared.Money;
import com.farewatch.domain.shared.NotificationChannel;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Notification 엔티티 (단위)")
class NotificationTest {

    private static final Long RULE_ID = 1L;
    private static final Long ROUTE_ID = 10L;
    private static final LocalDate DEP = LocalDate.of(2026, 5, 15);

    @Test
    @DisplayName("유효한 필드로 send() 팩토리 생성")
    void createWithValidFields() {
        Notification n = Notification.send(
                RULE_ID,
                ROUTE_ID,
                DEP,
                FareVerdictKind.CHEAP,
                Money.krw(178_000L),
                NotificationChannel.LOG,
                "지금 구매를 추천합니다");

        assertThat(n.getAlertRuleId()).isEqualTo(RULE_ID);
        assertThat(n.getRouteId()).isEqualTo(ROUTE_ID);
        assertThat(n.getDepartureDate()).isEqualTo(DEP);
        assertThat(n.getVerdict()).isEqualTo(FareVerdictKind.CHEAP);
        assertThat(n.getPriceAtSend()).isEqualTo(Money.krw(178_000L));
        assertThat(n.getChannel()).isEqualTo(NotificationChannel.LOG);
        assertThat(n.getMessage()).isEqualTo("지금 구매를 추천합니다");
        assertThat(n.getSentAt()).isNotNull();
    }

    @Test
    @DisplayName("null alertRuleId 거부")
    void rejectsNullRuleId() {
        assertThatThrownBy(() -> Notification.send(
                null, ROUTE_ID, DEP, FareVerdictKind.CHEAP,
                Money.krw(100L), NotificationChannel.LOG, "x"))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("null verdict 거부")
    void rejectsNullVerdict() {
        assertThatThrownBy(() -> Notification.send(
                RULE_ID, ROUTE_ID, DEP, null,
                Money.krw(100L), NotificationChannel.LOG, "x"))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("null channel 거부")
    void rejectsNullChannel() {
        assertThatThrownBy(() -> Notification.send(
                RULE_ID, ROUTE_ID, DEP, FareVerdictKind.CHEAP,
                Money.krw(100L), null, "x"))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("0원 priceAtSend 거부 (무의미한 알림)")
    void rejectsZeroPrice() {
        assertThatThrownBy(() -> Notification.send(
                RULE_ID, ROUTE_ID, DEP, FareVerdictKind.CHEAP,
                Money.krw(0L), NotificationChannel.LOG, "x"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("price");
    }
}
