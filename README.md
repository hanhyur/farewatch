# FareWatch

> **"지금 이 항공권 가격에 사도 되는가?"** — 가격 히스토리에 기반해 구매 시점을 판단해주는 시스템.

[![CI](https://github.com/hanhyur/farewatch/actions/workflows/ci.yml/badge.svg)](https://github.com/hanhyur/farewatch/actions/workflows/ci.yml)
[![Java](https://img.shields.io/badge/Java-21-orange)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.5-brightgreen)](https://spring.io/projects/spring-boot)
[![Next.js](https://img.shields.io/badge/Next.js-16-black)](https://nextjs.org/)
[![License](https://img.shields.io/badge/License-MIT-blue)](#license)

---

## 개요

항공권은 가격 변동 폭이 크고, 같은 노선이라도 예매 시점에 따라 수십만 원 차이가 납니다. 일반 메타서치(스카이스캐너 등)는 **현재 최저가**만 보여주지 `지금 이 가격이 역사적으로 싼지`는 알려주지 않습니다.

**FareWatch는 노선별 가격 스냅샷을 주기적으로 수집하고, 통계 기반으로 "지금 사도 되는가"를 정량적으로 판단합니다.**

| 판단 | 기준 | 액션 |
|------|------|------|
| **Cheap** ("지금 사세요") | `price < avg - 1.0 * stdDev` | 즉시 구매 추천 |
| **Fair** ("적정가") | 평균 +-0.5 stdDev 밴드 | 손해는 아님 |
| **Expensive** ("더 기다리세요") | `price > avg + 0.5 * stdDev` | 구매 보류 |
| **Insufficient** ("데이터 부족") | `sample < 30` | 통계 유의성 미달 |

## 주요 기능

- **가격 수집**: 12시간 주기 자동 수집 (스케줄러)
- **특가 판단**: z-score 기반 Rule Engine (Sealed Interface)
- **실시간 검색**: 왕복/편도, 직항/경유 필터, 공항 자동완성 (80+ 공항)
- **알림**: 조건 충족 시 이메일 알림 (중복 7일 쿨다운)
- **대시보드**: 노선 카드 + 판단 배지 + 가격 차트 + 필터

## 아키텍처

```
┌─────────────┐   @Scheduled(12h)    ┌───────────────┐
│  Scheduler  │ ──────────────────▶ │ FareCollector │  (인터페이스)
└─────────────┘                     └───────┬───────┘
                                            │ fetchFares()
                                            ▼
                                   ┌────────────────┐
                                   │ FareSnapshot   │  저장
                                   │  Repository    │
                                   └────────┬───────┘
                                            │ publish
                                            ▼
                          ┌───────────────────────────────┐
                          │  FareCollectedEvent (Spring)  │
                          └────┬────────────────────┬─────┘
                               │                    │
                StatisticsUpdateHandler    AlertEvaluationHandler
                               │                    │
                               ▼                    ▼
                     fare_statistics 재계산    판단 → NotificationSender
```

수집 → 저장 → 이벤트 → 통계/알림 단방향 파이프라인.

### 수집기 구조 (Strategy Pattern)

수집기(`FareCollector`)와 알림 발송기(`NotificationSender`)는 **인터페이스로 추상화**되어 있어, 구현체 교체만으로 실제 API 연동이 가능합니다.

```
FareCollector (인터페이스)
├── MockFareCollector        ← 저장소에 포함 (기본값, 외부 의존 없음)
└── SerpApiFareCollector     ← 실환경 (SerpApi Google Flights 연동)

NotificationSender (인터페이스)
├── LogNotificationSender    ← 저장소에 포함 (콘솔 로그 출력)
└── EmailNotificationSender  ← 실환경 (SMTP 이메일 발송)
```

> **보안 참고**: 실제 운영 환경에서는 SerpApi를 통해 Google Flights 데이터를 수집하지만, API 키 보안을 위해 저장소에는 Mock 구현체만 포함되어 있습니다. `application-local.yml`에 API 키를 설정하면 `@ConditionalOnProperty`에 의해 자동으로 실제 수집기로 전환됩니다.

## 기술 스택

### 백엔드
| 항목 | 기술 |
|------|------|
| Language | Java 21 (record, sealed interface, pattern matching) |
| Framework | Spring Boot 3.3.5 |
| Persistence | Spring Data JPA + Hibernate 6.5 |
| Database | H2 (dev) / PostgreSQL 16 (prod, Docker) |
| Migration | Flyway |
| API Docs | springdoc-openapi 2.6 (Swagger UI) |
| Build | Gradle (Groovy DSL) |
| Test | JUnit 5 + Mockito + AssertJ (80+ tests) |

### 프론트엔드
| 항목 | 기술 |
|------|------|
| Framework | Next.js 16 + React 19 |
| Styling | Tailwind CSS 4 |
| Data Fetching | TanStack Query |
| Chart | Recharts |
| Form | React Hook Form + Zod |

### 인프라
| 항목 | 기술 |
|------|------|
| 컨테이너 | Docker Compose (PostgreSQL + Backend + Frontend) |
| CI | GitHub Actions |

## 모노레포 구조

```
farewatch/
├── gradlew, settings.gradle       Gradle wrapper (루트)
├── docker-compose.yml             풀스택 Docker Compose
├── .env.example                   환경변수 템플릿
├── backend/
│   ├── Dockerfile
│   ├── build.gradle
│   └── src/main/java/com/farewatch/
│       ├── domain/                엔티티, VO, Repository
│       │   ├── route/             Route
│       │   ├── fare/              FareSnapshot, FareStatistics
│       │   ├── alert/             AlertRule, Notification
│       │   ├── judgment/          FareVerdict (sealed interface)
│       │   └── shared/            AirportCode, Money, DateRange 등
│       ├── application/           비즈니스 로직
│       │   ├── collector/         FareCollector 인터페이스 + 수집 서비스
│       │   ├── search/            실시간 검색 서비스
│       │   ├── analyzer/          통계 계산
│       │   ├── judgment/          특가 판단 Rule Engine
│       │   ├── alert/             알림 평가
│       │   └── event/             FareCollectedEvent
│       ├── infrastructure/        외부 연동 구현체
│       │   ├── collector/         MockFareCollector, SerpApiFareCollector
│       │   ├── notification/      Log/Email NotificationSender
│       │   └── scheduler/         @Scheduled 수집 트리거
│       └── api/                   REST 컨트롤러
│           ├── route/             노선 CRUD
│           ├── fare/              가격 히스토리/통계
│           ├── judgment/          판단 결과
│           ├── search/            실시간 검색
│           ├── airport/           공항 자동완성
│           ├── alert/             알림 규칙/이력
│           └── common/            ApiResponse, CORS, 예외 핸들러
└── frontend/
    ├── Dockerfile
    ├── package.json
    └── src/
        ├── app/                   페이지 (/, /search, /routes/[id], /notifications)
        ├── components/            UI 컴포넌트
        ├── lib/api/               API 클라이언트
        └── types/                 TypeScript 타입 정의
```

## 시작하기

### 방법 1: Docker Compose (권장)

```bash
git clone https://github.com/hanhyur/farewatch.git
cd farewatch

# (선택) SerpApi 실연동 시 .env 파일 생성
cp .env.example .env
# .env에 FAREWATCH_COLLECTOR_SERPAPI_API_KEY 설정

# 풀스택 기동 (PostgreSQL + Backend + Frontend)
docker compose up -d
```

| 서비스 | URL |
|--------|-----|
| Frontend | http://localhost:3000 |
| Backend API | http://localhost:8080 |
| Swagger UI | http://localhost:8080/swagger-ui.html |

```bash
docker compose logs -f        # 실시간 로그
docker compose down            # 중지
docker compose down -v         # 중지 + DB 데이터 삭제
```

### 방법 2: 로컬 개발

```bash
# 백엔드 (H2 + Mock 수집기)
./gradlew :backend:bootRun

# 백엔드 (시드 데이터 포함)
./gradlew :backend:bootRun --args='--spring.profiles.active=dev'

# 프론트엔드
cd frontend
npm install
npm run dev
```

### SerpApi 실연동

1. [SerpApi](https://serpapi.com/)에서 API 키 발급 (무료 250회/월)
2. `backend/src/main/resources/application-local.yml` 생성:
   ```yaml
   farewatch:
     collector:
       serpapi:
         api-key: YOUR_API_KEY
   ```
3. `--spring.profiles.active=local` 또는 Docker의 `.env` 파일에 설정
4. 키가 감지되면 `MockFareCollector` 대신 `SerpApiFareCollector`가 자동 활성화

### 프로파일 매트릭스

| 프로파일 | DB | 수집기 | 시드 데이터 | 이메일 |
|---|---|---|---|---|
| (기본) | H2 | Mock | X | X |
| dev | H2 | Mock | O | X |
| local | H2 | SerpApi | X | 설정 시 |
| docker | PostgreSQL | .env에 따라 | X | 설정 시 |
| docker,dev | PostgreSQL | .env에 따라 | O | 설정 시 |

## REST API

모든 응답은 공통 래퍼:
```json
{ "success": true, "data": { ... }, "timestamp": "2026-04-13T14:22:00Z" }
```

| Method | Path | 설명 |
|--------|------|------|
| `GET` | `/api/v1/routes` | 노선 목록 (origin/destination/active 필터) |
| `POST` | `/api/v1/routes` | 노선 등록 |
| `DELETE` | `/api/v1/routes/{id}` | 노선 비활성화 |
| `GET` | `/api/v1/routes/{id}/fares` | 가격 히스토리 |
| `GET` | `/api/v1/routes/{id}/statistics` | 통계 조회 |
| `GET` | `/api/v1/routes/{id}/judgment` | **판단 결과** (핵심) |
| `GET` | `/api/v1/search` | 실시간 항공편 검색 (왕복/편도, 직항/경유) |
| `GET` | `/api/v1/airports?q=` | 공항 자동완성 (한/영 도시명, IATA 코드) |
| `POST` | `/api/v1/alert-rules` | 알림 규칙 등록 |
| `GET` | `/api/v1/alert-rules` | 알림 규칙 목록 |
| `DELETE` | `/api/v1/alert-rules/{id}` | 알림 규칙 삭제 |
| `GET` | `/api/v1/notifications` | 알림 이력 |

## 설계 원칙

### Domain-Driven Design (DDD)
- **Aggregate Root 5개**: Route, FareSnapshot, FareStatistics, AlertRule, Notification
- **교차 참조는 ID 만** — `@ManyToOne`/`@OneToMany` 사용 안 함
- **Value Object** (`record` + `@Embeddable`): AirportCode, Money, DateRange, EmailAddress
- **Sealed Interface**: FareVerdict — Java 21 pattern matching으로 exhaustiveness 보장

### 인터페이스 기반 추상화
- `FareCollector` — 수집기 추상화 (Mock / SerpApi)
- `NotificationSender` — 발송기 추상화 (Log / Email)
- `@ConditionalOnProperty` / `@ConditionalOnMissingBean`으로 자동 전환

### 테스트 주도 개발 (TDD)
- RED → GREEN → REFACTOR 사이클
- 80+ 단위/통합 테스트
- `@DataJpaTest`로 레포지토리 슬라이스 테스트

## 테스트

```bash
./gradlew :backend:test                    # 전체 백엔드 테스트
./gradlew :backend:test --tests "*.judgment.*"  # 특정 패키지만
./gradlew :backend:jacocoTestReport        # 커버리지 리포트
```

## 브랜치 전략

```
main         ← 안정 브랜치
 └─ feat/*   ← 피처 브랜치 (main에서 분기, PR로 병합)
```

Conventional Commits: `feat:`, `fix:`, `refactor:`, `chore:`, `docs:`

## License

MIT
