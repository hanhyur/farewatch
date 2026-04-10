package com.farewatch.infrastructure.collector;

import com.farewatch.application.collector.FareCollector;
import com.farewatch.domain.fare.FareSnapshot;
import com.farewatch.domain.route.Route;
import com.farewatch.domain.shared.Money;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * SerpApi Google Flights 를 통한 실제 항공권 가격 수집기.
 *
 * <p>{@code farewatch.collector.serpapi.api-key} 프로퍼티가 설정되어 있을 때만 빈으로
 * 등록된다. 키가 없으면 {@link MockFareCollector} 가 그대로 사용된다.
 *
 * <p>SerpApi 호출 1회로 best_flights + other_flights 의 최저가를 스냅샷으로 저장한다.
 * {@code price_insights} (Google 자체 가격 분석) 는 {@code rawData} JSONB 에 보관해서
 * 향후 판단 엔진 보완에 활용할 수 있다.
 *
 * <p><b>무료 플랜 (250호출/월)</b>: 스케줄러가 12시간 주기 × 2노선 × 2출발일 =
 * 8호출/일 ≈ 240호출/월로 운영하면 범위 안에 들어온다.
 */
@Component
@ConditionalOnProperty("farewatch.collector.serpapi.api-key")
public class SerpApiFareCollector implements FareCollector {

    public static final String SOURCE_NAME = "SERPAPI_GOOGLE_FLIGHTS";

    private static final Logger log = LoggerFactory.getLogger(SerpApiFareCollector.class);
    private static final String BASE_URL = "https://serpapi.com/search.json";
    private static final Duration TIMEOUT = Duration.ofSeconds(30);

    private final String apiKey;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public SerpApiFareCollector(
            @Value("${farewatch.collector.serpapi.api-key}") String apiKey,
            ObjectMapper objectMapper) {
        this.apiKey = Objects.requireNonNull(apiKey, "SerpApi API key must not be null");
        this.objectMapper = objectMapper;
        this.httpClient =
                HttpClient.newBuilder()
                        .connectTimeout(TIMEOUT)
                        .followRedirects(HttpClient.Redirect.NORMAL)
                        .build();
    }

    @Override
    public List<FareSnapshot> fetchFares(Route route, LocalDate targetDate) {
        Objects.requireNonNull(route, "route must not be null");
        Objects.requireNonNull(targetDate, "targetDate must not be null");
        if (route.getId() == null) {
            throw new IllegalArgumentException("route id must not be null");
        }

        try {
            String url = buildUrl(route, targetDate);
            HttpRequest request =
                    HttpRequest.newBuilder()
                            .uri(URI.create(url))
                            .timeout(TIMEOUT)
                            .GET()
                            .build();

            HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.warn(
                        "SerpApi returned status {} for {} → {} on {}",
                        response.statusCode(),
                        route.getOrigin().value(),
                        route.getDestination().value(),
                        targetDate);
                return List.of();
            }

            return parseResponse(response.body(), route.getId(), targetDate);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("SerpApi call interrupted for route {}", route.getId());
            return List.of();
        } catch (Exception e) {
            log.warn(
                    "SerpApi call failed for {} → {} on {}: {}",
                    route.getOrigin().value(),
                    route.getDestination().value(),
                    targetDate,
                    e.getMessage());
            return List.of();
        }
    }

    @Override
    public String sourceName() {
        return SOURCE_NAME;
    }

    private String buildUrl(Route route, LocalDate targetDate) {
        return BASE_URL
                + "?engine=google_flights"
                + "&departure_id=" + route.getOrigin().value()
                + "&arrival_id=" + route.getDestination().value()
                + "&outbound_date=" + targetDate
                + "&type=2"      // one-way
                + "&currency=KRW"
                + "&hl=ko"
                + "&gl=kr"
                + "&adults=1"
                + "&api_key=" + apiKey;
    }

    List<FareSnapshot> parseResponse(String json, Long routeId, LocalDate targetDate)
            throws Exception {
        JsonNode root = objectMapper.readTree(json);

        List<FareSnapshot> snapshots = new ArrayList<>();

        // best_flights 에서 최저가 수집
        addFlights(root.path("best_flights"), routeId, targetDate, snapshots);
        // other_flights 에서도 수집
        addFlights(root.path("other_flights"), routeId, targetDate, snapshots);

        if (snapshots.isEmpty()) {
            log.info("SerpApi returned no flights for routeId={}, date={}", routeId, targetDate);
            return List.of();
        }

        // 최저가 1건만 반환 — 스냅샷은 "그 시점의 최저가" 를 기록하는 것이 목적.
        // 모든 항공편을 저장하면 DB가 빠르게 비대해진다.
        FareSnapshot cheapest =
                snapshots.stream()
                        .min((a, b) -> Long.compare(a.getPrice().amount(), b.getPrice().amount()))
                        .orElseThrow();

        // rawData 에 price_insights 포함 (Google 자체 가격 분석)
        Map<String, Object> rawData = new LinkedHashMap<>();
        rawData.put("source", SOURCE_NAME);
        rawData.put("airline", extractAirline(cheapest, root));
        JsonNode insights = root.path("price_insights");
        if (!insights.isMissingNode()) {
            rawData.put("google_price_level", insights.path("price_level").asText(null));
            rawData.put("google_lowest_price", insights.path("lowest_price").asInt(0));
            JsonNode typical = insights.path("typical_price_range");
            if (typical.isArray() && typical.size() == 2) {
                rawData.put("google_typical_low", typical.get(0).asInt());
                rawData.put("google_typical_high", typical.get(1).asInt());
            }
        }

        // rawData 가 채워진 새 스냅샷을 반환
        FareSnapshot result =
                FareSnapshot.record(
                        routeId,
                        targetDate,
                        cheapest.getPrice(),
                        SOURCE_NAME,
                        rawData);
        return List.of(result);
    }

    private void addFlights(
            JsonNode flightsArray,
            Long routeId,
            LocalDate targetDate,
            List<FareSnapshot> out) {
        if (flightsArray.isMissingNode() || !flightsArray.isArray()) {
            return;
        }
        for (JsonNode offer : flightsArray) {
            int price = offer.path("price").asInt(0);
            if (price <= 0) {
                continue;
            }
            out.add(
                    FareSnapshot.record(
                            routeId,
                            targetDate,
                            Money.krw(price),
                            SOURCE_NAME,
                            null));
        }
    }

    private String extractAirline(FareSnapshot cheapest, JsonNode root) {
        // best_flights 에서 가격이 매칭되는 첫 항공편의 항공사명 추출
        for (String section : List.of("best_flights", "other_flights")) {
            JsonNode arr = root.path(section);
            if (!arr.isArray()) {
                continue;
            }
            for (JsonNode offer : arr) {
                if (offer.path("price").asInt(0) == (int) cheapest.getPrice().amount()) {
                    JsonNode flights = offer.path("flights");
                    if (flights.isArray() && !flights.isEmpty()) {
                        return flights.get(0).path("airline").asText("unknown");
                    }
                }
            }
        }
        return "unknown";
    }
}
