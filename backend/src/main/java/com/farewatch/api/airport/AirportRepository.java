package com.farewatch.api.airport;

import java.util.List;
import java.util.Optional;

/**
 * 인메모리 공항 데이터 저장소. 한국 출발 기준 주요 공항.
 */
public final class AirportRepository {

    private AirportRepository() {}

    private static final List<AirportInfo> AIRPORTS = List.of(
            // === 한국 ===
            new AirportInfo("ICN", "인천국제공항", "Incheon International", "인천/서울", "Seoul", "한국"),
            new AirportInfo("GMP", "김포국제공항", "Gimpo International", "서울", "Seoul", "한국"),
            new AirportInfo("PUS", "김해국제공항", "Gimhae International", "부산", "Busan", "한국"),
            new AirportInfo("CJU", "제주국제공항", "Jeju International", "제주", "Jeju", "한국"),
            new AirportInfo("TAE", "대구국제공항", "Daegu International", "대구", "Daegu", "한국"),
            new AirportInfo("CJJ", "청주국제공항", "Cheongju International", "청주", "Cheongju", "한국"),
            new AirportInfo("MWX", "무안국제공항", "Muan International", "무안/광주", "Muan", "한국"),

            // === 일본 ===
            new AirportInfo("NRT", "나리타국제공항", "Narita International", "도쿄", "Tokyo", "일본"),
            new AirportInfo("HND", "하네다공항", "Haneda", "도쿄", "Tokyo", "일본"),
            new AirportInfo("KIX", "간사이국제공항", "Kansai International", "오사카", "Osaka", "일본"),
            new AirportInfo("ITM", "이타미공항", "Itami (Osaka)", "오사카", "Osaka", "일본"),
            new AirportInfo("FUK", "후쿠오카공항", "Fukuoka", "후쿠오카", "Fukuoka", "일본"),
            new AirportInfo("CTS", "신치토세공항", "New Chitose", "삿포로", "Sapporo", "일본"),
            new AirportInfo("NGO", "주부국제공항", "Chubu Centrair", "나고야", "Nagoya", "일본"),
            new AirportInfo("OKA", "나하공항", "Naha", "오키나와", "Okinawa", "일본"),

            // === 중국 ===
            new AirportInfo("PVG", "푸둥국제공항", "Shanghai Pudong", "상하이", "Shanghai", "중국"),
            new AirportInfo("SHA", "훙차오공항", "Shanghai Hongqiao", "상하이", "Shanghai", "중국"),
            new AirportInfo("PEK", "베이징수도공항", "Beijing Capital", "베이징", "Beijing", "중국"),
            new AirportInfo("PKX", "베이징다싱공항", "Beijing Daxing", "베이징", "Beijing", "중국"),
            new AirportInfo("HKG", "홍콩국제공항", "Hong Kong International", "홍콩", "Hong Kong", "중국"),
            new AirportInfo("CAN", "바이윈공항", "Guangzhou Baiyun", "광저우", "Guangzhou", "중국"),
            new AirportInfo("SZX", "선전바오안공항", "Shenzhen Bao'an", "선전", "Shenzhen", "중국"),
            new AirportInfo("CTU", "톈푸공항", "Chengdu Tianfu", "청두", "Chengdu", "중국"),
            new AirportInfo("TSN", "톈진빈하이공항", "Tianjin Binhai", "톈진", "Tianjin", "중국"),
            new AirportInfo("TAO", "칭다오자오둥공항", "Qingdao Jiaodong", "칭다오", "Qingdao", "중국"),
            new AirportInfo("DLC", "다롄저우수이쯔공항", "Dalian Zhoushuizi", "다롄", "Dalian", "중국"),

            // === 대만 ===
            new AirportInfo("TPE", "타오위안국제공항", "Taoyuan International", "타이베이", "Taipei", "대만"),
            new AirportInfo("TSA", "쑹산공항", "Taipei Songshan", "타이베이", "Taipei", "대만"),
            new AirportInfo("KHH", "가오슝국제공항", "Kaohsiung International", "가오슝", "Kaohsiung", "대만"),

            // === 동남아시아 ===
            new AirportInfo("BKK", "수완나품공항", "Suvarnabhumi", "방콕", "Bangkok", "태국"),
            new AirportInfo("DMK", "돈므앙공항", "Don Mueang", "방콕", "Bangkok", "태국"),
            new AirportInfo("CNX", "치앙마이공항", "Chiang Mai", "치앙마이", "Chiang Mai", "태국"),
            new AirportInfo("HKT", "푸켓공항", "Phuket", "푸켓", "Phuket", "태국"),
            new AirportInfo("SGN", "탄선녓공항", "Tan Son Nhat", "호치민", "Ho Chi Minh", "베트남"),
            new AirportInfo("HAN", "노이바이공항", "Noi Bai", "하노이", "Hanoi", "베트남"),
            new AirportInfo("DAD", "다낭공항", "Da Nang", "다낭", "Da Nang", "베트남"),
            new AirportInfo("CXR", "깜라인공항", "Cam Ranh", "나트랑", "Nha Trang", "베트남"),
            new AirportInfo("PQC", "푸꾸옥공항", "Phu Quoc", "푸꾸옥", "Phu Quoc", "베트남"),
            new AirportInfo("SIN", "창이공항", "Changi", "싱가포르", "Singapore", "싱가포르"),
            new AirportInfo("MNL", "니노이아키노공항", "Ninoy Aquino", "마닐라", "Manila", "필리핀"),
            new AirportInfo("CEB", "막탄세부공항", "Mactan-Cebu", "세부", "Cebu", "필리핀"),
            new AirportInfo("CRK", "클라크공항", "Clark", "클라크", "Clark", "필리핀"),
            new AirportInfo("KUL", "쿠알라룸푸르공항", "Kuala Lumpur International", "쿠알라룸푸르", "Kuala Lumpur", "말레이시아"),
            new AirportInfo("KOT", "코타키나발루공항", "Kota Kinabalu", "코타키나발루", "Kota Kinabalu", "말레이시아"),
            new AirportInfo("DPS", "응우라라이공항", "Ngurah Rai", "발리", "Bali", "인도네시아"),
            new AirportInfo("REP", "시엠립공항", "Siem Reap-Angkor", "시엠립", "Siem Reap", "캄보디아"),
            new AirportInfo("PNH", "프놈펜공항", "Phnom Penh", "프놈펜", "Phnom Penh", "캄보디아"),
            new AirportInfo("RGN", "양곤공항", "Yangon", "양곤", "Yangon", "미얀마"),
            new AirportInfo("VTE", "왓따이공항", "Wattay", "비엔티안", "Vientiane", "라오스"),

            // === 괌/사이판 ===
            new AirportInfo("GUM", "괌국제공항", "A.B. Won Pat International", "괌", "Guam", "괌"),
            new AirportInfo("SPN", "사이판공항", "Saipan International", "사이판", "Saipan", "사이판"),

            // === 미주 ===
            new AirportInfo("LAX", "로스앤젤레스공항", "Los Angeles International", "로스앤젤레스", "Los Angeles", "미국"),
            new AirportInfo("JFK", "존F케네디공항", "John F. Kennedy International", "뉴욕", "New York", "미국"),
            new AirportInfo("SFO", "샌프란시스코공항", "San Francisco International", "샌프란시스코", "San Francisco", "미국"),
            new AirportInfo("SEA", "시애틀타코마공항", "Seattle-Tacoma", "시애틀", "Seattle", "미국"),
            new AirportInfo("ORD", "오헤어공항", "O'Hare International", "시카고", "Chicago", "미국"),
            new AirportInfo("HNL", "호놀룰루공항", "Daniel K. Inouye", "호놀룰루", "Honolulu", "미국"),
            new AirportInfo("ATL", "애틀랜타공항", "Hartsfield-Jackson", "애틀랜타", "Atlanta", "미국"),
            new AirportInfo("DFW", "댈러스공항", "Dallas/Fort Worth", "댈러스", "Dallas", "미국"),
            new AirportInfo("YVR", "밴쿠버공항", "Vancouver International", "밴쿠버", "Vancouver", "캐나다"),
            new AirportInfo("YYZ", "토론토피어슨공항", "Toronto Pearson", "토론토", "Toronto", "캐나다"),

            // === 유럽 ===
            new AirportInfo("CDG", "샤를드골공항", "Charles de Gaulle", "파리", "Paris", "프랑스"),
            new AirportInfo("LHR", "히드로공항", "Heathrow", "런던", "London", "영국"),
            new AirportInfo("FRA", "프랑크푸르트공항", "Frankfurt am Main", "프랑크푸르트", "Frankfurt", "독일"),
            new AirportInfo("FCO", "피우미치노공항", "Fiumicino", "로마", "Rome", "이탈리아"),
            new AirportInfo("BCN", "엘프라트공항", "Barcelona–El Prat", "바르셀로나", "Barcelona", "스페인"),
            new AirportInfo("MAD", "마드리드바라하스공항", "Adolfo Suárez Madrid–Barajas", "마드리드", "Madrid", "스페인"),
            new AirportInfo("AMS", "스키폴공항", "Amsterdam Schiphol", "암스테르담", "Amsterdam", "네덜란드"),
            new AirportInfo("IST", "이스탄불공항", "Istanbul Airport", "이스탄불", "Istanbul", "터키"),
            new AirportInfo("ZRH", "취리히공항", "Zurich Airport", "취리히", "Zurich", "스위스"),
            new AirportInfo("VIE", "빈공항", "Vienna International", "비엔나", "Vienna", "오스트리아"),
            new AirportInfo("PRG", "프라하공항", "Václav Havel", "프라하", "Prague", "체코"),
            new AirportInfo("HEL", "헬싱키공항", "Helsinki-Vantaa", "헬싱키", "Helsinki", "핀란드"),

            // === 오세아니아 ===
            new AirportInfo("SYD", "시드니공항", "Sydney Kingsford Smith", "시드니", "Sydney", "호주"),
            new AirportInfo("NAN", "난디공항", "Nadi International", "난디", "Nadi", "피지"),

            // === 몽골/중앙아시아 ===
            new AirportInfo("UBN", "칭기즈칸공항", "Chinggis Khaan", "울란바토르", "Ulaanbaatar", "몽골"),
            new AirportInfo("ALA", "알마티공항", "Almaty International", "알마티", "Almaty", "카자흐스탄")
    );

    /** 키워드로 공항 검색. IATA 코드, 도시명(한/영), 공항명, 국가명에서 매칭. */
    public static List<AirportInfo> search(String query) {
        if (query == null || query.isBlank()) {
            return AIRPORTS;
        }
        String q = query.trim().toLowerCase();
        return AIRPORTS.stream()
                .filter(a -> a.iata().toLowerCase().contains(q)
                        || a.city().contains(q)
                        || a.cityEn().toLowerCase().contains(q)
                        || a.name().contains(q)
                        || a.nameEn().toLowerCase().contains(q)
                        || a.country().contains(q))
                .toList();
    }

    /** IATA 코드로 단건 조회. */
    public static Optional<AirportInfo> findByIata(String iata) {
        if (iata == null) return Optional.empty();
        String code = iata.toUpperCase().trim();
        return AIRPORTS.stream()
                .filter(a -> a.iata().equals(code))
                .findFirst();
    }
}
