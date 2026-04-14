package com.farewatch.application.search;

import static org.assertj.core.api.Assertions.assertThat;

import com.farewatch.api.search.FlightSearchResult;
import com.farewatch.api.search.SearchRequest;
import com.farewatch.api.search.SearchRequest.TripType;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MockFlightSearchServiceTest {

    private final MockFlightSearchService service = new MockFlightSearchService();

    private SearchRequest oneWay(String origin, String dest, LocalDate date) {
        return new SearchRequest(origin, dest, date, null, TripType.ONE_WAY, 0);
    }

    @Test
    @DisplayName("편도 검색 결과에 항공편이 3개 이상 포함된다")
    void search_returnsMultipleFlights() {
        FlightSearchResult result = service.search(
                oneWay("PUS", "NRT", LocalDate.now().plusDays(7)));

        assertThat(result.flights()).hasSizeGreaterThanOrEqualTo(3);
        assertThat(result.origin()).isEqualTo("PUS");
        assertThat(result.destination()).isEqualTo("NRT");
        assertThat(result.tripType()).isEqualTo("ONE_WAY");
        assertThat(result.returnDate()).isNull();
    }

    @Test
    @DisplayName("결과는 가격순으로 정렬된다")
    void search_sortedByPrice() {
        FlightSearchResult result = service.search(
                oneWay("PUS", "NRT", LocalDate.now().plusDays(7)));

        for (int i = 1; i < result.flights().size(); i++) {
            assertThat(result.flights().get(i).price())
                    .isGreaterThanOrEqualTo(result.flights().get(i - 1).price());
        }
    }

    @Test
    @DisplayName("동일 입력은 동일 결과를 반환한다 (결정론적)")
    void search_deterministic() {
        LocalDate date = LocalDate.of(2026, 5, 1);
        FlightSearchResult r1 = service.search(oneWay("PUS", "NRT", date));
        FlightSearchResult r2 = service.search(oneWay("PUS", "NRT", date));

        assertThat(r1.flights()).hasSize(r2.flights().size());
        for (int i = 0; i < r1.flights().size(); i++) {
            assertThat(r1.flights().get(i).price()).isEqualTo(r2.flights().get(i).price());
        }
    }

    @Test
    @DisplayName("가격 인사이트가 포함된다")
    void search_includesPriceInsights() {
        FlightSearchResult result = service.search(
                oneWay("PUS", "HND", LocalDate.now().plusDays(14)));

        assertThat(result.priceInsights()).isNotNull();
        assertThat(result.priceInsights().lowestPrice()).isPositive();
        assertThat(result.priceInsights().priceLevel()).isNotBlank();
    }

    @Test
    @DisplayName("다른 노선은 다른 결과를 반환한다")
    void search_differentRoutesProduceDifferentResults() {
        LocalDate date = LocalDate.of(2026, 5, 1);
        FlightSearchResult r1 = service.search(oneWay("PUS", "NRT", date));
        FlightSearchResult r2 = service.search(oneWay("ICN", "FUK", date));

        assertThat(r1.flights().getFirst().price())
                .isNotEqualTo(r2.flights().getFirst().price());
    }

    @Test
    @DisplayName("왕복 검색 시 가격이 편도보다 높다")
    void search_roundTripPriceHigherThanOneWay() {
        LocalDate depDate = LocalDate.of(2026, 5, 1);
        LocalDate retDate = LocalDate.of(2026, 5, 8);

        FlightSearchResult oneWayResult = service.search(
                oneWay("PUS", "NRT", depDate));
        FlightSearchResult roundTripResult = service.search(
                new SearchRequest("PUS", "NRT", depDate, retDate, TripType.ROUND_TRIP, 0));

        assertThat(roundTripResult.tripType()).isEqualTo("ROUND_TRIP");
        assertThat(roundTripResult.returnDate()).isEqualTo(retDate.toString());

        long oneWayMin = oneWayResult.flights().getFirst().price();
        long roundTripMin = roundTripResult.flights().getFirst().price();
        assertThat(roundTripMin).isGreaterThan(oneWayMin);
    }

    @Test
    @DisplayName("직항 필터 적용 시 경유 항공편이 없다")
    void search_directOnly_noStops() {
        FlightSearchResult result = service.search(
                new SearchRequest("PUS", "NRT", LocalDate.of(2026, 5, 1),
                        null, TripType.ONE_WAY, 1));

        assertThat(result.flights()).allMatch(f -> f.stops() == 0);
    }
}
