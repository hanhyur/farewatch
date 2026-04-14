package com.farewatch.api.search;

import com.farewatch.api.common.ApiResponse;
import com.farewatch.api.search.SearchRequest.TripType;
import com.farewatch.application.search.FlightSearchService;
import com.farewatch.application.search.MockFlightSearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 실시간 항공편 검색 API. SerpApi 키가 있으면 실제 검색, 없으면 Mock 데이터.
 */
@Tag(name = "Search", description = "실시간 항공편 검색")
@RestController
@RequestMapping("/api/v1/search")
public class SearchController {

    private final Optional<FlightSearchService> realService;
    private final Optional<MockFlightSearchService> mockService;

    public SearchController(
            Optional<FlightSearchService> realService,
            Optional<MockFlightSearchService> mockService) {
        this.realService = realService;
        this.mockService = mockService;
    }

    @Operation(
            summary = "항공편 검색",
            description = "출발지/도착지/날짜/여정유형/경유 조건으로 항공편을 검색한다. "
                    + "SerpApi 키가 설정된 경우 실시간 결과, 아닌 경우 Mock 데이터를 반환한다. "
                    + "동일 조건은 1시간 캐시.")
    @GetMapping
    public ApiResponse<FlightSearchResult> search(
            @Parameter(description = "출발 공항 IATA 코드", example = "PUS")
            @RequestParam String origin,

            @Parameter(description = "도착 공항 IATA 코드", example = "NRT")
            @RequestParam String destination,

            @Parameter(description = "출발일 (YYYY-MM-DD)", example = "2026-05-01")
            @RequestParam LocalDate departureDate,

            @Parameter(description = "귀국일 (YYYY-MM-DD, 왕복 시 필수)")
            @RequestParam(required = false) LocalDate returnDate,

            @Parameter(description = "여정 유형: ONE_WAY / ROUND_TRIP (기본: ONE_WAY)")
            @RequestParam(defaultValue = "ONE_WAY") TripType tripType,

            @Parameter(description = "경유 필터: 0=전체, 1=직항만, 2=1회이하, 3=2회이하 (기본: 0)")
            @RequestParam(defaultValue = "0") int stops) {

        String o = origin.toUpperCase().trim();
        String d = destination.toUpperCase().trim();

        if (o.length() != 3 || d.length() != 3) {
            return ApiResponse.error("IATA code must be exactly 3 characters");
        }
        if (departureDate.isBefore(LocalDate.now())) {
            return ApiResponse.error("departureDate must be today or later");
        }
        if (tripType == TripType.ROUND_TRIP && returnDate == null) {
            return ApiResponse.error("returnDate is required for round-trip search");
        }
        if (returnDate != null && returnDate.isBefore(departureDate)) {
            return ApiResponse.error("returnDate must be on or after departureDate");
        }
        if (stops < 0 || stops > 3) {
            return ApiResponse.error("stops must be 0, 1, 2, or 3");
        }

        SearchRequest req = new SearchRequest(o, d, departureDate, returnDate, tripType, stops);

        FlightSearchResult result = realService
                .map(svc -> svc.search(req))
                .orElseGet(() -> mockService
                        .map(svc -> svc.search(req))
                        .orElseThrow(() -> new IllegalStateException(
                                "No search service available")));

        return ApiResponse.ok(result);
    }
}
