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
- [ ] **12. (후속) 대용량 xlsx 메모리** — `getBytes()` + DOM 방식 파싱. 스트리밍 파서 검토.

## 🟡 P3 — 구조 / 유지보수
- [x] **14. 에러 처리 통일** — 모든 예외가 400, not-found는 500. → `@RestControllerAdvice`.
- [x] **15. 행 CRUD 스키마 검증 없음** — 스키마 밖 키 저장, 타입 미검증. → 컬럼 키 화이트리스트 + 타입 변환/검증.
- [x] **16. UploadTaskService 메모리 누수** — 완료 작업이 맵에서 제거되지 않음. → 종료 10분 뒤 스케줄러가 제거.
- [ ] **17. App.jsx 1237줄 단일 파일** — 페이지/모달/상수 분리. 카테고리·타입 옵션 중복, 엑셀 탭 '기타' 누락. → 진행: 5-B 완료(상수 `constants/` 통합과 '기타' 누락 해결, `MainLayout`·`DatasetListPage` 분리). 남은 것: 상세 페이지(5-C), 업로드 페이지(5-D).
- [x] **18. API URL 하드코딩** — Vite `server.proxy` + 상대경로 `/api`.
- [x] **19. antd v6 deprecated API** — `Space direction`, `Alert message`, `Spin tip` 등. → `Divider type`도 함께 교체.
- [x] **20. 미사용 코드** — `/api/upload` 동기 업로드, `uploadDatasetFile`, `UploadRequest` DTO. → 프론트 미사용 `React` import, Vite 템플릿 잔여 파일(`src/assets/*`, `public/icons.svg`)도 제거.
- [x] **21. 테스트 부재** — `contextLoads`만 존재(MySQL 필요). 셀 변환·CSV 파싱·스키마 검증 단위 테스트 추가. → JUnit 5 단위 테스트 54개(DB 불필요) 추가.

## 검증 시나리오
1. 수식/날짜/정수 포함 xlsx 업로드 → 그리드 값 확인
2. CP949 CSV 업로드 → 한글 정상
3. 중복 헤더 파일 → Step1에서 이름 수정 후 적재 가능
4. 수천 행 업로드 → 진행률 정상 완료, 상세/삭제 속도
5. 행 추가/수정/삭제
