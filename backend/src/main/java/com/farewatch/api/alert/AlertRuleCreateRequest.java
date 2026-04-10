package com.farewatch.api.alert;

import com.farewatch.domain.shared.VerdictTrigger;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;

/**
 * 알림 규칙 생성 요청. {@link com.farewatch.domain.alert.AlertRule#create} 의 입력
 * 그대로의 형태.
 *
 * <p>{@code targetPrice} 는 선택 — null 가능.
 */
public record AlertRuleCreateRequest(
        @NotNull Long routeId,
        @NotNull @Email String userIdentifier,
        @NotNull LocalDate departureDateFrom,
        @NotNull LocalDate departureDateTo,
        @Positive Long targetPrice,
        @NotNull VerdictTrigger verdictTrigger) {}
