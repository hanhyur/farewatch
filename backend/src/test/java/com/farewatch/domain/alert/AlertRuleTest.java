package com.farewatch.domain.alert;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.farewatch.domain.shared.DateRange;
import com.farewatch.domain.shared.EmailAddress;
import com.farewatch.domain.shared.Money;
import com.farewatch.domain.shared.VerdictTrigger;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("AlertRule 엔티티 (단위)")
class AlertRuleTest {

    private static final Long ROUTE_ID = 1L;
    private static final EmailAddress EMAIL = new EmailAddress("hanhyur@example.com");
    private static final DateRange RANGE =
            new DateRange(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31));

    @Test
    @DisplayName("필수값으로 create() 팩토리 생성")
    void createWithRequiredFields() {
        AlertRule rule = AlertRule.create(
                ROUTE_ID, EMAIL, RANGE, Money.krw(200_000L), VerdictTrigger.CHEAP);

        assertThat(rule.getRouteId()).isEqualTo(ROUTE_ID);
        assertThat(rule.getUserIdentifier()).isEqualTo(EMAIL);
        assertThat(rule.getDepartureRange()).isEqualTo(RANGE);
        assertThat(rule.getTargetPrice()).isEqualTo(Money.krw(200_000L));
        assertThat(rule.getVerdictTrigger()).isEqualTo(VerdictTrigger.CHEAP);
        assertThat(rule.isActive()).isTrue();
        assertThat(rule.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("targetPrice 는 nullable (사용자가 목표가 미지정)")
    void nullTargetPriceAllowed() {
        AlertRule rule = AlertRule.create(ROUTE_ID, EMAIL, RANGE, null, VerdictTrigger.CHEAP_OR_FAIR);

        assertThat(rule.getTargetPrice()).isNull();
    }

    @Test
    @DisplayName("null verdictTrigger 거부")
    void rejectsNullTrigger() {
        assertThatThrownBy(() -> AlertRule.create(ROUTE_ID, EMAIL, RANGE, Money.krw(100L), null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("null routeId 거부")
    void rejectsNullRouteId() {
        assertThatThrownBy(() -> AlertRule.create(
                null, EMAIL, RANGE, Money.krw(100L), VerdictTrigger.CHEAP))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("null email 거부")
    void rejectsNullEmail() {
        assertThatThrownBy(() -> AlertRule.create(
                ROUTE_ID, null, RANGE, Money.krw(100L), VerdictTrigger.CHEAP))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("null range 거부")
    void rejectsNullRange() {
        assertThatThrownBy(() -> AlertRule.create(
                ROUTE_ID, EMAIL, null, Money.krw(100L), VerdictTrigger.CHEAP))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("deactivate() / activate() 상태 전이")
    void activateDeactivate() {
        AlertRule rule = AlertRule.create(ROUTE_ID, EMAIL, RANGE, null, VerdictTrigger.CHEAP);

        rule.deactivate();
        assertThat(rule.isActive()).isFalse();

        rule.activate();
        assertThat(rule.isActive()).isTrue();
    }
}
