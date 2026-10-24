# DGJS 2.0 개선 로드맵

2026-10-13 코드 분석에서 나온 문제 목록. 완료한 항목은 `[x]`로 바꾸고 커밋 해시를 적는다.

## 완료
- [x] CLAUDE.md 생성 및 규칙 이관
- [x] DB 접속 정보를 소스에서 분리 (`application-local.yml` / 환경변수)
- [x] Swagger(springdoc) API 명세 추가 → http://localhost:8080/swagger-ui.html
- [x] git 커밋 이력에서 DB 비밀번호 제거 (filter-repo, force push 완료)

## 🔴 P1 — 버그 / 데이터 정합성
- [x] **1. 진행률 폴링 레이스 컨디션** — `initTask()`가 @Async 스레드 안에서 호출됨(`DatasetService.processAsyncUpload`). 첫 폴링이 먼저 도착하면 "존재하지 않는 작업 ID(FAILED)" 응답을 받아 프론트가 실패 처리. → Controller에서 비동기 호출 전에 `initTask`. (aa56270)
- [x] **2. 적재 실패 시 부분 데이터 잔존** — 비동기 적재 중 실패하면 meta+column+일부 행이 남고, 적재 중에도 목록에 미완성 데이터셋이 노출됨. → 실패 시 정리(삭제), 필요하면 `dataset_meta.status`(PROCESSING/READY/FAILED) 추가. (aa56270)
- [x] **3. 수식 셀이 수식 문자열로 저장** — `getCellValue`의 FORMULA가 `getCellFormula()` 반환. → 캐시된 결과값 사용. (aa56270)
- [x] **4. 날짜/숫자 포맷** — 날짜가 `Date.toString()` 형식, 정수가 `1001.0`으로 저장. → 날짜는 ISO 형식, 정수는 long. (aa56270)
- [x] **5. 중복 컬럼명 수정 불가** — 미리보기 `valid=false`면 적재 버튼이 비활성인데, Step1에서 컬럼명을 고쳐도 valid가 갱신되지 않음. → 프론트에서 현재 스키마 기준 재검증 + 서버 최종 업로드 시 재검증. (d1451d5, 6b56e6b)
- [x] **6. CSV 파싱 결함** — 따옴표 안 줄바꿈, `""` 이스케이프 미처리, UTF-8 고정(CP949 한글 CSV 깨짐). → 인코딩 감지(MS949 fallback) + 파서 개선(또는 Apache Commons CSV). (d1451d5)

## 🟠 P2 — 성능
- [x] **7. 상세 API가 전체 행 로드** — `getDatasetDetail`이 모든 row를 읽음(프론트는 미사용). → rows 제거.
- [x] **8. 인덱스 없음** — `dataset_row(dataset_id, row_index)`, `dataset_column(dataset_id)` 인덱스 추가.
- [x] **9. 삭제가 건별 DELETE** — 파생 `deleteByDatasetId` → `@Modifying @Query` 벌크 삭제.
- [x] **10. 목록 N+1** — 데이터셋마다 count + 컬럼 전체 조회 → 그룹 count 쿼리로 대체.
- [x] **11. 배치 insert 미동작 + SQL 로그 폭주** — IDENTITY라 saveAll도 1건씩, show-sql/format_sql on. → 로그 off, `JdbcTemplate.batchUpdate` + `rewriteBatchedStatements=true`.
- [x] **12. (후속) 대용량 xlsx 메모리** — `getBytes()` + DOM 방식 파싱. 스트리밍 파서 검토. → xlsx는 SAX 스트리밍(`XlsxRowReader`), 업로드 파일은 임시 파일로 저장해 `byte[]` 제거(CSV 포함), 미리보기는 샘플 10행에서 중단. 힙 512MB 기준 10만 행(5.8MB)에서 메모리 부족이던 것이 30만 행까지 힙 150MB 이내로 처리됨.

## 🟡 P3 — 구조 / 유지보수
- [x] **14. 에러 처리 통일** — 모든 예외가 400, not-found는 500. → `@RestControllerAdvice`.
- [x] **15. 행 CRUD 스키마 검증 없음** — 스키마 밖 키 저장, 타입 미검증. → 컬럼 키 화이트리스트 + 타입 변환/검증.
- [x] **16. UploadTaskService 메모리 누수** — 완료 작업이 맵에서 제거되지 않음. → 종료 10분 뒤 스케줄러가 제거.
- [x] **17. App.jsx 1237줄 단일 파일** — 페이지/모달/상수 분리. 카테고리·타입 옵션 중복, 엑셀 탭 '기타' 누락. → 5-B~5-D로 완료: `constants/` 통합과 '기타' 누락 해결, `MainLayout`·목록·상세·업로드 페이지를 `pages/`와 `components/`로 분리. `App.jsx`는 라우터만 남음(20줄).
- [x] **18. API URL 하드코딩** — Vite `server.proxy` + 상대경로 `/api`.
- [x] **19. antd v6 deprecated API** — `Space direction`, `Alert message`, `Spin tip` 등. → `Divider type`도 함께 교체.
- [x] **20. 미사용 코드** — `/api/upload` 동기 업로드, `uploadDatasetFile`, `UploadRequest` DTO. → 프론트 미사용 `React` import, Vite 템플릿 잔여 파일(`src/assets/*`, `public/icons.svg`)도 제거.
- [x] **21. 테스트 부재** — `contextLoads`만 존재(MySQL 필요). 셀 변환·CSV 파싱·스키마 검증 단위 테스트 추가. → JUnit 5 단위 테스트 54개(DB 불필요) 추가.

---

# 2차 로드맵: 프론트 개편(F) + 모듈 A

진행 순서: F-1 → F-2 → A-1 → A-2 → A-3 → A-4 → 마무리(M). 파트마다 보고 → 승인 → 개발 → 정리·커밋 메시지 절차를 따른다.

## 확정된 설계 결정

### 프론트 화면
- 레이아웃: 상단 바 + 좌측 사이드바. 사이드바는 본문 위에 겹쳐 열리는 오버레이(`Drawer`)이며 기본은 닫힘, 열리면 본문이 어두워져 조작 불가.
- 테마: antd `ConfigProvider` 토큰(`src/theme/theme.js`)으로 통일. 라이트 기본, 다크는 후속 작업(토큰 구조만 대응).
- 홈 대시보드: 보류. `/`는 `/data`로 이동. 사이드바에 "데이터 적재" 메뉴는 두지 않고 목록의 "신규 데이터 적재" 버튼으로만 진입.
- 사이드바 메뉴: "데이터셋" + ADMIN 전용 "관리"(사원·부서·프로젝트). 관리 메뉴는 대상 화면이 생기는 A-4에서 노출.
- 목록 열(최종 목표): 데이터셋명, 프로젝트, 행 수, 소유자(이름만), 등록일, 최근 적재일. 필터: 검색, 프로젝트, "내 것만".
- 분류(category): 프로젝트 도입 전까지 유지하고, 프로젝트 열이 들어간 뒤 화면과 컬럼을 삭제(M-2).
- 적재 화면 본격 재설계는 A-3 이후(M-2). 입력 항목이 A-3에서 바뀌기 때문에 이중 작업을 피하려는 것임.
- 프로젝트 미지정 데이터셋 허용, 화면에는 "미지정"(또는 "기타")으로 표시.
- 최근 적재일: 최신 적재 데이터를 확인하는 용도. 적재 완료와 행 추가 시 갱신하는 안(행 수정·삭제는 제외)이며, F-2 착수 시 확인한다.

### 모듈 A
- 인증: 서버 세션(Spring Security). JWT는 모듈 D에서 필요하면 별도 검토.
- 계정: 관리자 발급(자체 가입 없음). 서버 시작 시 초기 관리자 계정을 시드로 생성. 사번은 일련번호(`E0001`).
- 테이블 4개: `user`, `department`, `project`, `project_member`(프로젝트-사원 N:M 연결, 제1정규형).
  - `user`: emp_no(PK), name, email(UNIQUE, 로그인 아이디, 소문자 정규화), password(BCrypt), dept_code(FK), job_title(enum 검증: 사원/대리/과장/차장/부장/임원), role(ADMIN/USER), status(활성/비활성), created_at 등
  - `department`: dept_code(PK), name
  - `project`: project_code(PK), name
  - `project_member`: project_code(FK), emp_no(FK)
- 권한: 조회는 로그인한 모두 가능(전사 공개). 수정(메타 수정, 행 추가·수정·삭제)은 데이터셋 소유자, 해당 데이터셋 프로젝트의 참여자(`project_member`), ADMIN만 가능. 프로젝트 미지정 데이터셋의 수정은 소유자와 ADMIN만. 데이터셋 자체 삭제는 소유자와 ADMIN만(프로젝트 참여자는 불가, 사용자 확인 필요). 부서·프로젝트는 조회 제한이 아니라 목록 보기용 필터. 사원·부서·프로젝트 관리는 ADMIN 전용. 검사는 서버에서 하며(화면 숨김만으로 막지 않음), 권한 판정은 한 곳(서비스 함수)에 모아 규칙 변경이 쉽게 한다.
- 데이터셋 연결: `dataset_meta`에 owner(사번), project_code(NULL 허용) 추가. 기존 mock(`user_admin`, `org_hq`) 데이터는 초기 관리자로 이관.
- 로그인 오류 문구는 계정 존재 여부가 드러나지 않게 하나로 통일. 시도 횟수 제한 검토.
- 권한 없는 접근은 존재 여부를 숨기도록 404 응답을 검토.

## F. 프론트 개편
- [x] **F-1. 레이아웃·테마** — `theme/theme.js`(테마 토큰), `MainLayout`(상단 바 + 오버레이 사이드바), `SideMenu`, `UserMenu`(mock 사용자), `/` → `/data` 리다이렉트. 기존 세 화면 내용은 그대로.
- [x] **F-2. 목록·상세 개편 (프로젝트·소유자 제외)** — 두 파트로 나눠 진행
  - [x] **F-2-1. 최근 적재일 + 목록** — 백엔드: `dataset_meta.last_loaded_at` 추가, 적재 완료·행 추가 시 갱신(행 수정·삭제 제외), 값이 없으면 등록일로 대체해 목록·상세 응답에 포함, 단위 테스트 추가. 프론트: 공통 `PageTitle`, 이름 검색, 열 정리(열 수 제거, 최근 적재일 추가), 최근 적재일 내림차순 기본 정렬과 열 정렬, 날짜 형식 통일(`utils/formatDate.js`). 실제 DB·브라우저 검증은 승인 후 진행
  - [x] **F-2-2. 상세·적재 화면** — 상세: `Tabs`로 데이터 / 스키마 / 정보 분리(`DatasetDataTab`·`DatasetSchemaTab`·`DatasetInfoTab`, 페이지는 378줄에서 136줄), "메타데이터 정상"·ID 태그 제거, 행 추가 시 최근 적재일 갱신. 적재 화면은 `PageTitle` 적용, 탭 이름을 "엑셀·CSV 파일 적재" / "직접 스키마 설계"로 정리. 브라우저 동작 검증은 사용자 확인 대기
  - 적재 화면: 데이터와 무관한 최소 범위만(엑셀·CSV와 수동 스키마 진입점 구분, 새 레이아웃에 맞는 제목·여백 정리). 1단계 입력 항목(소유자·조직·분류)은 A-3, M-2에서 바뀌므로 건드리지 않음

## A. 모듈 A (권한/소속)
- [ ] **A-1. 모델과 관리 API** — user·department·project·project_member 엔티티, ADMIN 전용 CRUD API, BCrypt, 직급 enum 검증, 초기 관리자·샘플 데이터 시드, 단위 테스트
- [ ] **A-2. 로그인·세션** — Spring Security 세션 인증, 로그인·로그아웃·내 정보 API, 프론트 로그인 화면·라우트 가드, `UserMenu`를 실제 사용자로 교체(mock 제거), 비밀번호 변경
- [ ] **A-3. 데이터셋 소유·권한** — `dataset_meta`에 owner·project_code 추가, mock 이관, 등록자는 세션에서 결정(요청 값 신뢰 금지), 수정 권한 검사(소유자·프로젝트 참여자·ADMIN)와 삭제 권한 검사(소유자·ADMIN)를 판정 함수 한 곳으로 모아 모든 수정 엔드포인트에 적용, 목록·상세 응답에 "수정 가능 여부" 포함(화면 버튼 표시용), 업로드 화면에서 소유자·조직 입력 제거, 프로젝트 선택 추가
- [ ] **A-4. 관리 화면** — 사원·부서·프로젝트·멤버 관리 화면(ADMIN 전용, 사이드바 "관리" 메뉴 노출), 비밀번호 초기화

## M. 마무리
- [ ] **M-1. 목록 프로젝트·소유자 반영** — 프로젝트 열·필터, 소유자 이름 표시, "내 것만" 필터
- [ ] **M-2. 적재 화면 재설계 + 분류 제거** — A-3 이후 진행(소유자·조직 입력 제거, 프로젝트 선택 추가가 반영된 상태에서 한 번에 설계). 1단계 메타 입력 폼, 2단계 스키마 검토, 3단계 진행 화면의 흐름을 정리하고, 분류는 화면(업로드·수정·수동 스키마·목록)과 `category` 컬럼, `constants/category.js`를 함께 삭제. 착수 전 현재 화면의 불편한 점을 사용자에게 확인하고 필요하면 목업으로 먼저 제시
- [ ] **M-3. 다크 모드** — 전환 버튼, 페이지 내 하드코딩 색상을 토큰으로 교체
- [ ] **M-4. (보류) 홈 대시보드** — 통계·최근 데이터셋. 요약 API 필요

## 검증 시나리오
1. 수식/날짜/정수 포함 xlsx 업로드 → 그리드 값 확인
2. CP949 CSV 업로드 → 한글 정상
3. 중복 헤더 파일 → Step1에서 이름 수정 후 적재 가능
4. 수천 행 업로드 → 진행률 정상 완료, 상세/삭제 속도
5. 행 추가/수정/삭제
