package com.farewatch.api.airport;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AirportRepositoryTest {

    @Test
    @DisplayName("한국어 도시명으로 검색")
    void search_koreanCity() {
        var results = AirportRepository.search("도쿄");
        assertThat(results).extracting(AirportInfo::iata).contains("NRT", "HND");
    }

    @Test
    @DisplayName("영어 도시명으로 검색")
    void search_englishCity() {
        var results = AirportRepository.search("tokyo");
        assertThat(results).extracting(AirportInfo::iata).contains("NRT", "HND");
    }

    @Test
    @DisplayName("IATA 코드로 검색")
    void search_iataCode() {
        var results = AirportRepository.search("NRT");
        assertThat(results).hasSize(1);
        assertThat(results.getFirst().city()).isEqualTo("도쿄");
    }

    @Test
    @DisplayName("국가명으로 검색")
    void search_country() {
        var results = AirportRepository.search("태국");
        assertThat(results).extracting(AirportInfo::iata).contains("BKK", "DMK", "CNX", "HKT");
    }

    @Test
    @DisplayName("빈 쿼리는 전체 반환")
    void search_empty_returnsAll() {
        var results = AirportRepository.search("");
        assertThat(results).hasSizeGreaterThan(50);
    }

    @Test
    @DisplayName("findByIata로 단건 조회")
    void findByIata() {
        var result = AirportRepository.findByIata("PUS");
        assertThat(result).isPresent();
        assertThat(result.get().city()).isEqualTo("부산");
    }
}
