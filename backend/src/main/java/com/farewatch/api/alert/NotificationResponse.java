package com.farewatch.api.alert;

import com.farewatch.domain.alert.Notification;
import com.farewatch.domain.shared.FareVerdictKind;
import com.farewatch.domain.shared.NotificationChannel;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 알림 이력 응답. */
public record NotificationResponse(
        Long id,
        Long alertRuleId,
        Long routeId,
        LocalDate departureDate,
        LocalDateTime sentAt,
        FareVerdictKind verdict,
        long priceAtSend,
        String currency,
        NotificationChannel channel,
        String message) {

    public static NotificationResponse from(Notification n) {
        return new NotificationResponse(
                n.getId(),
                n.getAlertRuleId(),
                n.getRouteId(),
                n.getDepartureDate(),
                n.getSentAt(),
                n.getVerdict(),
                n.getPriceAtSend().amount(),
                n.getPriceAtSend().currency(),
                n.getChannel(),
                n.getMessage());
    }
}
