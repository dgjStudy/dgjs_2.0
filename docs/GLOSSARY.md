# DGJS 2.0 용어집

이 프로젝트에서 쓰는 용어를 정리한 문서이다. 기준은 현재 구현된 코드(모듈 B, C)이며, 아직 구현되지 않은 모듈 A(사용자, 부서, 프로젝트, 권한)의 용어는 구현 후에 추가한다.

## 읽는 방법
- 항목은 한글(가나다순), 영문·코드 식별자(A-Z순) 순서로 나열한다.
- 각 항목의 **분류**는 아래 색인의 분류와 같다.
- 항목은 서로 독립적으로 읽혀도 뜻이 통하도록 정의, 코드 식별자, 관련 용어, 위치를 함께 적는다.
- 구조·흐름의 상세는 [ARCHITECTURE.md](ARCHITECTURE.md), 진행 계획은 [ROADMAP.md](ROADMAP.md)를 본다.

## 분류별 색인

| 분류 | 항목 |
|---|---|
| 도메인 | 데이터셋, 등록일, 메타데이터, 미리보기, 소유자, 소속 조직, 수동 스키마 설계, 스키마, 적재, 최근 적재일, 카테고리, 컬럼, 컬럼 키, 컬럼 표시 설정, 컬럼명, 타입 추정, 행, 행 검증, STRING, NUMBER, DATE, BOOLEAN |
| 상태·진행 | 상태(status), 작업 ID, 진행률, PROCESSING, READY, FAILED, COMPLETED |
| 저장 구조 | JSON 적재 전략, dataset_meta, dataset_column, dataset_row, data_json, row_index, sort_order, last_loaded_at, owner_id, org_id, 인덱스 |
| 처리 방식 | 방문자, 배치 INSERT, 비동기 적재, 스트리밍, 인코딩 감지, 임시 파일, 적재 실패 정리, 청크, 폴링, 서버 페이징, DOM, SAX |
| 아키텍처 | Controller, Service, Repository, 엔티티, DTO, Config, 전역 예외 처리 |
| 프로젝트 약어 | 모듈, Core MVP, mock 사용자, 멀티테넌시 |
| 기술 | @Async, @Transactional, Apache POI, CORS, ddl-auto, JPA, Lombok, ORM, Swagger, 레이스 컨디션, 트랜잭션, 페이징 |

---

## 한글

### 데이터셋
- 분류: 도메인
- 정의: 사용자가 적재하거나 직접 설계한 표 하나. 엑셀·CSV 파일 1개(엑셀은 첫 번째 시트만) 또는 수동 스키마 1개가 데이터셋 1건이 된다.
- 구성: 메타데이터 1건(`dataset_meta`) + 컬럼 정의 N건(`dataset_column`) + 행 M건(`dataset_row`)
- 관련 용어: 메타데이터, 컬럼, 행, 적재, 수동 스키마 설계
- 위치: 화면 `/data`(목록), `/data/:id`(상세) / 코드 `DatasetMeta`, `DatasetService`

### 등록일
- 분류: 도메인
- 정의: 데이터셋 메타데이터가 처음 저장된 시각. 한 번 저장되면 바뀌지 않는다.
- 코드: `dataset_meta.created_at`, 응답 필드 `createdAt`
- 관련 용어: 최근 적재일

### 레이스 컨디션
- 분류: 기술
- 정의: 둘 이상의 작업이 동시에 진행될 때 실행 순서에 따라 결과가 달라지는 문제.
- 이 프로젝트의 사례: 비동기 적재를 시작하면서 진행률 작업을 등록하기 전에 화면의 첫 진행률 조회가 도착하면 "존재하지 않는 작업 ID"로 실패 처리되던 문제(ROADMAP 1번). 지금은 Controller가 비동기 호출 전에 작업을 먼저 등록한다.
- 관련 용어: 비동기 적재, 폴링, 작업 ID

### 멀티테넌시
- 분류: 프로젝트 약어
- 정의: 여러 사용자·조직이 하나의 시스템을 함께 쓰되 데이터의 소속을 구분하는 구조.
- 이 프로젝트에서: 인증(모듈 A) 구현 전이라도 엔티티와 API에 항상 `owner_id`, `org_id`를 두는 규칙. 현재는 mock 값을 쓴다.
- 관련 용어: owner_id, org_id, mock 사용자, 모듈

### 메타데이터
- 분류: 도메인
- 정의: 데이터셋에 대한 정보. 이름, 설명, 카테고리, 소유자, 소속 조직, 상태, 등록일, 최근 적재일.
- 주의: 컬럼 정의(`dataset_column`)와 행 데이터(`dataset_row`)는 메타데이터에 포함하지 않고 별도 테이블로 다룬다.
- 코드: `dataset_meta` 테이블, `DatasetMeta` 엔티티
- 관련 용어: 데이터셋, 스키마

### 모듈
- 분류: 프로젝트 약어
- 정의: 플랫폼의 기능 단위. 파이프라인 순서는 모듈 0 → B → C → A → D.

| 모듈 | 이름 | 상태 |
|---|---|---|
| 0 | .exe 수집기 | 미착수 |
| B | 적재/스키마 | 구현됨 |
| C | 시각화/그리드 | 구현됨 |
| A | 권한/소속 | 계획(2차 로드맵) |
| D | 외부 연계 API | 후순위 |

- 관련 용어: Core MVP

### 미리보기
- 분류: 도메인
- 정의: 적재 1단계에서 파일을 올렸을 때 DB에 저장하지 않고 헤더, 샘플 10행, 추정 타입, 구조 검증 결과만 돌려주는 단계.
- 동작: 첫 행을 컬럼명(헤더)으로 읽고 `col_0`, `col_1`... 키를 붙인다. 빈 헤더는 `Column_N`으로 임시 이름을 붙이고 경고를 낸다. 샘플 10행에서 읽기를 멈춘다.
- API: `POST /api/preview`
- 관련 용어: 적재, 타입 추정, 스키마

### 방문자
- 분류: 처리 방식
- 정의: 엑셀을 읽는 쪽과 읽은 행을 처리하는 쪽을 분리하기 위한 콜백 인터페이스 `RowVisitor`. 읽는 쪽이 행마다 `visit(rowNumber, cells)`를 호출하고, 반환값이 `false`이면 읽기를 중단한다.
- 용도: 같은 읽기 코드를 미리보기(10행에서 중단)와 적재(끝까지 읽기)가 공유한다.
- 주의: 객체 구조에 작업을 분리하는 정식 GoF 방문자 패턴이라기보다 콜백 방식에 가깝다.
- 위치: `SpreadsheetReader.RowVisitor`
- 관련 용어: 스트리밍, SAX, DOM

### 배치 INSERT
- 분류: 처리 방식
- 정의: 여러 행을 한 번에 묶어서 DB에 저장하는 방식.
- 이 프로젝트에서: 기본키가 자동 증가(IDENTITY)라서 JPA `saveAll`은 건별 INSERT가 되므로, 행 적재만 `JdbcTemplate.batchUpdate`로 500행씩 직접 저장한다. DB 접속 URL의 `rewriteBatchedStatements=true`가 묶음 처리를 빠르게 한다.
- 위치: `DatasetService.insertRows`
- 관련 용어: 청크, 적재

### 비동기 적재
- 분류: 처리 방식
- 정의: 파일 적재를 요청 처리와 별도 스레드에서 진행하고, 요청에는 작업 ID만 즉시 응답하는 방식. 대용량 파일을 올려도 화면이 멈추지 않는다.
- 흐름: Controller가 요청 검증, 작업 ID 생성, 임시 파일 저장, 진행률 작업 등록을 동기 구간에서 처리한 뒤 `@Async` 메서드를 호출한다. 이후 백그라운드가 메타 → 컬럼 → 행을 저장한다.
- API: `POST /api/upload-async`
- 위치: `DatasetService.processAsyncUpload`, `AsyncConfig`
- 관련 용어: @Async, 진행률, 폴링, 작업 ID, 임시 파일

### 상태(status)
- 분류: 상태·진행
- 정의: 데이터셋의 적재 상태. `PROCESSING`, `READY`, `FAILED` 중 하나이며 `NULL`은 `READY`로 간주한다(컬럼 추가 전 기존 데이터).
- 효과: 목록과 상세 조회는 `READY`(또는 NULL)만 노출한다. 적재 중이거나 실패한 데이터셋에는 행을 추가할 수 없다(404).
- 코드: `dataset_meta.status`
- 주의: 작업 ID 단위의 진행 상태(`PROCESSING`, `COMPLETED`, `FAILED`)와는 별개의 값이다.
- 관련 용어: PROCESSING, READY, FAILED, 진행률

### 서버 페이징
- 분류: 처리 방식
- 정의: 행 전체를 보내지 않고 서버가 `page`, `size`로 잘라 한 페이지씩 보내는 방식. 상세 화면의 데이터 그리드가 이 방식으로 조회한다.
- 규칙: `page`는 0부터 시작, 기본 `size`는 20. 음수 `page`는 0으로, 1 미만 `size`는 1로 보정한다.
- API: `GET /api/datasets/{id}/rows?page&size`
- 관련 용어: 페이징, 인덱스

### 소속 조직
- 분류: 도메인
- 정의: 데이터셋이 속한 조직을 나타내는 값. 현재는 mock 값 `org_hq`가 들어간다.
- 코드: `dataset_meta.org_id`
- 관련 용어: org_id, 멀티테넌시, mock 사용자

### 소유자
- 분류: 도메인
- 정의: 데이터셋을 등록한 사용자를 나타내는 값. 현재는 화면이나 요청이 보낸 값을 그대로 저장하며 mock 값 `user_admin`이 기본이다.
- 코드: `dataset_meta.owner_id`
- 관련 용어: owner_id, 멀티테넌시, mock 사용자

### 수동 스키마 설계
- 분류: 도메인
- 정의: 파일 없이 화면에서 컬럼명과 타입을 직접 정해 빈 데이터셋을 만드는 방식. 화면 이름은 "직접 스키마 설계"이다.
- 동작: 메타와 컬럼 정의만 저장하고 행은 0건으로 시작한다. 컬럼 키는 `col_0`부터 자동 부여되며, 빈 컬럼명은 `컬럼_N`, 빈 타입은 `STRING`이 된다. 행은 상세 화면에서 하나씩 추가한다. 비동기가 아니며 하나의 트랜잭션으로 처리한다.
- API: `POST /api/datasets/manual`
- 관련 용어: 스키마, 컬럼, 적재

### 스키마
- 분류: 도메인
- 정의: 데이터셋의 열 구조. 컬럼 목록과 각 컬럼의 키, 이름, 타입, 순서를 합친 것.
- 저장 위치: `dataset_column` 테이블
- 관련 용어: 컬럼, 컬럼 키, 컬럼명, STRING, NUMBER, DATE, BOOLEAN

### 스트리밍
- 분류: 처리 방식
- 정의: 파일 전체를 메모리에 올리지 않고 앞에서부터 조금씩 읽으며 처리하는 방식. xlsx는 SAX 스트리밍으로 읽어서 행 수가 늘어도 메모리 사용량이 일정하다.
- 효과: 힙 512MB 기준 10만 행에서 메모리 부족이던 것이 30만 행까지 힙 150MB 이내로 처리된다(ROADMAP 12번).
- 위치: `XlsxRowReader`
- 관련 용어: SAX, DOM, 임시 파일, 방문자

### 엔티티
- 분류: 아키텍처
- 정의: DB 테이블과 1:1로 대응하는 자바 클래스(JPA 엔티티). 클래스가 테이블, 필드가 컬럼, 객체 하나가 행 하나에 해당한다. API 응답으로 직접 반환하지 않고 DTO로 변환한다.
- 위치: `domain/` 패키지의 `DatasetMeta`, `DatasetColumn`, `DatasetRow`
- 관련 용어: JPA, ORM, DTO, Repository

### 인덱스
- 분류: 저장 구조
- 정의: 조회 속도를 높이기 위해 DB가 따로 관리하는 색인.
- 이 프로젝트에서: `dataset_row(dataset_id, row_index)`, `dataset_column(dataset_id)`에 인덱스가 있다. 엔티티의 `@Table(indexes = ...)`로 선언한다.
- 관련 용어: 서버 페이징, ddl-auto

### 인코딩 감지
- 분류: 처리 방식
- 정의: CSV 파일의 문자 인코딩을 자동으로 판별하는 동작. 파일을 스트림으로 한 번 훑어 UTF-8로 엄격하게 해석되면 UTF-8, 아니면 엑셀이 저장하는 CSV에 흔한 MS949(CP949)로 읽는다. 파일 앞의 BOM은 제거한다.
- 위치: `DatasetService.detectCsvCharset`, `openCsvParser`
- 관련 용어: 적재, 미리보기

### 임시 파일
- 분류: 처리 방식
- 정의: 업로드한 파일을 비동기 처리 동안 보관하기 위해 서버 임시 폴더에 저장한 파일. 이름은 `dgjs-upload-*.tmp`이다. 요청이 끝나면 서버가 원래 업로드 임시 파일을 지우기 때문에 별도 파일로 옮기며, 파일 내용을 `byte[]`로 메모리에 들고 있지 않게 해 준다. 처리가 끝나면(성공, 실패 모두) 삭제한다.
- 위치: `UploadFileStorage`
- 관련 용어: 비동기 적재, 스트리밍

### 작업 ID
- 분류: 상태·진행
- 정의: 비동기 적재 한 건을 식별하는 UUID(`taskId`). 적재 시작 응답으로 내려가며, 화면이 이 값으로 진행률을 조회한다.
- 관련 용어: 진행률, 폴링, 비동기 적재
- API: `GET /api/tasks/{taskId}/progress`

### 적재
- 분류: 도메인
- 정의: 엑셀·CSV 파일의 내용을 DB(`dataset_meta`, `dataset_column`, `dataset_row`)에 저장하는 일.
- 3단계: ① 파일 선택과 미리보기(저장 없음) ② 스키마 확인·수정(컬럼명, 타입) ③ 비동기 적재와 진행률 표시
- 규칙: 완전히 빈 행은 건너뛴다. 파일 적재는 값을 읽은 그대로 저장하며 타입 검증은 하지 않는다(엑셀 값은 읽을 때 숫자는 정수면 long, 날짜는 ISO 문자열로 바뀌고, CSV 값은 모두 문자열이다).
- 위치: 화면 `/upload`, 코드 `DatasetService.processAsyncUpload`
- 관련 용어: 미리보기, 비동기 적재, 수동 스키마 설계, 청크

### 적재 실패 정리
- 분류: 처리 방식
- 정의: 비동기 적재 도중 오류가 나면 이미 저장된 데이터를 삭제해 미완성 데이터셋이 남지 않게 하는 동작. 행, 컬럼, 메타 순서로 하나의 트랜잭션에서 삭제한다. 삭제마저 실패하면 메타 상태를 `FAILED`로 남겨 목록에서 숨긴다.
- 위치: `DatasetService.cleanupFailedUpload`
- 관련 용어: 상태(status), FAILED, 적재

### 전역 예외 처리
- 분류: 아키텍처
- 정의: Service가 던진 예외를 한곳에서 가로채 일관된 오류 응답으로 바꾸는 방식. Controller에는 `try/catch`를 두지 않는다.
- 응답 형식: 항상 `{ "message": "한국어 메시지" }`
- 매핑: 검증 실패와 요청 형식 오류는 400, 대상 없음과 없는 경로는 404, 지원하지 않는 메서드는 405, 그 외는 500(상세는 서버 로그에만 남기고 일반 문구로 응답)
- 위치: `GlobalExceptionHandler`
- 관련 용어: Config, Service

### 진행률
- 분류: 상태·진행
- 정의: 비동기 적재가 어디까지 진행됐는지 나타내는 0~100 값과 안내 메시지. 서버 메모리(`UploadTaskService`)에 작업 ID별로 보관하며, 서버를 재시작하면 사라진다.
- 구간: 10%(메타 저장), 25%(컬럼 저장), 40~95%(행 저장), 100%(완료)
- 보관: 완료·실패 후 10분이 지나면 1분 주기 스케줄러가 제거한다.
- 관련 용어: 작업 ID, 폴링, 비동기 적재

### 청크
- 분류: 처리 방식
- 정의: 대량 행을 나눠 처리하는 묶음 단위. 적재는 행을 500개씩 모아 한 번에 저장한다.
- 위치: `DatasetService`의 `CHUNK_SIZE = 500`
- 관련 용어: 배치 INSERT, 적재

### 최근 적재일
- 분류: 도메인
- 정의: 데이터셋에 데이터가 마지막으로 들어온 시각. 적재 완료 시와 행 추가 시에 갱신하며, 행 수정과 삭제에서는 갱신하지 않는다. 값이 없는 데이터셋(컬럼 추가 전 데이터, 행이 없는 수동 데이터셋)은 응답에서 등록일로 대체해 보여 준다.
- 코드: `dataset_meta.last_loaded_at`, 응답 필드 `lastLoadedAt`
- 관련 용어: 등록일, 적재

### 카테고리
- 분류: 도메인
- 정의: 데이터셋의 분류 값. 화면 표기는 "분류"이다. 선택지는 에너지/환경, 설비관리, 품질관리, 생산실적, 기타이며 기본값은 기타이다.
- 계획: 프로젝트 개념 도입 후 화면과 컬럼을 삭제할 예정이다(ROADMAP M-2).
- 코드: `dataset_meta.category`, 프론트 `constants/category.js`
- 관련 용어: 메타데이터

### 컬럼
- 분류: 도메인
- 정의: 데이터셋의 열 하나의 정의. 키, 이름, 타입, 순서를 가진다. 열의 정의일 뿐이며 실제 값은 행의 JSON 안에 있다.
- 코드: `dataset_column` 테이블, `DatasetColumn` 엔티티
- 관련 용어: 스키마, 컬럼 키, 컬럼명, 행

### 컬럼 키
- 분류: 도메인
- 정의: 행 JSON 안에서 값을 가리키는 컬럼의 고유 식별자. 컬럼 위치 순서대로 `col_0`, `col_1`, ... 형식으로 자동 부여한다. 컬럼명을 바꿔도 키는 바뀌지 않아 행 데이터를 수정할 필요가 없다.
- 예시: `{"col_0": "홍길동", "col_1": 30}`
- 코드: `dataset_column.column_key`
- 관련 용어: 컬럼명, data_json, 스키마

### 컬럼 표시 설정
- 분류: 도메인
- 정의: 상세 화면의 데이터 그리드에서 보고 싶은 컬럼만 켜고 끄는 화면 기능. 화면 표시만 바꾸며 저장된 데이터와 스키마는 바뀌지 않는다.
- 위치: 프론트 `ColumnSettingsContent`, `DatasetDataTab`
- 관련 용어: 컬럼

### 컬럼명
- 분류: 도메인
- 정의: 화면에 표시되는 컬럼 이름. 엑셀·CSV는 첫 행의 값이 컬럼명이 된다. 적재 시 빈 값이나 중복은 허용하지 않는다(400).
- 코드: `dataset_column.column_name`
- 관련 용어: 컬럼 키, 컬럼

### 타입 추정
- 분류: 도메인
- 정의: 미리보기 단계에서 샘플 행의 값을 보고 컬럼 타입을 자동으로 정하는 동작. 샘플 값이 모두 숫자로 읽히면 `NUMBER`, 그 외는 `STRING`이다. `DATE`와 `BOOLEAN`은 자동 추정하지 않으며 사용자가 수정하거나 수동 스키마 설계에서 지정한다.
- 위치: `DatasetService.inferColumnDataType`
- 관련 용어: 미리보기, STRING, NUMBER, DATE, BOOLEAN

### 트랜잭션
- 분류: 기술
- 정의: 여러 DB 작업을 하나로 묶어 모두 성공하거나 모두 취소되게 하는 단위.
- 이 프로젝트에서: Service 메서드에 `@Transactional`을 붙여 경계를 정한다. 조회 전용은 `readOnly = true`. 비동기 적재는 하나의 큰 트랜잭션이 아니라 단계별로 따로 확정되며, 실패 시 "적재 실패 정리"가 되돌린다.
- 관련 용어: @Transactional, 적재 실패 정리

### 페이징
- 분류: 기술
- 정의: 많은 데이터를 일정 개수씩 나누어 조회하는 방식. 이 프로젝트는 서버가 잘라서 보내는 방식(서버 페이징)을 쓴다.
- 관련 용어: 서버 페이징

### 폴링
- 분류: 처리 방식
- 정의: 화면이 일정 간격으로 서버에 반복 질문해 상태를 확인하는 방식. 적재 화면은 1초마다 진행률 API를 호출하고, `COMPLETED`이면 상세 페이지로 이동하며 `FAILED`이면 실패 메시지를 보여 준다.
- API: `GET /api/tasks/{taskId}/progress`
- 관련 용어: 진행률, 작업 ID, 비동기 적재

### 행
- 분류: 도메인
- 정의: 데이터셋의 데이터 한 줄. 모든 컬럼의 값을 JSON 문자열 하나로 저장한다. 행 번호(`row_index`)는 0부터 순서대로 부여된다. 상세 화면의 행 `key`는 행 ID(`row_id`)이며 수정·삭제에 쓰인다.
- 코드: `dataset_row` 테이블, `DatasetRow` 엔티티
- 관련 용어: data_json, row_index, 컬럼 키

### 행 검증
- 분류: 도메인
- 정의: 행을 추가하거나 수정할 때 값이 스키마에 맞는지 검사하고 타입에 맞게 변환하는 동작. 스키마에 없는 키는 거부하고(단, `key`는 무시), 빈 값은 모든 타입에서 빈 문자열로 저장한다. 수정 시 값이 바뀌지 않은 컬럼은 기존 값을 그대로 유지한다.
- 타입별 규칙: STRING은 객체·배열 거부, NUMBER는 숫자로 읽히는 값만 허용(`"1,000"` 거부), DATE는 `yyyy-MM-dd` 또는 `yyyy-MM-ddTHH:mm`, BOOLEAN은 true/false(대소문자 무시)
- 실패 시: 400과 한국어 메시지
- 위치: `RowDataValidator`
- 관련 용어: 스키마, 컬럼 키, 적재(적재 시에는 이 검증을 하지 않음)

---

## 영문·코드 식별자

### @Async
- 분류: 기술
- 정의: 메서드를 호출한 쪽과 별도 스레드에서 실행하게 하는 스프링 어노테이션. `AsyncConfig`의 `@EnableAsync`로 기능을 켜야 동작한다.
- 위치: `DatasetService.processAsyncUpload`
- 관련 용어: 비동기 적재, Config

### @Transactional
- 분류: 기술
- 정의: 메서드 전체를 하나의 트랜잭션으로 묶는 스프링 어노테이션. 조회 전용 메서드에는 `readOnly = true`를 붙인다.
- 관련 용어: 트랜잭션, Service

### Apache POI
- 분류: 기술
- 정의: 자바에서 엑셀 파일을 읽는 라이브러리. 버전 5.2.5를 쓴다. xlsx는 SAX(`XlsxRowReader`), xls와 암호화 파일은 DOM(`WorkbookFactory`)으로 읽는다.
- 관련 용어: SAX, DOM, 스트리밍

### BOOLEAN
- 분류: 도메인
- 정의: 컬럼 타입의 하나(화면 표기: 불리언). 값은 true 또는 false이며 불리언으로 저장한다. 자동 추정되지 않는다.
- 관련 용어: STRING, NUMBER, DATE, 행 검증

### COMPLETED
- 분류: 상태·진행
- 정의: 비동기 작업(작업 ID)의 완료 상태. 진행률 응답에 `datasetId`가 함께 담기며, 화면은 이 값으로 상세 페이지에 이동한다. 데이터셋 `status`에는 없는 값이다.
- 관련 용어: 진행률, 작업 ID, READY

### Config
- 분류: 아키텍처
- 정의: 애플리케이션 전체의 동작 방식을 정하는 설정 코드. 업무 로직은 없다.
- 구성: `AsyncConfig`(비동기 켬), `SchedulingConfig`(주기 실행 켬), `GlobalExceptionHandler`(예외 → 오류 응답), `WebConfig`(CORS), `OpenApiConfig`(Swagger 문서 정보)
- 위치: `config/` 패키지

### Controller
- 분류: 아키텍처
- 정의: API 주소와 메서드를 연결하고 요청을 받아 Service를 호출하며 응답을 만드는 계층. 이 프로젝트는 업무 로직을 Service에 모으고 Controller는 호출만 담당하도록 얇게 유지하는 것을 규칙으로 한다.
- 위치: `DatasetController` (`/api/**` 전체)
- 관련 용어: Service, DTO

### Core MVP
- 분류: 프로젝트 약어
- 정의: 현재 개발 단계. 모듈 B(적재/스키마)와 모듈 C(시각화/그리드)까지를 가리킨다.
- 관련 용어: 모듈

### CORS
- 분류: 기술
- 정의: 출처(주소, 포트)가 다른 곳에서 오는 요청을 브라우저가 기본으로 막는 정책. 프론트(5173 포트)가 백엔드(8080 포트)를 호출할 수 있도록 서버가 허용 출처를 선언한다.
- 이 프로젝트에서: `/api/**`에 대해 `http://localhost:5173`, `http://localhost:3000`을 허용한다. 개발 서버는 Vite proxy로도 요청을 전달한다.
- 위치: `WebConfig`, `vite.config.js`

### data_json
- 분류: 저장 구조
- 정의: 행 한 줄의 모든 값을 `{"col_0": 값, "col_1": 값}` 형태로 담은 문자열 컬럼(LONGTEXT). 컬럼 수와 종류가 다른 어떤 데이터셋이든 같은 테이블에 담을 수 있게 한다.
- 코드: `dataset_row.data_json`
- 관련 용어: JSON 적재 전략, 컬럼 키, 행

### dataset_column
- 분류: 저장 구조
- 정의: 컬럼 정의를 저장하는 테이블. 한 행이 컬럼 하나이다.
- 주요 컬럼: `column_id`(PK), `dataset_id`(소속 데이터셋), `column_key`, `column_name`, `data_type`, `sort_order`
- 참고: `dataset_id`는 외래키 제약 없는 단순 숫자 참조이며, 삭제 순서와 일관성은 Service 코드가 지킨다.
- 관련 용어: 컬럼, 스키마

### dataset_meta
- 분류: 저장 구조
- 정의: 데이터셋 메타데이터를 저장하는 테이블. 한 행이 데이터셋 하나이다.
- 주요 컬럼: `dataset_id`(PK), `name`, `description`, `category`, `owner_id`, `org_id`, `status`, `created_at`, `last_loaded_at`
- 관련 용어: 메타데이터, 데이터셋

### dataset_row
- 분류: 저장 구조
- 정의: 데이터 행을 저장하는 테이블. 한 행이 데이터 한 줄이다. 모든 데이터셋의 행이 이 테이블 하나에 들어간다.
- 주요 컬럼: `row_id`(PK), `dataset_id`(소속 데이터셋), `row_index`(순서), `data_json`(값)
- 인덱스: `(dataset_id, row_index)`
- 참고: `dataset_id`는 외래키 제약 없는 단순 숫자 참조이다.
- 관련 용어: 행, data_json, JSON 적재 전략

### DATE
- 분류: 도메인
- 정의: 컬럼 타입의 하나(화면 표기: 날짜형). 값은 ISO 문자열(자정이면 날짜만, 아니면 날짜와 시각)로 저장한다. 자동 추정되지 않는다.
- 예시: `2026-10-13`, `2026-10-13T09:30`
- 관련 용어: STRING, NUMBER, BOOLEAN, 행 검증

### ddl-auto
- 분류: 기술
- 정의: 엔티티 클래스를 보고 DB 테이블을 자동으로 만들거나 갱신하는 JPA 설정. 이 프로젝트는 `update`이며, 프로토타입 한정이다.
- 위치: `application.yml`
- 관련 용어: JPA, 엔티티, 인덱스

### DOM
- 분류: 처리 방식
- 정의: 문서 전체를 메모리에 객체로 올린 뒤 탐색하는 읽기 방식. 파일이 클수록 메모리 사용이 늘어난다. 이 프로젝트에서는 xls(65,536행 제한)와 암호화된 엑셀 파일에만 쓴다.
- 관련 용어: SAX, 스트리밍, Apache POI

### DTO
- 분류: 아키텍처
- 정의: API로 주고받는 데이터의 규격을 정의한 클래스(Data Transfer Object). DB 구조와 분리되어 화면에 필요한 모양과 노출할 필드만 담는다. 목록의 행 수처럼 DB 컬럼에 없는 값도 담을 수 있다. 값을 채우는 것은 Service이다.
- 구성: `DatasetDto`(목록, 상세, 컬럼, 행 페이징, 수동 생성 요청, 메타 수정 요청), `SchemaPreviewDto`(미리보기 응답, 최종 업로드 요청, 진행률 응답)
- 이름 규칙: `XxxRequest`, `XxxResponse`
- 참고: 행 추가·수정 요청은 컬럼이 데이터셋마다 달라 DTO가 아닌 `Map`으로 받는다.
- 관련 용어: 엔티티, Controller

### FAILED
- 분류: 상태·진행
- 정의: 실패 상태. 데이터셋 `status`에서는 적재 실패 후 정리마저 실패해 남겨진 데이터셋을 뜻하며 목록에서 숨긴다. 작업 진행률에서는 적재 실패 또는 존재하지 않는 작업 ID를 뜻한다.
- 관련 용어: 상태(status), 적재 실패 정리, 진행률

### JPA
- 분류: 기술
- 정의: 자바 객체와 DB 테이블을 연결해 주는 표준 기술(Spring Data JPA 사용). SQL을 직접 쓰지 않고 `save`, `findById` 같은 메서드나 메서드 이름 규칙, `@Query`로 DB를 다룬다.
- 관련 용어: ORM, 엔티티, Repository

### JSON 적재 전략
- 분류: 저장 구조
- 정의: 사용자별 `CREATE TABLE`을 하지 않고, 메타데이터 테이블(`dataset_meta`)과 컬럼 정의 테이블(`dataset_column`), JSON 데이터 테이블(`dataset_row`)로 분리해 모든 데이터셋을 공통 테이블 3개에 저장하는 원칙. 프로젝트 규칙으로 유지한다.
- 관련 용어: data_json, 메타데이터, 컬럼, 행

### last_loaded_at
- 분류: 저장 구조
- 정의: `dataset_meta`의 최근 적재일 컬럼. NULL일 수 있으며 응답에서는 등록일로 대체한다.
- 관련 용어: 최근 적재일

### Lombok
- 분류: 기술
- 정의: getter, setter, 생성자, 빌더 같은 반복 코드를 어노테이션으로 자동 생성하는 라이브러리. 엔티티는 `@Getter @NoArgsConstructor @Builder` 중심, DTO는 `@Data @Builder @NoArgsConstructor @AllArgsConstructor`, 의존성 주입은 `@RequiredArgsConstructor`를 쓴다.

### mock 사용자
- 분류: 프로젝트 약어
- 정의: 인증(모듈 A) 구현 전까지 사용하는 임시 사용자·조직 값. 소유자 `user_admin`, 조직 `org_hq`를 쓴다. 상단 바에 표시하는 임시 사용자 정보는 프론트 상수(`MOCK_USER`)이다.
- 위치: 프론트 `constants/mockUser.js`, 백엔드 기본값
- 관련 용어: 멀티테넌시, owner_id, org_id

### NUMBER
- 분류: 도메인
- 정의: 컬럼 타입의 하나(화면 표기: 숫자형). 소수부가 없고 절댓값이 1e15 미만이면 정수(long), 그 외는 실수(double)로 저장한다. 미리보기에서 샘플 값이 모두 숫자로 읽히면 자동으로 이 타입이 추정된다.
- 관련 용어: STRING, DATE, BOOLEAN, 타입 추정, 행 검증

### ORM
- 분류: 기술
- 정의: 객체와 관계형 DB 테이블을 자동으로 연결해 주는 기법(Object-Relational Mapping). JPA가 그 표준이다.
- 관련 용어: JPA, 엔티티

### org_id
- 분류: 저장 구조
- 정의: 데이터셋이 속한 조직 값. 현재는 mock 값 `org_hq`가 기본이다.
- 코드: `dataset_meta.org_id`, 응답 필드 `orgId`
- 관련 용어: 소속 조직, 멀티테넌시

### owner_id
- 분류: 저장 구조
- 정의: 데이터셋 등록자 값. 현재는 mock 값 `user_admin`이 기본이다.
- 코드: `dataset_meta.owner_id`, 응답 필드 `ownerId`
- 관련 용어: 소유자, 멀티테넌시

### PROCESSING
- 분류: 상태·진행
- 정의: 적재 진행 중 상태. 데이터셋 `status`로는 적재가 끝나기 전까지의 상태로 목록과 상세에서 숨기며, 작업 진행률에서는 아직 끝나지 않은 작업을 뜻한다.
- 관련 용어: 상태(status), READY, 진행률

### READY
- 분류: 상태·진행
- 정의: 데이터셋이 정상적으로 적재를 마치고 조회할 수 있는 상태. `status`가 NULL인 기존 데이터도 같은 의미로 취급한다.
- 관련 용어: 상태(status), PROCESSING

### Repository
- 분류: 아키텍처
- 정의: DB 읽기·쓰기를 담당하는 계층. Spring Data JPA 인터페이스로, 구현 코드 없이 메서드 이름 규칙이나 `@Query`로 쿼리가 만들어진다.
- 구성: `DatasetMetaRepository`, `DatasetColumnRepository`, `DatasetRowRepository`
- 관련 용어: JPA, 엔티티, Service

### row_index
- 분류: 저장 구조
- 정의: 데이터셋 안에서 행의 순서를 나타내는 0부터 시작하는 번호. 행 추가 시 현재 마지막 번호에 1을 더해 부여한다.
- 코드: `dataset_row.row_index`

### SAX
- 분류: 처리 방식
- 정의: 문서를 앞에서부터 읽으며 요소를 만날 때마다 이벤트로 알려 주는 읽기 방식. 메모리에 전체를 올리지 않는다. xlsx 읽기에 쓴다.
- 위치: `XlsxRowReader`
- 관련 용어: 스트리밍, DOM, 방문자

### Service
- 분류: 아키텍처
- 정의: 업무 로직과 트랜잭션 경계를 담당하는 계층. 검증, 파싱, 조합, DTO 변환을 하며 DB 접근은 Repository에 맡긴다.
- 구성: `DatasetService`(중심), `SpreadsheetReader`, `XlsxRowReader`(엑셀 읽기), `UploadFileStorage`(임시 파일), `UploadTaskService`(진행률 보관), `RowDataValidator`(행 검증), `ResourceNotFoundException`(404용 예외)
- 관련 용어: Controller, Repository

### sort_order
- 분류: 저장 구조
- 정의: 컬럼의 표시 순서를 나타내는 0부터 시작하는 번호.
- 코드: `dataset_column.sort_order`

### STRING
- 분류: 도메인
- 정의: 컬럼 타입의 하나(화면 표기: 문자형). 기본 타입이며, 값은 문자열로 저장한다. 객체나 배열은 허용하지 않는다.
- 관련 용어: NUMBER, DATE, BOOLEAN, 타입 추정

### Swagger
- 분류: 기술
- 정의: API 명세를 자동으로 보여 주는 도구(springdoc OpenAPI). 백엔드 실행 후 `http://localhost:8080/swagger-ui.html`에서 확인하며 JSON 명세는 `/v3/api-docs`이다. 현재는 설명 어노테이션이 없어 경로와 파라미터 형태만 표시된다.
- 위치: `OpenApiConfig`
- 관련 용어: Config
