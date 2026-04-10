package com.farewatch.api.route;

import com.farewatch.api.common.ApiResponse;
import com.farewatch.api.common.ResourceNotFoundException;
import com.farewatch.domain.route.Route;
import com.farewatch.domain.route.RouteRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 노선 목록/상세 컨트롤러. 단순 read-only 라 별도 application 서비스 없이 repository 를
 * 직접 호출한다.
 */
@Tag(name = "Routes", description = "모니터링 대상 항공 노선 조회")
@RestController
@RequestMapping("/api/v1/routes")
public class RouteController {

    private final RouteRepository routeRepository;

    public RouteController(RouteRepository routeRepository) {
        this.routeRepository = routeRepository;
    }

    /** 모든 노선 (활성/비활성 포함) 을 ID 오름차순으로 반환. */
    @Operation(
            summary = "노선 목록",
            description = "등록된 모든 노선을 ID 오름차순으로 반환한다. 활성/비활성 모두 포함.")
    @ApiResponses(@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200"))
    @GetMapping
    public ApiResponse<List<RouteResponse>> list() {
        List<RouteResponse> body =
                routeRepository.findAll().stream()
                        .sorted((a, b) -> Long.compare(a.getId(), b.getId()))
                        .map(RouteResponse::from)
                        .toList();
        return ApiResponse.ok(body);
    }

    /** 노선 단건 조회. 없으면 404. */
    @Operation(summary = "노선 단건 조회", description = "ID 로 노선 정보를 조회한다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "노선을 찾을 수 없음")
    })
    @GetMapping("/{id}")
    public ApiResponse<RouteResponse> get(
            @Parameter(description = "노선 ID", example = "1") @PathVariable Long id) {
        Route route =
                routeRepository
                        .findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("route not found: " + id));
        return ApiResponse.ok(RouteResponse.from(route));
    }
}
