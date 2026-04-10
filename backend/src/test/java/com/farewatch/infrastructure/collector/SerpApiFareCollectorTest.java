package com.farewatch.infrastructure.collector;

import static org.assertj.core.api.Assertions.assertThat;

import com.farewatch.domain.fare.FareSnapshot;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SerpApiFareCollectorTest {

    private SerpApiFareCollector collector;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        collector = new SerpApiFareCollector("test-key", objectMapper);
    }

    @Test
    @DisplayName("parseResponse: extracts cheapest fare from best_flights + other_flights")
    void parseResponse_extractsCheapest() throws Exception {
        String json =
                """
                {
                  "best_flights": [
                    {"price": 180000, "flights": [{"airline": "대한항공", "flight_number": "KE 789"}]},
                    {"price": 150000, "flights": [{"airline": "진에어", "flight_number": "LJ 201"}]}
                  ],
                  "other_flights": [
                    {"price": 200000, "flights": [{"airline": "아시아나", "flight_number": "OZ 101"}]}
                  ],
                  "price_insights": {
                    "lowest_price": 150000,
                    "price_level": "low",
                    "typical_price_range": [120000, 250000]
                  }
                }
                """;

        List<FareSnapshot> snapshots = collector.parseResponse(json, 1L, LocalDate.of(2026, 5, 10));

        assertThat(snapshots).hasSize(1);
        FareSnapshot snap = snapshots.get(0);
        assertThat(snap.getPrice().amount()).isEqualTo(150_000L);
        assertThat(snap.getPrice().currency()).isEqualTo("KRW");
        assertThat(snap.getSource()).isEqualTo("SERPAPI_GOOGLE_FLIGHTS");
        assertThat(snap.getRouteId()).isEqualTo(1L);
        assertThat(snap.getDepartureDate()).isEqualTo(LocalDate.of(2026, 5, 10));
    }

    @Test
    @DisplayName("parseResponse: rawData contains Google price insights")
    void parseResponse_containsPriceInsights() throws Exception {
        String json =
                """
                {
                  "best_flights": [
                    {"price": 122600, "flights": [{"airline": "제주항공"}]}
                  ],
                  "price_insights": {
                    "lowest_price": 122600,
                    "price_level": "typical",
                    "typical_price_range": [89000, 195000]
                  }
                }
                """;

        FareSnapshot snap =
                collector.parseResponse(json, 1L, LocalDate.of(2026, 5, 10)).get(0);

        assertThat(snap.getRawData()).containsEntry("google_price_level", "typical");
        assertThat(snap.getRawData()).containsEntry("google_lowest_price", 122600);
        assertThat(snap.getRawData()).containsEntry("google_typical_low", 89000);
        assertThat(snap.getRawData()).containsEntry("google_typical_high", 195000);
        assertThat(snap.getRawData()).containsEntry("airline", "제주항공");
    }

    @Test
    @DisplayName("parseResponse: no flights returns empty list")
    void parseResponse_noFlights_returnsEmpty() throws Exception {
        String json = """
                {"best_flights": [], "other_flights": []}
                """;

        List<FareSnapshot> snapshots = collector.parseResponse(json, 1L, LocalDate.of(2026, 5, 10));

        assertThat(snapshots).isEmpty();
    }

    @Test
    @DisplayName("parseResponse: missing price_insights still works")
    void parseResponse_missingInsights_stillWorks() throws Exception {
        String json =
                """
                {
                  "best_flights": [
                    {"price": 200000, "flights": [{"airline": "KE"}]}
                  ]
                }
                """;

        List<FareSnapshot> snapshots = collector.parseResponse(json, 1L, LocalDate.of(2026, 5, 10));

        assertThat(snapshots).hasSize(1);
        assertThat(snapshots.get(0).getPrice().amount()).isEqualTo(200_000L);
    }

    @Test
    @DisplayName("sourceName returns SERPAPI_GOOGLE_FLIGHTS")
    void sourceName() {
        assertThat(collector.sourceName()).isEqualTo("SERPAPI_GOOGLE_FLIGHTS");
    }
}
