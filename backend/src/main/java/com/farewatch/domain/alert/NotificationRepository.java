package com.farewatch.domain.alert;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /** 최신 발송순으로 페이지 반환 (알림 이력 화면). */
    List<Notification> findAllByOrderBySentAtDesc(Pageable pageable);

    /**
     * 7일 중복 방지 쿼리. 특정 {@code (alertRuleId, departureDate)} 조합에 대해
     * {@code since} 시점 이후 발송된 알림이 있는지 확인.
     *
     * <p>애플리케이션은 발송 직전 {@code since = now().minusDays(7)} 로 이 쿼리를
     * 호출해서 true 이면 skip 한다.
     */
    boolean existsByAlertRuleIdAndDepartureDateAndSentAtAfter(
            Long alertRuleId, LocalDate departureDate, LocalDateTime since);
}
