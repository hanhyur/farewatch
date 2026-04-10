package com.farewatch.api.alert;

import com.farewatch.api.common.ApiResponse;
import com.farewatch.domain.alert.NotificationRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 알림 이력 컨트롤러. {@code GET /api/v1/notifications} 만 노출 — 알림은 시스템이 자동
 * 발송하므로 수동 생성 API 는 없다.
 *
 * <p>최신순 정렬, 기본 50건. 클라이언트에서 더 받고 싶으면 {@code limit} 파라미터 사용.
 */
@Tag(name = "Notifications", description = "알림 발송 이력 조회 (read-only)")
@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    static final int DEFAULT_LIMIT = 50;
    static final int MAX_LIMIT = 200;

    private final NotificationRepository notificationRepository;

    public NotificationController(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Operation(
            summary = "알림 이력",
            description = "발송된 알림을 sent_at 내림차순(최신 먼저)으로 반환한다.")
    @ApiResponses(@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200"))
    @GetMapping
    public ApiResponse<List<NotificationResponse>> list(
            @Parameter(description = "최대 결과 수 (기본 50, 최대 200)")
                    @RequestParam(required = false)
                    Integer limit) {
        int pageSize = clamp(limit);
        List<NotificationResponse> body =
                notificationRepository.findAllByOrderBySentAtDesc(PageRequest.of(0, pageSize))
                        .stream()
                        .map(NotificationResponse::from)
                        .toList();
        return ApiResponse.ok(body);
    }

    private static int clamp(Integer raw) {
        if (raw == null || raw <= 0) {
            return DEFAULT_LIMIT;
        }
        return Math.min(raw, MAX_LIMIT);
    }
}
