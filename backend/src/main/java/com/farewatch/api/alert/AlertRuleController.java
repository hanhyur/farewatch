package com.farewatch.api.alert;

import com.farewatch.api.common.ApiResponse;
import com.farewatch.api.common.ResourceNotFoundException;
import com.farewatch.domain.alert.AlertRule;
import com.farewatch.domain.alert.AlertRuleRepository;
import com.farewatch.domain.route.RouteRepository;
import com.farewatch.domain.shared.DateRange;
import com.farewatch.domain.shared.EmailAddress;
import com.farewatch.domain.shared.Money;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 알림 규칙 CRUD 컨트롤러.
 *
 * <ul>
 *   <li>{@code POST /api/v1/alert-rules} — 등록 (201 Created)
 *   <li>{@code GET /api/v1/alert-rules} — 활성 규칙 목록. {@code routeId} 쿼리로 필터 가능
 *   <li>{@code DELETE /api/v1/alert-rules/{id}} — soft delete (deactivate)
 * </ul>
 *
 * <p>삭제는 hard delete 가 아니라 {@link AlertRule#deactivate()} 호출로 비활성화
 * — {@link com.farewatch.domain.alert.Notification} FK 무결성 보존.
 */
@RestController
@RequestMapping("/api/v1/alert-rules")
public class AlertRuleController {

    private final AlertRuleRepository alertRuleRepository;
    private final RouteRepository routeRepository;

    public AlertRuleController(
            AlertRuleRepository alertRuleRepository, RouteRepository routeRepository) {
        this.alertRuleRepository = alertRuleRepository;
        this.routeRepository = routeRepository;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<AlertRuleResponse> create(@Valid @RequestBody AlertRuleCreateRequest req) {
        if (!routeRepository.existsById(req.routeId())) {
            throw new ResourceNotFoundException("route not found: " + req.routeId());
        }
        AlertRule rule =
                AlertRule.create(
                        req.routeId(),
                        new EmailAddress(req.userIdentifier()),
                        new DateRange(req.departureDateFrom(), req.departureDateTo()),
                        req.targetPrice() == null ? null : Money.krw(req.targetPrice()),
                        req.verdictTrigger());
        AlertRule saved = alertRuleRepository.save(rule);
        return ApiResponse.ok(AlertRuleResponse.from(saved));
    }

    /** 활성 규칙 목록. {@code routeId} 가 주어지면 해당 노선에 한정. */
    @GetMapping
    public ApiResponse<List<AlertRuleResponse>> list(
            @RequestParam(required = false) Long routeId) {
        List<AlertRule> rules =
                (routeId != null)
                        ? alertRuleRepository.findByRouteIdAndActiveTrue(routeId)
                        : alertRuleRepository.findByActiveTrue();
        return ApiResponse.ok(rules.stream().map(AlertRuleResponse::from).toList());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivate(@PathVariable Long id) {
        AlertRule rule =
                alertRuleRepository
                        .findById(id)
                        .orElseThrow(
                                () -> new ResourceNotFoundException("alert rule not found: " + id));
        rule.deactivate();
        alertRuleRepository.save(rule);
    }
}
