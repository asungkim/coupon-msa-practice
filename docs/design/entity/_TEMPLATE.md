# {{PROJECT_NAME}} 엔티티 스펙

## 기본 데이터 (BaseEntity)

모든 엔티티는 BaseEntity를 상속합니다.

- id (UUID, PK)
- createdAt (LocalDateTime)
- updatedAt (LocalDateTime)
- deletedAt (LocalDateTime, nullable)

<!--
  프로젝트에 맞게 BaseEntity 필드를 수정하세요.
  예: Long id, auto-increment 방식 등
-->

**Soft Delete 헬퍼 메서드:**

- `isDeleted()`: deletedAt이 null이 아니면 true 반환
- `delete()`: deletedAt을 현재 시각으로 설정
- `restore()`: deletedAt을 null로 설정하여 복구

**상태 기반 라이프사이클 적용 엔티티:**

<!-- 상태 전환이 필요한 엔티티를 나열하세요 -->
- {{ENTITY_1}}: `ACTIVE` / `ARCHIVED`
- {{ENTITY_2}}: `ACTIVE` / `PAUSED`

---

## 1. {{도메인 그룹 이름}}

### {{ENTITY_NAME}}

- {{field_name}} ({{Type}}, {{constraint}})
- {{field_name}} ({{Type}}, {{constraint}})

**ENUM:**

```java
enum {{EnumName}} {
    VALUE_1,  // 설명
    VALUE_2   // 설명
}
```

**인덱스:**

- `idx_{{entity}}_{{field}}` on ({{field}})

**비고:**

- 비즈니스 규칙, 조회 조건, 특이사항 등

---

## 2. {{도메인 그룹 이름}}

### {{ENTITY_NAME}}

<!-- 위 패턴을 반복하여 모든 엔티티를 정의합니다 -->

---

<!--
  작성 가이드:
  1. 도메인 그룹별로 섹션을 나눕니다 (예: 조직, 회원, 수업, 출석 등)
  2. 각 엔티티마다 필드, Enum, 인덱스, 비고를 빠짐없이 기록합니다
  3. FK 관계는 필드 옆에 (FK → 대상 엔티티)로 표기합니다
  4. Soft Delete vs 상태 전환의 기준을 엔티티별로 명확히 합니다
-->
