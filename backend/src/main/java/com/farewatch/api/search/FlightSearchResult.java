package com.farewatch.api.search;

import java.util.List;

/**
 * 실시간 항공편 검색 결과.
 */
public record FlightSearchResult(
        String origin,
        String destination,
        String departureDate,
        String returnDate,
        String tripType,
        List<FlightOffer> flights,
        PriceInsights priceInsights) {

    public record FlightOffer(
            String airline,
            String airlineCode,
            long price,
            String currency,
            String departureTime,
            String arrivalTime,
            String duration,
            int stops,
            boolean isBest) {}

    public record PriceInsights(
            Long lowestPrice,
            String priceLevel,
            Long typicalPriceLow,
            Long typicalPriceHigh) {}
}
