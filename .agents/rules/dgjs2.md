---
trigger: always_on
---

# Project Context: DGJS 2.0 Platform (Prototype)

## 1. Project Overview
- DGJS 2.0은 정형 데이터(엑셀, CSV, 수동 스키마)를 적재하고, 이를 웹에서 시각화 및 조회하며, 외부 시스템과 API로 연계할 수 있는 경량 데이터 플랫폼입니다.
- 아키텍처 파이프라인: [모듈 0: .exe 수집기] -> [모듈 B: 적재/스키마] -> [모듈 C: 시각화/그리드] -> [모듈 A: 권한/소속] -> [모듈 D: 연계 API]

## 2. Tech Stack & Rules
- Backend: Java Spring Boot 3.x, MySQL, MyBatis/JPA
- Frontend: React (Vite 기반), JavaScript/JSX
- UI Library: Ant Design (antd) 최신 버전 사용
- Data Storage Strategy:
  - 사용자별 DDL(`CREATE TABLE`)을 남발하지 않고, '메타데이터 테이블'과 '데이터 적재 테이블(JSON/공통 구조)' 분리 전략 채택.
  - 1,000행 이상의 대용량 적재 시 비동기 처리 고려.
- Frontend Styling Rule:
  - 별도의 CSS/SCSS 파일을 작성하지 않는다.
  - 모든 UI와 인터랙션은 Ant Design(antd) 내장 컴포넌트(Table, Form, Upload, Card, message 등)와 인라인 스타일(style prop)로만 해결한다.
  - 순수 HTML 태그 대신 antd 컴포넌트 우선 적용.
- Multi-tenancy (보안/소속):
  - 프로토타입 단계에서는 인증(A 모듈)을 구현하기 전이라도 엔티티와 API 파라미터에 항상 `owner_id`와 `org_id`를 상정하고 설계한다 (초기값은 mock data로 처리).

## 3. Current Phase: Core MVP (Module B + C)
- 사용자/로그인(A) 및 외부 연계 API(D)는 후순위.
- 현재는 [데이터 적재(B)]와 [데이터 테이블 조회/시각화(C)] 3개 페이지를 최우선으로 구축한다.
  1) /data (전체 데이터셋 목록)
  2) /data/:id (데이터셋 상세 그리드 뷰)
  3) /upload (엑셀 업로드 및 스키마 정의 페이지)

## 4. Coding Assistant Guidelines
- 장황한 이론 설명은 생략하고, 즉시 실행 가능한 코드(JSX, SpringBoot Controller/Service/Entity/Repository) 위주로 작성한다.
- 요청된 컴포넌트나 API 단위로 모듈화하여 간결하고 명확하게 제공한다.

## 5. Stack-specific Rules (요약 — 상세는 backend/CLAUDE.md, frontend/CLAUDE.md)
- Backend:
  - Controller는 요청/응답만, 로직은 Service. Entity를 직접 반환하지 말고 DTO(`XxxDto` 중첩 클래스)로 변환한다.
  - 예외는 `@RestControllerAdvice`에서 처리 (400/404/500), 오류 본문은 `{ "message": "..." }`.
  - 목록 API는 페이징 필수, 대량 insert/delete는 벌크 처리, `@Async` 안에서 작업 상태 초기화 금지.
  - 수정 후 `./gradlew compileJava` (로직 변경 시 `./gradlew test`).
- Frontend:
  - 구조는 `pages/` `components/` `api/` `constants/`. 파일 약 300줄 초과 시 분리 검토.
  - 백엔드 호출은 `src/api/`에서만, Base URL은 Vite proxy + 상대경로 `/api`.
  - 폼은 antd `Form`, 그리드는 서버사이드 페이징, 알림은 `message`/`Modal`. antd v6 deprecated API 금지.
  - 수정 후 `npm run lint`, `npm run build`.