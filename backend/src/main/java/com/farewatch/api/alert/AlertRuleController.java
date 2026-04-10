package com.farewatch.api.alert;

import com.farewatch.api.common.ApiResponse;
import com.farewatch.api.common.ResourceNotFoundException;
import com.farewatch.domain.alert.AlertRule;
import com.farewatch.domain.alert.AlertRuleRepository;
import com.farewatch.domain.route.RouteRepository;
import com.farewatch.domain.shared.DateRange;
import com.farewatch.domain.shared.EmailAddress;
import com.farewatch.domain.shared.Money;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Alert Rules", description = "사용자 알림 규칙 CRUD (등록/조회/비활성화)")
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

    @Operation(
            summary = "알림 규칙 등록",
            description =
                    "노선·이메일·출발일 범위·트리거 조건으로 새 알림 규칙을 등록한다. "
                            + "동일 (rule, departureDate) 조합은 7일 이내 중복 발송이 자동 차단됨.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "검증 실패 (이메일 형식, 필수 필드 누락 등)"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "노선을 찾을 수 없음")
    })
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
    @Operation(
            summary = "알림 규칙 목록",
            description = "활성 알림 규칙 목록을 반환한다. routeId 쿼리로 특정 노선에 한정 가능.")
    @ApiResponses(@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200"))
    @GetMapping
    public ApiResponse<List<AlertRuleResponse>> list(
            @Parameter(description = "필터 노선 ID (선택)") @RequestParam(required = false)
                    Long routeId) {
        List<AlertRule> rules =
                (routeId != null)
                        ? alertRuleRepository.findByRouteIdAndActiveTrue(routeId)
                        : alertRuleRepository.findByActiveTrue();
        return ApiResponse.ok(rules.stream().map(AlertRuleResponse::from).toList());
    }

    @Operation(
            summary = "알림 규칙 삭제 (soft delete)",
            description =
                    "알림 규칙을 비활성화한다. 실제 삭제가 아닌 active=false 로 변경 — "
                            + "Notification 이력의 FK 무결성 보존을 위함.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "알림 규칙을 찾을 수 없음")
    })
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivate(@Parameter(description = "알림 규칙 ID") @PathVariable Long id) {
        AlertRule rule =
                alertRuleRepository
                        .findById(id)
                        .orElseThrow(
                                () -> new ResourceNotFoundException("alert rule not found: " + id));
        rule.deactivate();
        alertRuleRepository.save(rule);
    }
}
