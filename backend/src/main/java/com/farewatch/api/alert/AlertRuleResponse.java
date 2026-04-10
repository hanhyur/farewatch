package com.farewatch.api.alert;

import com.farewatch.domain.alert.AlertRule;
import com.farewatch.domain.shared.VerdictTrigger;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 알림 규칙 응답. */
public record AlertRuleResponse(
        Long id,
        Long routeId,
        String userIdentifier,
        LocalDate departureDateFrom,
        LocalDate departureDateTo,
        Long targetPrice,
        VerdictTrigger verdictTrigger,
        boolean active,
        LocalDateTime createdAt) {

    public static AlertRuleResponse from(AlertRule rule) {
        return new AlertRuleResponse(
                rule.getId(),
                rule.getRouteId(),
                rule.getUserIdentifier().value(),
                rule.getDepartureRange().from(),
                rule.getDepartureRange().to(),
                rule.getTargetPrice() == null ? null : rule.getTargetPrice().amount(),
                rule.getVerdictTrigger(),
                rule.isActive(),
                rule.getCreatedAt());
    }
}
