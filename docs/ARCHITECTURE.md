# DGJS 2.0 아키텍처

## 디렉터리 구조

```
dgjs_2.0/
├─ CLAUDE.md                  Claude Code 작업 규칙
├─ docs/                      프로젝트 문서
├─ backend/                   Spring Boot 3.3.4 / Java 17 / JPA / MySQL / Apache POI / springdoc
│   └─ src/main/java/com/dgjs/backend/
│       ├─ config/
│       │   ├─ AsyncConfig        @EnableAsync (비동기 적재)
│       │   ├─ SchedulingConfig   @EnableScheduling (종료된 작업 정리)
│       │   ├─ GlobalExceptionHandler  @RestControllerAdvice, 오류 응답 통일
│       │   ├─ WebConfig          CORS 허용 (localhost:5173, 3000)
│       │   └─ OpenApiConfig      Swagger 문서 메타정보
│       ├─ controller/
│       │   └─ DatasetController  모든 REST API (/api/**)
│       ├─ service/
│       │   ├─ DatasetService     파일 파싱(엑셀/CSV), 미리보기·검증, 비동기 적재, 데이터셋/행 CRUD
│       │   ├─ UploadTaskService  비동기 적재 진행률을 taskId별로 메모리에 보관, 종료 10분 뒤 정리
│       │   ├─ RowDataValidator   행 추가·수정 시 스키마 키·타입 검증 및 변환
│       │   └─ ResourceNotFoundException  대상 없음(404) 예외
│       ├─ domain/                JPA 엔티티 (DatasetMeta, DatasetColumn, DatasetRow)
│       ├─ repository/            Spring Data JPA Repository
│       └─ dto/
│           ├─ DatasetDto         목록/상세/페이징/CRUD 요청·응답
│           └─ SchemaPreviewDto   미리보기 응답, 최종 업로드 요청, 진행률 응답
│   └─ src/test/java/com/dgjs/backend/   단위 테스트 (행 검증, CSV·엑셀 파싱, 업로드 검증, 작업 정리, 오류 응답). contextLoads만 MySQL 필요
└─ frontend/                  React 19 / Vite / antd 6 / react-router 7
    └─ src/
        ├─ main.jsx               엔트리
        ├─ App.jsx                레이아웃 + 페이지 3개
        │   ├─ DataListPage       /, /data      데이터셋 목록·삭제
        │   ├─ DataDetailPage     /data/:id     메타 정보, 서버 페이징 그리드, 컬럼 표시 토글, 메타 수정, 행 CRUD
        │   └─ DataUploadPage     /upload       ① 엑셀/CSV 3단계 적재  ② 수동 스키마 설계
        └─ api/datasetApi.js      백엔드 호출 fetch 래퍼
```

## 데이터 모델

사용자별 `CREATE TABLE` 대신, 메타데이터와 데이터를 공통 테이블 3개에 나눠 저장한다.

| 테이블 | 의미 | 주요 컬럼 |
|---|---|---|
| `dataset_meta` | 데이터셋 1건 (엑셀 시트 1장) | dataset_id, name, description, category, owner_id, org_id, status(PROCESSING/READY/FAILED, NULL=READY), created_at |
| `dataset_column` | 열 정의 (헤더 1칸) | dataset_id, column_key(`col_0..N`), column_name, data_type(STRING/NUMBER/DATE/BOOLEAN), sort_order. 인덱스 `(dataset_id)` |
| `dataset_row` | 행 1줄 | dataset_id, row_index, data_json(`{"col_0": ..., "col_1": ...}`). 인덱스 `(dataset_id, row_index)` |

## API

전체 명세는 백엔드 실행 후 **Swagger UI**에서 확인할 수 있다: http://localhost:8080/swagger-ui.html (JSON: `/v3/api-docs`)

| Method | Path | 역할 |
|---|---|---|
| GET | /api/datasets | 데이터셋 목록 (행 수/열 수 포함) |
| GET | /api/datasets/{id} | 상세 (메타 + 컬럼) |
| GET | /api/datasets/{id}/rows?page&size | 행 페이징 조회 |
| POST | /api/preview | 파일 → 헤더, 샘플 10행, 타입 추정, 구조 검증 |
| POST | /api/upload-async | 파일 + 확정 스키마 → 비동기 적재 시작, taskId 반환 |
| GET | /api/tasks/{taskId}/progress | 적재 진행률 조회 |
| POST | /api/datasets/manual | 수동 스키마로 빈 데이터셋 생성 |
| PUT / DELETE | /api/datasets/{id} | 메타 수정 / 데이터셋 삭제 |
| POST / PUT / DELETE | /api/datasets/{id}/rows[/{rowId}] | 행 추가 / 수정 / 삭제 |

### 오류 응답
- 전역 처리는 `GlobalExceptionHandler`가 한다. 컨트롤러에는 try/catch를 두지 않는다.
- 본문은 항상 `{ "message": "한국어 메시지" }`이다.

| 상태 | 대상 |
|---|---|
| 400 | `IllegalArgumentException`(검증 실패), JSON 형식 오류, 필수 요청 값·파트 누락, 파일 크기 초과, 요청 값 형식 오류 |
| 404 | `ResourceNotFoundException`(데이터셋·행 없음), 존재하지 않는 경로 |
| 405 | 지원하지 않는 HTTP 메서드 |
| 500 | 그 밖의 예외. 상세는 서버 로그에만 남기고 일반 문구로 응답 |

## 주요 흐름

### 파일 적재 (엑셀/CSV)
1. **미리보기** — 파일 선택 시 `POST /api/preview`. 헤더로 `col_i` 키를 만들고, 상위 10행을 읽어 열마다 타입을 추정(전부 숫자면 NUMBER, 아니면 STRING)하고, 빈 헤더·빈 파일 등 구조 문제를 검증한다. 중복 컬럼명은 Step1에서 수정 가능하므로 막지 않고, 프론트가 현재 스키마 기준으로 실시간 검증하며 서버도 `POST /api/upload-async`에서 컬럼명(빈 값·중복)·컬럼 키를 재검증한다(400). CSV는 Apache Commons CSV로 파싱하며(따옴표 안 줄바꿈·`""` 이스케이프 지원) UTF-8 엄격 검사에 실패하면 MS949로 읽고 BOM은 제거한다.
2. **스키마 확정** — 사용자가 컬럼명/타입을 수정한 뒤 `POST /api/upload-async` (file + metadata JSON).
3. **비동기 적재** — 컨트롤러가 파일을 바이트 배열로 읽고, `UploadTaskService.initTask`로 작업을 먼저 등록한 뒤 `@Async` 메서드에 넘기고 즉시 taskId를 응답한다. 백그라운드 스레드가 meta(status=PROCESSING) → column → 행(500건 단위, `JdbcTemplate.batchUpdate`)을 저장하며 진행률을 기록하고, 완료 시 status를 READY로 바꾼다. 실패하면 저장된 meta/column/row를 삭제한다(삭제마저 실패하면 FAILED로 남김). 목록·상세 조회는 READY(또는 NULL)만 노출한다.
4. **진행률 폴링** — 프론트가 1초마다 `GET /api/tasks/{taskId}/progress`를 호출해 진행률을 표시하고, COMPLETED면 상세 페이지로 이동한다. 완료·실패한 작업은 10분 뒤 메모리에서 제거되며(1분 주기), 이후 조회하면 "존재하지 않는 작업 ID"(FAILED)로 응답한다.

### 상세 조회
`GET /api/datasets/{id}`(메타·컬럼) + `GET /api/datasets/{id}/rows`(현재 페이지)를 조합한다. 행 응답에는 `key = row_id`가 붙어 행 수정/삭제에 쓰인다.

### 행 추가·수정 검증
`POST/PUT /api/datasets/{id}/rows[/{rowId}]`는 저장 전에 `RowDataValidator`가 컬럼 정의(`dataset_column`)로 검증·변환한다. 실패하면 400, 적재 중이거나 실패한 데이터셋에 추가하면 404이다.
- 스키마에 없는 키는 거부한다(`key`는 무시).
- 빈 값(null, 공백)은 모든 타입에서 `""`로 저장한다.

| 타입 | 허용 | 저장 형태 |
|---|---|---|
| STRING | 문자·숫자·불리언 | 문자열 (객체·배열 거부) |
| NUMBER | 숫자, 숫자로 읽히는 문자열 (`"1,000"`은 거부) | 정수는 long, 그 외 double |
| DATE | `yyyy-MM-dd`, `yyyy-MM-ddTHH:mm`, `yyyy-MM-dd HH:mm` | ISO 문자열 (자정이면 날짜만) |
| BOOLEAN | true/false, `"true"`/`"false"` (대소문자 무시) | 불리언 |

수정 시에는 기존 값과 같은 컬럼은 검증하지 않고 기존 값을 유지한다. 과거 적재 데이터에 형식이 맞지 않는 값이 있어도 다른 컬럼을 수정할 수 있게 하기 위함이다.

### 수동 생성
컬럼 정의만으로 meta + column을 저장(행 0개)하고, 상세 페이지에서 행을 추가한다.
