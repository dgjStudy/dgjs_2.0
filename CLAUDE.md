# DGJS 2.0 Platform (Prototype)

정형 데이터(엑셀, CSV, 수동 스키마)를 적재하고, 웹에서 조회/시각화하며, 외부 시스템과 API로 연계하는 경량 데이터 플랫폼.

- 파이프라인: [모듈 0: .exe 수집기] → [모듈 B: 적재/스키마] → [모듈 C: 시각화/그리드] → [모듈 A: 권한/소속] → [모듈 D: 연계 API]
- 현재 단계: **Core MVP (모듈 B + C)**. 사용자/로그인(A)과 외부 연계 API(D)는 후순위.
- 원본 규칙: `.agents/rules/dgjs2.md` (Antigravity IDE용). 규칙이 바뀌면 두 파일을 함께 갱신한다.

## 실행 방법

### Backend (`backend/`, Spring Boot 3.3.4 / Java 17 / MySQL)
1. 로컬 MySQL에 `dgjs2_db` 스키마 생성 (테이블은 `ddl-auto: update`로 자동 생성)
2. DB 접속 정보 설정 — 아래 중 하나
   - `src/main/resources/application-local.yml.example` 를 `application-local.yml` 로 복사 후 값 입력 (git 무시됨)
   - 또는 환경변수 `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`
3. `./gradlew bootRun` → http://localhost:8080
4. API 명세(Swagger UI): http://localhost:8080/swagger-ui.html

### Frontend (`frontend/`, React 19 + Vite + antd 6)
- `npm install` → `npm run dev` → http://localhost:5173
- `npm run lint` (oxlint), `npm run build`

## 구조

```
backend/src/main/java/com/dgjs/backend/
  config/      AsyncConfig(@EnableAsync), WebConfig(CORS)
  controller/  DatasetController — 모든 REST API (/api/**)
  service/     DatasetService(파싱·적재·CRUD 핵심 로직), UploadTaskService(비동기 진행률 인메모리 맵)
  domain/      DatasetMeta, DatasetColumn, DatasetRow
  repository/  JpaRepository
  dto/         DatasetDto, SchemaPreviewDto
frontend/src/
  App.jsx            레이아웃 + 페이지 3개 (/data, /data/:id, /upload)
  api/datasetApi.js  fetch 래퍼
```

### 데이터 모델 (사용자별 CREATE TABLE 금지 — 메타/데이터 분리 전략)
- `dataset_meta` — 데이터셋 1건 (name, description, category, owner_id, org_id, created_at)
- `dataset_column` — 열 정의 (column_key=`col_0..N`, column_name, data_type: STRING/NUMBER/DATE/BOOLEAN, sort_order)
- `dataset_row` — 행 1건 = `data_json` (`{"col_0": ..., "col_1": ...}`), row_index

### 파일 적재 흐름
1. `POST /api/preview` — 헤더 + 샘플 10행 파싱, 열 타입 추정, 구조 검증
2. 사용자가 스키마(컬럼명/타입) 확정 → `POST /api/upload-async` → `@Async`로 500건 청크 적재, taskId 반환
3. 프론트가 1초마다 `GET /api/tasks/{taskId}/progress` 폴링 → 완료 시 상세 페이지 이동

## 규칙
- **Backend**: Spring Boot 3.x, JPA (MyBatis 허용), MySQL. 1,000행 이상 대용량 적재는 비동기 처리.
- **Frontend Styling**
  - 별도 CSS/SCSS 파일 작성 금지.
  - 모든 UI는 antd 컴포넌트(Table, Form, Upload, Card, message 등) + 인라인 `style` prop으로만 해결.
  - 순수 HTML 태그보다 antd 컴포넌트 우선.
- **Multi-tenancy**: 인증(A 모듈) 구현 전이라도 엔티티·API 파라미터에 항상 `owner_id`, `org_id`를 상정 (현재 mock: `user_admin`, `org_hq`).
- **보안**: DB 비밀번호 등 시크릿은 절대 커밋하지 않는다 (`application-local.yml` 또는 환경변수 사용).
- **코딩 스타일**: 장황한 이론 설명보다 즉시 실행 가능한 코드 위주. 컴포넌트/API 단위로 모듈화.
