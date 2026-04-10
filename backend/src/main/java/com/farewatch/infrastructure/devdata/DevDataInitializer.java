package com.farewatch.infrastructure.devdata;

import com.farewatch.application.analyzer.StatisticsCalculator;
import com.farewatch.domain.fare.FareSnapshot;
import com.farewatch.domain.fare.FareSnapshotRepository;
import com.farewatch.domain.fare.FareStatistics;
import com.farewatch.domain.fare.FareStatisticsRepository;
import com.farewatch.domain.route.Route;
import com.farewatch.domain.route.RouteRepository;
import com.farewatch.domain.shared.AirportCode;
import com.farewatch.domain.shared.Money;
import jakarta.annotation.PostConstruct;
import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 로컬 개발 ({@code dev} 프로파일) 전용 시드 데이터 부트스트래퍼.
 *
 * <p><b>왜 필요한가</b>: 프로덕션 흐름에서는 스케줄러가 6시간마다 1건씩 수집해서 통계
 * 판정에 필요한 30개 표본이 쌓이려면 며칠이 걸린다. 프론트엔드 개발 중에는 즉시 의미 있는
 * 화면이 필요하므로, 앱 시작 시 과거 데이터를 한 번에 백필한다.
 *
 * <p><b>생성 데이터</b>:
 *
 * <ul>
 *   <li>노선 2개: PUS→NRT, PUS→HND
 *   <li>각 노선마다 출발일 5개 (오늘 + 7, 14, 21, 28, 35일)
 *   <li>각 (노선, 출발일) 당 스냅샷 40개 — 통계 30개 임계값을 충분히 넘김
 *   <li>스냅샷 가격은 결정론적 노이즈로 평균 ± stdDev 분포 형성
 *   <li>{@code collectedAt} 은 과거 30일에 균일 분포 — 차트가 자연스럽게 보임
 *   <li>각 (노선, 출발일) 의 {@link FareStatistics} 즉시 계산해서 저장
 * </ul>
 *
 * <p><b>idempotent</b>: 이미 노선이 존재하면 아무 것도 하지 않는다 (앱 재기동 시 중복
 * 시드 방지). DB 를 비우려면 H2 console 에서 truncate 하거나 in-memory 모드 재시작.
 *
 * <p><b>reflection 사용</b>: {@link FareSnapshot} 은 {@code collectedAt} 을 {@code now()}
 * 로 강제하는 정적 팩토리만 노출한다. 프로덕션 코드에 dev-only 우회 메서드를 만들지
 * 않으려고 reflection 으로 직접 주입한다 — 이 클래스는 dev 프로파일에서만 로드되므로
 * 운영 빌드에는 영향이 없다.
 */
@Component
@Profile("dev")
public class DevDataInitializer {

    private static final Logger log = LoggerFactory.getLogger(DevDataInitializer.class);

    private static final int SNAPSHOTS_PER_KEY = 40;
    private static final int HISTORY_DAYS = 30;
    private static final int[] DEPARTURE_OFFSETS = {7, 14, 21, 28, 35};

    private final RouteRepository routeRepository;
    private final FareSnapshotRepository snapshotRepository;
    private final FareStatisticsRepository statisticsRepository;
    private final StatisticsCalculator calculator;

    public DevDataInitializer(
            RouteRepository routeRepository,
            FareSnapshotRepository snapshotRepository,
            FareStatisticsRepository statisticsRepository,
            StatisticsCalculator calculator) {
        this.routeRepository = routeRepository;
        this.snapshotRepository = snapshotRepository;
        this.statisticsRepository = statisticsRepository;
        this.calculator = calculator;
    }

    @PostConstruct
    @Transactional
    public void seed() {
        if (!routeRepository.findAll().isEmpty()) {
            log.info("DevDataInitializer: routes already present, skipping seed");
            return;
        }

        log.info("DevDataInitializer: seeding dev data...");
        List<Route> routes =
                List.of(
                        routeRepository.save(
                                Route.create(
                                        new AirportCode("PUS"), new AirportCode("NRT"), "KE")),
                        routeRepository.save(
                                Route.create(
                                        new AirportCode("PUS"), new AirportCode("HND"), "OZ")));

        LocalDate today = LocalDate.now();
        for (Route route : routes) {
            for (int offset : DEPARTURE_OFFSETS) {
                LocalDate departureDate = today.plusDays(offset);
                seedSnapshotsAndStats(route, departureDate, today);
            }
        }
        log.info(
                "DevDataInitializer: seeded {} routes, {} snapshots/key, {} departure dates each",
                routes.size(),
                SNAPSHOTS_PER_KEY,
                DEPARTURE_OFFSETS.length);
    }

    private void seedSnapshotsAndStats(Route route, LocalDate departureDate, LocalDate today) {
        // 결정론적 시드: (routeId, departureDate) 에 따라 평균 가격이 다르게 나오도록.
        long seed = 31L * route.getId() + departureDate.toEpochDay();
        Random random = new Random(seed);

        // 평균 가격을 18~25만원 범위에서 뽑고, 표준편차는 ~3만원으로 자연스러운 분포 생성.
        long centerPrice = 180_000L + (long) (random.nextDouble() * 70_000L);
        double stdDev = 30_000.0;

        List<Long> prices = new ArrayList<>(SNAPSHOTS_PER_KEY);
        for (int i = 0; i < SNAPSHOTS_PER_KEY; i++) {
            long price = (long) (centerPrice + random.nextGaussian() * stdDev);
            // 음수/0 방지 — Money 가 amount > 0 강제.
            if (price < 50_000L) {
                price = 50_000L;
            }
            if (price > 500_000L) {
                price = 500_000L;
            }
            prices.add(price);

            // collectedAt 을 과거 HISTORY_DAYS 일에 균일 분포.
            // 가장 오래된 것이 today - HISTORY_DAYS, 가장 최근이 today.
            int daysAgo = HISTORY_DAYS - (HISTORY_DAYS * i / SNAPSHOTS_PER_KEY);
            LocalDateTime collectedAt = today.minusDays(daysAgo).atTime(9, 0).plusMinutes(i * 7L);

            FareSnapshot snapshot =
                    FareSnapshot.record(
                            route.getId(), departureDate, Money.krw(price), "DEV_SEED", null);
            overrideCollectedAt(snapshot, collectedAt);
            snapshotRepository.save(snapshot);
        }

        StatisticsCalculator.Result r = calculator.compute(prices);
        FareStatistics stats =
                FareStatistics.compute(
                        route.getId(),
                        departureDate,
                        r.avgPrice(),
                        r.minPrice(),
                        r.maxPrice(),
                        r.stdDeviation(),
                        r.sampleCount(),
                        r.p25Price(),
                        r.p75Price());
        statisticsRepository.save(stats);
    }

    /**
     * dev 시드 전용: {@link FareSnapshot#collectedAt} 을 임의 과거 시점으로 설정해 차트
     * 분포를 자연스럽게 만든다. 운영 코드에는 setter 가 없다.
     */
    private static void overrideCollectedAt(FareSnapshot snapshot, LocalDateTime when) {
        try {
            Field f = FareSnapshot.class.getDeclaredField("collectedAt");
            f.setAccessible(true);
            f.set(snapshot, when);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(
                    "DevDataInitializer failed to set collectedAt via reflection", e);
        }
    }
}
