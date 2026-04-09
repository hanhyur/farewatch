package com.farewatch.infrastructure.collector;

import com.farewatch.application.collector.FareCollector;
import com.farewatch.domain.fare.FareSnapshot;
import com.farewatch.domain.route.Route;
import com.farewatch.domain.shared.Money;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import org.springframework.stereotype.Component;

/**
 * 결정론적 {@link FareCollector} 구현체. 외부 의존성 없이 {@code (routeId, departureDate)}
 * 쌍에 대해 항상 동일한 가격을 반환한다.
 *
 * <p><b>왜 결정론인가</b>: 실제 크롤러 구현체가 준비되기 전까지 통계·판단 파이프라인을 개발
 * 하려면 예측 가능한 데이터가 필요하다. 같은 입력에 같은 출력이 나와야 테스트가 안정적이고,
 * 로컬 개발 중에 사용자가 직접 JSON 응답을 눈으로 검증할 수 있다.
 *
 * <p><b>가격 생성 공식</b>: seed = hash(routeId, departureDate.toEpochDay()) 로
 * {@link Random} 을 초기화한 뒤 {@code [MIN_PRICE, MAX_PRICE]} 범위에서 한 번 뽑는다.
 * 여기에 요일별 소규모 가중치를 덧붙여 주말 가격이 약간 높게 나오도록 한다 — 현실감을
 * 아주 약간 부여.
 *
 * <p><b>thread-safety</b>: 상태가 없으므로 스레드 안전. 각 호출마다 새로운
 * {@link Random} 인스턴스를 생성한다.
 */
@Component
public class MockFareCollector implements FareCollector {

    /** 수집기 식별자. {@code FareSnapshot.source} 컬럼에 기록된다. */
    public static final String SOURCE_NAME = "MOCK";

    static final long MIN_PRICE = 80_000L;
    static final long MAX_PRICE = 400_000L;
    static final long WEEKEND_SURCHARGE = 25_000L;

    @Override
    public List<FareSnapshot> fetchFares(Route route, LocalDate targetDate) {
        Objects.requireNonNull(route, "route must not be null");
        Objects.requireNonNull(targetDate, "targetDate must not be null");
        if (route.getId() == null) {
            throw new IllegalArgumentException(
                    "route id must not be null — only persisted routes can be collected");
        }

        long price = generatePrice(route.getId(), targetDate);
        FareSnapshot snapshot =
                FareSnapshot.record(
                        route.getId(),
                        targetDate,
                        Money.krw(price),
                        SOURCE_NAME,
                        Map.of("generator", "mock", "seedVersion", "v1"));
        return List.of(snapshot);
    }

    @Override
    public String sourceName() {
        return SOURCE_NAME;
    }

    private static long generatePrice(long routeId, LocalDate departureDate) {
        long seed = 31L * routeId + departureDate.toEpochDay();
        Random random = new Random(seed);

        // base draw in [MIN_PRICE, MAX_PRICE]
        long range = MAX_PRICE - MIN_PRICE;
        // drop the surcharge budget from the base draw so the total still fits in the band
        long basePrice = MIN_PRICE + (long) (random.nextDouble() * (range - WEEKEND_SURCHARGE));

        // weekend bump: Sat(6) / Sun(7) get a fixed surcharge
        int dow = departureDate.getDayOfWeek().getValue();
        long surcharge = (dow == 6 || dow == 7) ? WEEKEND_SURCHARGE : 0L;

        long price = basePrice + surcharge;
        // invariant guard — base formula is bounded, but keep a defensive clamp
        if (price < MIN_PRICE) {
            return MIN_PRICE;
        }
        if (price > MAX_PRICE) {
            return MAX_PRICE;
        }
        return price;
    }
}
