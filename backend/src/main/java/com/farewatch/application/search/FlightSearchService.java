package com.farewatch.application.search;

import com.farewatch.api.search.FlightSearchResult;
import com.farewatch.api.search.FlightSearchResult.FlightOffer;
import com.farewatch.api.search.FlightSearchResult.PriceInsights;
import com.farewatch.api.search.SearchRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * 실시간 항공편 검색 서비스. SerpApi Google Flights를 호출하여 전체 항공편 목록을 반환.
 *
 * <p>동일 검색 조건에 대해 1시간 인메모리 캐시를 적용하여 SerpApi 쿼터를 절약한다.
 */
@Service
@ConditionalOnProperty("farewatch.collector.serpapi.api-key")
public class FlightSearchService {

    private static final Logger log = LoggerFactory.getLogger(FlightSearchService.class);
    private static final String BASE_URL = "https://serpapi.com/search.json";
    private static final Duration TIMEOUT = Duration.ofSeconds(30);
    private static final long CACHE_TTL_MS = 3_600_000; // 1시간

    private final String apiKey;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final Map<String, CachedResult> cache = new ConcurrentHashMap<>();

    public FlightSearchService(
            @Value("${farewatch.collector.serpapi.api-key}") String apiKey,
            ObjectMapper objectMapper) {
        this.apiKey = Objects.requireNonNull(apiKey);
        this.objectMapper = objectMapper;
        this.httpClient =
                HttpClient.newBuilder()
                        .connectTimeout(TIMEOUT)
                        .followRedirects(HttpClient.Redirect.NORMAL)
                        .build();
    }

    public FlightSearchResult search(SearchRequest req) {
        String cacheKey = buildCacheKey(req);

        CachedResult cached = cache.get(cacheKey);
        if (cached != null && !cached.isExpired()) {
            log.debug("Cache hit for {}", cacheKey);
            return cached.result();
        }

        try {
            String url = buildUrl(req);
            HttpRequest request =
                    HttpRequest.newBuilder()
                            .uri(URI.create(url))
                            .timeout(TIMEOUT)
                            .GET()
                            .build();

            HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.warn("SerpApi search returned status {} for {} → {} on {}",
                        response.statusCode(), req.origin(), req.destination(),
                        req.departureDate());
                return emptyResult(req);
            }

            FlightSearchResult result = parseResponse(response.body(), req);
            cache.put(cacheKey, new CachedResult(result, System.currentTimeMillis()));
            evictExpired();
            return result;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("SerpApi search interrupted for {} → {}", req.origin(), req.destination());
            return emptyResult(req);
        } catch (Exception e) {
            log.warn("SerpApi search failed for {} → {} on {}: {}",
                    req.origin(), req.destination(), req.departureDate(), e.getMessage());
            return emptyResult(req);
        }
    }

    FlightSearchResult parseResponse(String json, SearchRequest req) throws Exception {
        JsonNode root = objectMapper.readTree(json);

        List<FlightOffer> offers = new ArrayList<>();
        addOffers(root.path("best_flights"), offers, true);
        addOffers(root.path("other_flights"), offers, false);

        PriceInsights insights = parsePriceInsights(root.path("price_insights"));

        return new FlightSearchResult(
                req.origin(),
                req.destination(),
                req.departureDate().toString(),
                req.returnDate() != null ? req.returnDate().toString() : null,
                req.tripType().name(),
                offers,
                insights);
    }

    private void addOffers(JsonNode flightsArray, List<FlightOffer> out, boolean isBest) {
        if (flightsArray.isMissingNode() || !flightsArray.isArray()) {
            return;
        }
        for (JsonNode offer : flightsArray) {
            int price = offer.path("price").asInt(0);
            if (price <= 0) {
                continue;
            }

            JsonNode flights = offer.path("flights");
            String airline = "Unknown";
            String airlineCode = "";
            String departureTime = "";
            String arrivalTime = "";
            if (flights.isArray() && !flights.isEmpty()) {
                JsonNode first = flights.get(0);
                airline = first.path("airline").asText("Unknown");
                airlineCode = first.path("airline_logo").asText("");

                departureTime = first.path("departure_airport").path("time").asText("");
                JsonNode last = flights.get(flights.size() - 1);
                arrivalTime = last.path("arrival_airport").path("time").asText("");
            }

            int totalDuration = offer.path("total_duration").asInt(0);
            String duration = formatDuration(totalDuration);
            int stops = flights.isArray() ? flights.size() - 1 : 0;

            out.add(new FlightOffer(
                    airline, airlineCode, price, "KRW",
                    departureTime, arrivalTime, duration, stops, isBest));
        }
    }

    private PriceInsights parsePriceInsights(JsonNode insights) {
        if (insights.isMissingNode()) {
            return null;
        }
        Long lowest = insights.has("lowest_price")
                ? (long) insights.path("lowest_price").asInt(0) : null;
        String level = insights.path("price_level").asText(null);
        Long typLow = null;
        Long typHigh = null;
        JsonNode typical = insights.path("typical_price_range");
        if (typical.isArray() && typical.size() == 2) {
            typLow = (long) typical.get(0).asInt();
            typHigh = (long) typical.get(1).asInt();
        }
        return new PriceInsights(lowest, level, typLow, typHigh);
    }

    private String buildUrl(SearchRequest req) {
        StringBuilder sb = new StringBuilder(BASE_URL)
                .append("?engine=google_flights")
                .append("&departure_id=").append(req.origin())
                .append("&arrival_id=").append(req.destination())
                .append("&outbound_date=").append(req.departureDate())
                .append("&type=").append(req.tripType().serpApiValue())
                .append("&currency=KRW")
                .append("&hl=ko")
                .append("&gl=kr")
                .append("&adults=1")
                .append("&api_key=").append(apiKey);

        if (req.returnDate() != null) {
            sb.append("&return_date=").append(req.returnDate());
        }
        if (req.stops() > 0) {
            sb.append("&stops=").append(req.stops());
        }
        return sb.toString();
    }

    private String buildCacheKey(SearchRequest req) {
        return req.origin() + "-" + req.destination()
                + "-" + req.departureDate()
                + "-" + (req.returnDate() != null ? req.returnDate() : "OW")
                + "-" + req.tripType()
                + "-s" + req.stops();
    }

    private static String formatDuration(int totalMinutes) {
        if (totalMinutes <= 0) {
            return "";
        }
        int hours = totalMinutes / 60;
        int minutes = totalMinutes % 60;
        return hours + "h " + minutes + "m";
    }

    private FlightSearchResult emptyResult(SearchRequest req) {
        return new FlightSearchResult(
                req.origin(),
                req.destination(),
                req.departureDate().toString(),
                req.returnDate() != null ? req.returnDate().toString() : null,
                req.tripType().name(),
                List.of(),
                null);
    }

    private void evictExpired() {
        cache.entrySet().removeIf(e -> e.getValue().isExpired());
    }

    private record CachedResult(FlightSearchResult result, long createdAtMs) {
        boolean isExpired() {
            return System.currentTimeMillis() - createdAtMs > CACHE_TTL_MS;
        }
    }
}
