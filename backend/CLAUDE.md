# Backend 규칙 (Spring Boot)

> `backend/` 하위 파일을 다룰 때 자동으로 읽힌다. 공통 규칙은 루트 `CLAUDE.md` 참고.

## 환경
- Spring Boot 3.3.4, Java 17 (toolchain), Gradle wrapper, MySQL, JPA, Apache POI, springdoc-openapi, Lombok
- 빌드/실행: `./gradlew compileJava`, `./gradlew bootRun` (DB 접속 정보는 `application-local.yml` 또는 환경변수)

## 패키지 구조
- `config` / `controller` / `service` / `domain` / `repository` / `dto` (패키지: `com.dgjs.backend`)
- 새 기능도 이 패키지에 추가한다. 기능별 하위 패키지 분리는 하지 않는다 (프로토타입 단계).

## 데이터 저장 원칙
- 사용자별 `CREATE TABLE` 금지 — `dataset_meta` / `dataset_column` / `dataset_row(data_json)` 구조 유지
- 엔티티와 API에 항상 `owner_id`, `org_id` 상정 (현재 mock: `user_admin`, `org_hq`)
- 1,000행 이상 대용량 적재는 비동기 처리
- 스키마 변경은 엔티티 수정(`ddl-auto: update`)으로 처리한다 (프로토타입 한정 — 모듈 A 구현 시 마이그레이션 도구 도입 여부와 함께 재검토). 인덱스는 `@Table(indexes = ...)`로 선언한다.
- 대량 insert/delete는 건별 처리 금지 — 벌크 쿼리(`@Modifying @Query`) 또는 `JdbcTemplate.batchUpdate` 사용.

## 레이어 / 코드 컨벤션
- **Controller**: 요청 파싱·검증 위임·응답 생성만 한다. 비즈니스 로직과 `try/catch` 변환은 두지 않는다 (예외는 `@RestControllerAdvice`가 처리).
- **Service**: 비즈니스 로직과 트랜잭션(`@Transactional`) 경계. 조회 전용은 `readOnly = true`.
- **Repository**: Spring Data JPA 인터페이스. 복잡한 쿼리는 `@Query`.
- **Entity(`domain`)는 API로 직접 반환하지 않는다.** 반드시 DTO로 변환한다.
- **DTO**: 도메인별 외부 클래스(`DatasetDto`) 안에 `static` 중첩 클래스로 둔다. 이름은 `XxxRequest` / `XxxResponse` / `SummaryResponse`.
- **Lombok**: 엔티티는 `@Getter @NoArgsConstructor @Builder` 중심, DTO는 `@Data @Builder @NoArgsConstructor @AllArgsConstructor`. 의존성 주입은 `@RequiredArgsConstructor` + `private final` 필드 (필드 `@Autowired` 금지).
- **URL**: `/api/{리소스 복수형}` + REST 동사 (`GET/POST/PUT/DELETE`). 새 엔드포인트에는 springdoc `@Operation(summary = "...")`을 단다.
- **페이징**: 목록 API는 `page`/`size`(`Pageable`)를 받는다. 전체 로드(`findAll`) 금지.
- **비동기**: `@Async` 메서드 안에서 작업 상태를 초기화하지 않는다 — 작업 ID/상태는 호출 전에 동기 구간(Controller)에서 먼저 등록한다 (기존 코드에 이 결함이 있음).
- **주석**: 한국어로, "왜"가 필요한 곳에만 쓴다. 자명한 코드에 주석을 달지 않는다.
- **시크릿**: 접속 정보·키를 코드나 `application.yml`에 하드코딩하지 않는다.

## 예외 처리 / 응답 형식
- 전역 처리는 `@RestControllerAdvice`(`config` 패키지)에서 한다.
- 상태 코드 매핑
  - 입력/검증 오류 → `400`
  - 대상 없음 → `404` (`500`으로 새지 않게 한다)
  - 서버 내부 오류 → `500`
- 오류 응답 본문은 `{ "message": "사용자에게 보여줄 한국어 메시지" }` 로 통일한다 (프론트가 `message`를 그대로 표시).
- 예외 메시지에 SQL, 스택트레이스, 내부 경로를 노출하지 않는다.
- 성공 응답은 별도 래퍼 없이 DTO를 `ResponseEntity.ok(...)`로 반환한다.
- 기존 컨트롤러의 `catch (Exception e) → badRequest` 패턴은 신규 코드에서 쓰지 않는다 (기존 코드는 추후 정리 예정).

## 테스트 / 검증
- 코드 수정 후 `./gradlew compileJava`로 컴파일 확인은 필수. 로직 변경이면 `./gradlew test`도 실행한다.
- 순수 로직(셀 값 변환, CSV 파싱, 스키마 검증)은 DB 없이 도는 JUnit 5 단위 테스트로 작성한다.
- DB가 필요한 테스트(`@SpringBootTest`)는 최소화한다. 실행에 MySQL이 필요하면 그 사실을 보고에 명시한다.
- 컴파일, lint, build, test, 개발 서버 기동은 확인 없이 직접 실행해 검증한다. 시작한 서버·프로세스는 검증 후 종료한다.
- 실제 DB를 건드리는 검증(업로드·행 추가/수정/삭제 등)은 **먼저 사용자에게 무엇을 어떻게 할지 보고하고, 사용자가 명확히 승인한 뒤에만** 진행한다. 승인받은 경우에도 새로 만든 테스트 데이터에만 수행하고 기존 데이터셋은 수정·삭제하지 않으며, 끝나면 만든 데이터를 정리하고 결과를 보고한다.
- 실제 DB/브라우저 동작을 검증하지 못했으면 "검증 못함"이라고 그대로 보고한다.
- 구조·API·흐름이 바뀌면 `docs/ARCHITECTURE.md`를, 로드맵 항목을 끝내면 `docs/ROADMAP.md`를 함께 갱신한다.
