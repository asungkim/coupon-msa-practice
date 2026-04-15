## Backend Template

```markdown
# Feature: <기능 이름>

## 1. Problem Definition
- 해결해야 할 문제를 기술한다.
- 이 Feature가 없으면 어떤 문제가 발생하는지 명확히 한다.

## 2. Prerequisites
- 이 Feature가 의존하는 선행 작업/Phase를 명시한다.
- 예: Phase 1 global 도메인 (RsCode, RsData, ApiExceptionHandler 등)

## 3. Requirements
### Functional
- 기능 요구사항 목록

### Non-functional
- 성능, 보안, 확장성 등

## 4. API Design (Draft)
| Method | URL | 설명 |
|--------|-----|------|
| POST | /api/v1/xxx | 설명 |

### 요청/응답 예시

#### POST /api/v1/xxx

요청:
```json
{
  "field": "value"
}
```

성공 응답 (200):

```json
{
  "code": 200,
  "message": "요청이 성공했습니다",
  "data": { ... }
}
```

실패 응답 (400):

```json
{
  "code": 400,
  "message": "[field] 필수 입력입니다"
}
```

### 에러 케이스

| 상황 | RsCode | HTTP Status | 메시지 |
|------|--------|-------------|--------|
| 유효하지 않은 입력 | INVALID_INPUT | 400 | 잘못된 요청입니다 |

## 5. Domain Model (Draft)

- Entity, DTO, Relation, Validation 규칙을 코드 스니펫으로 기술한다.

### Entity

```kotlin
// Entity 코드 스니펫
```

### DTO

```kotlin
// 요청/응답 DTO 코드 스니펫
```

## 6. Implementation Steps

구현 순서와 각 단계의 산출물을 정의한다.

| Step | 작업 | 산출물 |
|------|------|--------|
| 1 | Entity + Repository | Entity, Repository 클래스 |
| 2 | Service 비즈니스 로직 | Service 클래스 |
| 3 | Controller + 요청/응답 DTO | Controller, DTO 클래스 |

## 7. TDD Plan

Task별로 RED → GREEN → REFACTOR 사이클을 정의한다.
한 행동 → 한 실패 테스트 → 최소 구현 → green → 리팩터링의 세로 슬라이스로 진행한다.

### Task 1: <작업 단위>

1. **테스트 작성 (RED)**: 검증할 행동을 기술한다.
2. **최소 구현 (GREEN)**: 테스트를 통과시키기 위한 최소 코드를 기술한다.
3. **리팩터링**: 필요 시 구조 개선을 기술한다.

### Task 2: <작업 단위>

1. **테스트 작성 (RED)**: ...
2. **최소 구현 (GREEN)**: ...
3. **리팩터링**: ...

## 8. Verification Criteria

이 Feature가 완료됐다고 판단하는 기준을 명시한다.

- [ ] `cd backend && ./gradlew test` 전체 통과
- [ ] 패키지 구조가 아래와 일치

```
├── domain/
│   └── <feature>/
│       ├── web/
│       ├── application/
│       ├── entity/
│       ├── enum/
│       ├── dto/
│       │   ├── request/
│       │   └── response/
│       └── repository/
```

- [ ] API 응답이 RsData 공통 래핑을 따름
- [ ] 에러 케이스가 ApiExceptionHandler를 통해 처리됨

## 9. Design Decisions

설계 결정과 근거를 기록한다. 역정합화 시 코드와 대조하는 기준이 된다.

| 결정 | 근거 |
|------|------|
| 예시: X 방식 선택 | 이유: ... |

```
