package com.farewatch.domain.alert;

import com.farewatch.domain.shared.EmailAddress;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlertRuleRepository extends JpaRepository<AlertRule, Long> {

    List<AlertRule> findByActiveTrue();

    List<AlertRule> findByRouteIdAndActiveTrue(Long routeId);

    List<AlertRule> findByUserIdentifier(EmailAddress userIdentifier);
}
