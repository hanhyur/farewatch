package com.farewatch.application.search;

import com.farewatch.api.search.FlightSearchResult;
import com.farewatch.api.search.FlightSearchResult.FlightOffer;
import com.farewatch.api.search.FlightSearchResult.PriceInsights;
import com.farewatch.api.search.SearchRequest;
import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Service;

/**
 * SerpApi 키가 없을 때 사용되는 Mock 검색 서비스.
 * 결정론적 가짜 항공편 데이터를 반환한다.
 */
@Service
@ConditionalOnMissingBean(FlightSearchService.class)
public class MockFlightSearchService {

    private static final String[][] AIRLINES = {
            {"대한항공", "KE"},
            {"아시아나항공", "OZ"},
            {"진에어", "LJ"},
            {"제주항공", "7C"},
            {"티웨이항공", "TW"}
    };

    public FlightSearchResult search(SearchRequest req) {
        long seed = req.origin().hashCode() * 31L + req.destination().hashCode() * 17L
                + req.departureDate().toEpochDay();
        Random rng = new Random(seed);

        boolean isWeekend = req.departureDate().getDayOfWeek() == DayOfWeek.SATURDAY
                || req.departureDate().getDayOfWeek() == DayOfWeek.SUNDAY;
        int weekendSurcharge = isWeekend ? 25_000 : 0;

        // 왕복이면 가격 1.6~1.9배
        boolean roundTrip = req.tripType() == SearchRequest.TripType.ROUND_TRIP;
        double tripMultiplier = roundTrip ? 1.6 + rng.nextDouble() * 0.3 : 1.0;

        List<FlightOffer> offers = new ArrayList<>();
        int count = 3 + rng.nextInt(4); // 3~6개
        for (int i = 0; i < count; i++) {
            String[] airline = AIRLINES[rng.nextInt(AIRLINES.length)];
            long basePrice = 100_000 + rng.nextInt(200_000) + weekendSurcharge;
            long price = (long) (basePrice * tripMultiplier);
            int hour = 6 + rng.nextInt(14);
            int minute = rng.nextInt(4) * 15;
            int durationMin = 120 + rng.nextInt(90);
            int arrHour = hour + durationMin / 60;
            int arrMinute = minute + durationMin % 60;
            if (arrMinute >= 60) {
                arrHour++;
                arrMinute -= 60;
            }

            // stops 필터 적용: 직항만(1)이면 경유 생성 안 함
            int stops = 0;
            if (req.stops() == 0 && rng.nextInt(5) == 0) {
                stops = 1; // 20% 확률로 경유
            }
            if (req.stops() == 1) {
                stops = 0; // 직항만
            }

            offers.add(new FlightOffer(
                    airline[0],
                    airline[1],
                    price,
                    "KRW",
                    String.format("%02d:%02d", hour, minute),
                    String.format("%02d:%02d", arrHour % 24, arrMinute),
                    (durationMin / 60) + "h " + (durationMin % 60) + "m",
                    stops,
                    i < 2));
        }

        offers.sort((a, b) -> Long.compare(a.price(), b.price()));

        long minPrice = offers.getFirst().price();
        long avgPrice = (long) offers.stream().mapToLong(FlightOffer::price).average().orElse(0);
        PriceInsights insights = new PriceInsights(
                minPrice, minPrice < avgPrice * 0.85 ? "low" : "typical",
                (long) (avgPrice * 0.8), (long) (avgPrice * 1.2));

        return new FlightSearchResult(
                req.origin(),
                req.destination(),
                req.departureDate().toString(),
                req.returnDate() != null ? req.returnDate().toString() : null,
                req.tripType().name(),
                offers,
                insights);
    }
}
