package com.farewatch.api.airport;

import com.farewatch.api.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Airports", description = "공항 검색 (자동완성)")
@RestController
@RequestMapping("/api/v1/airports")
public class AirportController {

    @Operation(
            summary = "공항 검색",
            description = "도시명, IATA 코드, 공항명, 국가명으로 공항을 검색한다. "
                    + "파라미터 없이 호출하면 전체 목록 반환.")
    @GetMapping
    public ApiResponse<List<AirportInfo>> search(
            @Parameter(description = "검색어 (도시명/IATA/공항명)", example = "도쿄")
            @RequestParam(required = false, defaultValue = "") String q) {
        List<AirportInfo> results = AirportRepository.search(q);
        return ApiResponse.ok(results);
    }
}
