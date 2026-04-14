package com.farewatch.api.airport;

/**
 * 공항 정보. 검색/자동완성 응답용.
 *
 * @param iata     IATA 공항 코드
 * @param name     공항명 (한국어)
 * @param nameEn   공항명 (영어)
 * @param city     도시명 (한국어)
 * @param cityEn   도시명 (영어)
 * @param country  국가명 (한국어)
 */
public record AirportInfo(
        String iata,
        String name,
        String nameEn,
        String city,
        String cityEn,
        String country) {}
