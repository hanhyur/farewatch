package com.farewatch.api.search;

import java.time.LocalDate;

/**
 * 항공편 검색 요청 파라미터.
 *
 * @param origin         출발 공항 IATA 코드 (3글자)
 * @param destination    도착 공항 IATA 코드 (3글자)
 * @param departureDate  출발일
 * @param returnDate     귀국일 (왕복인 경우 필수)
 * @param tripType       ROUND_TRIP / ONE_WAY (기본: ONE_WAY)
 * @param stops          직항/경유 필터: 0=모두, 1=직항만, 2=경유1회이하, 3=경유2회이하 (기본: 0)
 */
public record SearchRequest(
        String origin,
        String destination,
        LocalDate departureDate,
        LocalDate returnDate,
        TripType tripType,
        int stops) {

    public enum TripType {
        ONE_WAY(2),
        ROUND_TRIP(1);

        private final int serpApiValue;

        TripType(int serpApiValue) {
            this.serpApiValue = serpApiValue;
        }

        public int serpApiValue() {
            return serpApiValue;
        }
    }

    /**
     * SerpApi stops 파라미터 값. 0이면 파라미터 자체를 보내지 않는다.
     */
    public int serpApiStops() {
        return stops;
    }
}
