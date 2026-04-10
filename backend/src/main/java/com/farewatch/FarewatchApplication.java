package com.farewatch;

import com.farewatch.application.judgment.FareVerdictCalculator;
import java.time.Clock;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class FarewatchApplication {

    public static void main(String[] args) {
        SpringApplication.run(FarewatchApplication.class, args);
    }

    /**
     * 시스템 Clock. 스케줄러·도메인 로직에서 "지금" 을 주입받아 사용하기 위한 기본 빈.
     * 테스트에서는 {@code Clock.fixed(...)} 로 교체할 수 있다.
     */
    @Bean
    public Clock systemClock() {
        return Clock.systemDefaultZone();
    }

    /**
     * Rule Engine 빈. 기본 임계값 (sample 30, cheap 1.0σ, expensive 0.5σ) 으로 등록한다.
     * 향후 {@code @ConfigurationProperties} 로 외부화 가능.
     */
    @Bean
    public FareVerdictCalculator fareVerdictCalculator() {
        return new FareVerdictCalculator();
    }
}
