package com.farewatch.domain.alert;

import static org.assertj.core.api.Assertions.assertThat;

import com.farewatch.domain.shared.DateRange;
import com.farewatch.domain.shared.EmailAddress;
import com.farewatch.domain.shared.Money;
import com.farewatch.domain.shared.VerdictTrigger;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest
@DisplayName("AlertRuleRepository (@DataJpaTest)")
class AlertRuleRepositoryTest {

    @Autowired
    private AlertRuleRepository repository;

    private static final EmailAddress ALICE = new EmailAddress("alice@example.com");
    private static final EmailAddress BOB = new EmailAddress("bob@example.com");
    private static final DateRange MAY = new DateRange(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31));
    private static final DateRange JUNE = new DateRange(LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30));

    @Test
    @DisplayName("save + findById: 임베디드 VO 3종 round-trip (Email/DateRange/Money)")
    void saveAndFindWithEmbeddedVOs() {
        AlertRule saved = repository.save(
                AlertRule.create(1L, ALICE, MAY, Money.krw(200_000L), VerdictTrigger.CHEAP));

        Optional<AlertRule> found = repository.findById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getUserIdentifier()).isEqualTo(ALICE);
        assertThat(found.get().getDepartureRange()).isEqualTo(MAY);
        assertThat(found.get().getTargetPrice()).isEqualTo(Money.krw(200_000L));
        assertThat(found.get().getVerdictTrigger()).isEqualTo(VerdictTrigger.CHEAP);
    }

    @Test
    @DisplayName("nullable targetPrice 저장·조회 (amount+currency 모두 null)")
    void nullTargetPriceRoundTrip() {
        AlertRule saved = repository.save(
                AlertRule.create(1L, ALICE, MAY, null, VerdictTrigger.CHEAP_OR_FAIR));

        Optional<AlertRule> found = repository.findById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getTargetPrice()).isNull();
    }

    @Test
    @DisplayName("findByActiveTrue(): 비활성 규칙 제외")
    void findOnlyActive() {
        AlertRule a1 = repository.save(
                AlertRule.create(1L, ALICE, MAY, Money.krw(200_000L), VerdictTrigger.CHEAP));
        AlertRule a2 = repository.save(
                AlertRule.create(2L, BOB, JUNE, null, VerdictTrigger.CHEAP_OR_FAIR));
        AlertRule inactive = AlertRule.create(1L, BOB, MAY, null, VerdictTrigger.CHEAP);
        inactive.deactivate();
        repository.save(inactive);

        List<AlertRule> actives = repository.findByActiveTrue();

        assertThat(actives).extracting(AlertRule::getId)
                .containsExactlyInAnyOrder(a1.getId(), a2.getId());
    }

    @Test
    @DisplayName("findByRouteIdAndActiveTrue(): 특정 노선의 활성 규칙만")
    void findByRouteId() {
        repository.save(AlertRule.create(1L, ALICE, MAY, null, VerdictTrigger.CHEAP));
        repository.save(AlertRule.create(1L, BOB, MAY, null, VerdictTrigger.CHEAP));
        repository.save(AlertRule.create(2L, ALICE, MAY, null, VerdictTrigger.CHEAP));

        List<AlertRule> rules = repository.findByRouteIdAndActiveTrue(1L);

        assertThat(rules).hasSize(2);
    }

    @Test
    @DisplayName("findByUserIdentifier(): 이메일 VO 쿼리")
    void findByEmail() {
        repository.save(AlertRule.create(1L, ALICE, MAY, null, VerdictTrigger.CHEAP));
        repository.save(AlertRule.create(2L, BOB, MAY, null, VerdictTrigger.CHEAP));
        repository.save(AlertRule.create(3L, ALICE, JUNE, null, VerdictTrigger.CHEAP));

        List<AlertRule> aliceRules = repository.findByUserIdentifier(ALICE);

        assertThat(aliceRules).hasSize(2);
        assertThat(aliceRules).allMatch(r -> r.getUserIdentifier().equals(ALICE));
    }
}
