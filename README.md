# FareWatch

> **"지금 이 항공권 가격에 사도 되는가?"** — 가격 히스토리에 기반해 구매 시점을 판단해주는 시스템.

[![CI](https://github.com/hanhyur/farewatch/actions/workflows/ci.yml/badge.svg?branch=dev)](https://github.com/hanhyur/farewatch/actions/workflows/ci.yml)
[![Java](https://img.shields.io/badge/Java-21-orange)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.5-brightgreen)](https://spring.io/projects/spring-boot)
[![License](https://img.shields.io/badge/License-MIT-blue)](#license)

---

## 개요

항공권은 가격 변동 폭이 크고, 같은 노선이라도 예매 시점에 따라 수십만 원 차이가 납니다. 일반 메타서치(스카이스캐너 등)는 **현재 최저가**만 보여주지 `지금 이 가격이 역사적으로 싼지`는 알려주지 않습니다.

**FareWatch는 노선별 가격 스냅샷을 주기적으로 수집·통계화해서 "지금 사도 되는가"를 정량적으로 판단합니다.**

| 판단 | 기준 | 액션 |
|------|------|------|
| 🟢 **Cheap** ("지금 사세요") | `price < avg − 1.0σ` | 즉시 구매 추천 |
| 🟡 **Fair** ("적정가") | 평균 ±0.5σ 밴드 | 손해는 아님 |
| 🔴 **Expensive** ("더 기다리세요") | `price > avg + 0.5σ` | 구매 보류 |
| ⚪ **Insufficient** ("데이터 부족") | `sample < 30` | 통계 유의성 미달 |

## 아키텍처 한눈에

```
┌─────────────┐   @Scheduled(6h)     ┌───────────────┐
│  Scheduler  │ ───────────────────▶ │ FareCollector │  (인터페이스)
└─────────────┘                      └───────┬───────┘
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

수집 → 저장 → 이벤트 → 통계/알림 단방향 파이프라인. 수집기와 발송기는 인터페이스로 추상화되어, 실제 API / 이메일 연동은 구현체 교체로 가능합니다.

## 기술 스택

### 백엔드
- **Language**: Java 21 (record, sealed interface, pattern matching)
- **Framework**: Spring Boot 3.3.5
- **Persistence**: Spring Data JPA + Hibernate 6.5
- **Database**: H2 (dev, in-memory) / PostgreSQL (prod)
- **API Docs**: springdoc-openapi 2.6 (Swagger UI)
- **Build**: Gradle (Groovy DSL)
- **Test**: JUnit 5 + Mockito + AssertJ

### 프론트엔드 *(Phase 2 — 예정)*
- Next.js 기반 대시보드
- 노선 상세 + 가격 추이 차트 + 알림 규칙 등록 + 이력

## 모노레포 구조

루트는 **Gradle 멀티 프로젝트** 이며 `backend` 를 서브프로젝트로 포함합니다. `gradlew` 래퍼와 `settings.gradle` 은 루트에 있고, 루트에서 모든 Gradle 태스크를 실행할 수 있습니다.

```
farewatch/
├── gradlew, gradlew.bat        Gradle wrapper (루트)
├── gradle/wrapper/             wrapper jar + properties
├── settings.gradle             rootProject.name + include 'backend'
├── backend/                    Spring Boot 서브프로젝트
│   ├── build.gradle
│   └── src/
│       ├── main/java/com/farewatch/
│       │   ├── FarewatchApplication.java
│       │   └── domain/
│       │       ├── shared/      공용 VO (AirportCode, Money, DateRange, EmailAddress) + enum
│       │       ├── judgment/    FareVerdict (sealed interface)
│       │       ├── route/       Route + Repository
│       │       ├── fare/        FareSnapshot, FareStatistics + Repositories
│       │       └── alert/       AlertRule, Notification + Repositories
│       └── main/resources/
│           └── application.yml
├── frontend/                   Next.js 앱 (Phase 2)
└── README.md
```

## 시작하기

### 사전 요구사항

- **JDK 21+** (`java -version` 으로 확인)
- Git

### 백엔드 실행

```bash
git clone https://github.com/hanhyur/farewatch.git
cd farewatch
./gradlew :backend:bootRun
```

기동 확인:
```
Started FarewatchApplication in X.XXX seconds
Tomcat started on port 8080 (http)
```

### 접근 가능한 엔드포인트

| URL | 용도 |
|-----|------|
| [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html) | Swagger UI (API 탐색) |
| [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs) | OpenAPI 3 JSON |
| [http://localhost:8080/h2-console](http://localhost:8080/h2-console) | H2 웹 콘솔 |

H2 콘솔 접속 정보:
- JDBC URL: `jdbc:h2:mem:farewatch`
- User: `sa`
- Password: *(빈칸)*

### 테스트 실행

```bash
# 루트에서 모든 서브프로젝트 테스트
./gradlew test

# backend 만 콕 집어서
./gradlew :backend:test
```

## REST API (계획)

v0 의 공용 응답 래퍼:

```json
{
  "success": true,
  "data": { ... },
  "timestamp": "2026-04-09T14:22:00Z"
}
```

### 주요 엔드포인트

| Method | Path | 설명 |
|--------|------|------|
| `GET` | `/api/v1/routes` | 노선 목록 |
| `GET` | `/api/v1/routes/{id}/fares` | 가격 히스토리 |
| `GET` | `/api/v1/routes/{id}/statistics` | 통계 조회 |
| `GET` | `/api/v1/routes/{id}/judgment` | **판단 결과** (핵심) |
| `POST` | `/api/v1/alert-rules` | 알림 규칙 등록 |
| `GET` | `/api/v1/alert-rules` | 알림 규칙 목록 |
| `DELETE` | `/api/v1/alert-rules/{id}` | 알림 규칙 soft delete |
| `GET` | `/api/v1/notifications` | 알림 이력 |

판단 결과 응답 예시:
```json
{
  "success": true,
  "data": {
    "routeId": 1,
    "verdict": "CHEAP",
    "currentPrice": 178000,
    "avgPrice": 232000,
    "minPrice": 154000,
    "stdDeviation": 28500.0,
    "zScore": -1.89,
    "suggestion": "지금 구매를 추천합니다"
  },
  "timestamp": "2026-04-09T14:22:00Z"
}
```

## 설계 원칙

### Domain-Driven Design (DDD)
- **Aggregate Root 5개**: `Route`, `FareSnapshot`, `FareStatistics`, `AlertRule`, `Notification`
- **교차 참조는 ID 만** — `@ManyToOne`/`@OneToMany` 금지 (Vernon 스타일)
- **Value Object** (`record` + `@Embeddable`): `AirportCode`, `Money`, `DateRange`, `EmailAddress`
- **Sealed Interface**: `FareVerdict` — Java 21 pattern matching 으로 exhaustiveness 보장
- **생성자 기반 invariant**: 모든 엔티티는 `protected` 무인자 생성자 + `public static` 팩토리 + 검증

### 테스트 주도 개발 (TDD)
- **RED → GREEN → REFACTOR** 사이클 엄수
- **@DataJpaTest** 로 레포지토리 slice 테스트 (H2)
- **단위 테스트** 는 Spring context 없이 pure JUnit 5

### 개발 순서
1. **Phase 1 — 백엔드**
   - [x] Gradle + Spring Boot 부트스트랩
   - [x] 도메인 엔티티 5개 + Repository
   - [ ] Rule Engine (z-score 판단 로직)
   - [ ] Mock FareCollector + Scheduler
   - [ ] 이벤트 파이프라인 (통계 재계산 + 알림 평가)
   - [ ] REST API + 공용 응답 래퍼
2. **Phase 2 — 프론트엔드**
   - [ ] Next.js 프로젝트 세팅
   - [ ] 메인 대시보드 / 노선 상세 / 알림 등록 / 이력 4페이지

## 브랜치 전략

```
main         ← 릴리스/안정
 └─ dev      ← 통합 브랜치 (피처 병합 대상)
     └─ feat/*   ← 피처 브랜치 (dev에서 분기)
```

- `main` 직접 푸시 금지, `dev` 통해서만 릴리스
- 피처 브랜치는 `dev` 에서 분기, PR 통해 `dev` 로 병합
- Conventional Commits (`feat(backend): ...`, `fix: ...`, `chore: ...`)

## 기여하기

```bash
# 1. dev 브랜치에서 피처 브랜치 생성
git checkout dev
git pull
git checkout -b feat/my-feature

# 2. TDD 사이클
# - 실패하는 테스트 작성 (RED)
# - 최소 구현 (GREEN)
# - 리팩토링 (REFACTOR)

# 3. 빌드 + 테스트 확인
./gradlew :backend:build

# 4. PR 생성 (dev 기준)
```

## License

MIT

## 참고

- Planner/설계 문서는 로컬에서만 관리됩니다 (`docs/` 는 gitignored).
- 현재 수집기는 Mock 구현만 존재합니다. 실제 항공권 API 연동은 인터페이스 교체로 예정.
