package com.farewatch.api.route;

import com.farewatch.api.common.ApiResponse;
import com.farewatch.api.common.ResourceNotFoundException;
import com.farewatch.domain.route.Route;
import com.farewatch.domain.route.RouteRepository;
import com.farewatch.domain.shared.AirportCode;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 노선 CRUD 컨트롤러.
 */
@Tag(name = "Routes", description = "모니터링 대상 항공 노선 관리")
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

    @Operation(
            summary = "노선 등록",
            description = "출발/도착 IATA 코드로 새 노선을 등록한다. 동일 노선이 이미 있으면 400.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "검증 실패 또는 중복 노선")
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<RouteResponse> create(@Valid @RequestBody RouteCreateRequest req) {
        AirportCode origin = new AirportCode(req.origin().toUpperCase());
        AirportCode destination = new AirportCode(req.destination().toUpperCase());

        routeRepository
                .findByOriginAndDestination(origin, destination)
                .ifPresent(
                        existing -> {
                            throw new IllegalArgumentException(
                                    "route already exists: "
                                            + origin.value()
                                            + " → "
                                            + destination.value()
                                            + " (id="
                                            + existing.getId()
                                            + ")");
                        });

        Route route = Route.create(origin, destination, req.airlineCode());
        Route saved = routeRepository.save(route);
        return ApiResponse.ok(RouteResponse.from(saved));
    }

    @Operation(
            summary = "노선 비활성화",
            description = "노선을 비활성화한다. 연관된 데이터는 보존되며 스케줄러가 더 이상 수집하지 않는다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "노선을 찾을 수 없음")
    })
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivate(@Parameter(description = "노선 ID") @PathVariable Long id) {
        Route route =
                routeRepository
                        .findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("route not found: " + id));
        route.deactivate();
        routeRepository.save(route);
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
