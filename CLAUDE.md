# DGJS 2.0 Platform (Prototype)

정형 데이터(엑셀, CSV, 수동 스키마)를 적재하고, 웹에서 조회/시각화하며, 외부 시스템과 API로 연계하는 경량 데이터 플랫폼.

- 파이프라인: [모듈 0: .exe 수집기] → [모듈 B: 적재/스키마] → [모듈 C: 시각화/그리드] → [모듈 A: 권한/소속] → [모듈 D: 연계 API]
- 현재 단계: **Core MVP (모듈 B + C) 완료, 모듈 A(권한/소속) 진행 중**(사원·부서·프로젝트, 로그인·세션 구현, 데이터셋 소유·권한과 관리 화면 남음). 외부 연계 API(D)는 후순위.
- 프로젝트 구조·데이터 모델·API·흐름: [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)
- 개선 작업 목록(P1~P3)과 진행 상황: [docs/ROADMAP.md](docs/ROADMAP.md)
- 스택별 세부 규칙: `backend/CLAUDE.md`, `frontend/CLAUDE.md` (해당 폴더 작업 시 자동 적용)
- 강제 규칙(git 쓰기 명령 차단 등): `.claude/settings.json`

## 실행 방법

### 한 번에 실행 (Windows PowerShell, 프로젝트 루트)
- `.\dev.ps1` — 백엔드와 프론트를 별도 창 없이 백그라운드로 실행하고, 준비되면 알려준다. 로그는 `.dev-logs\backend.log`, `.dev-logs\frontend.log`에 기록된다(git 무시됨). 실시간으로 보려면 `Get-Content .dev-logs\backend.log -Wait`.
- 실행 전 점검: MySQL 서비스 상태, `JAVA_HOME`(없으면 `~\.jdks`의 JDK 17 사용), `frontend\node_modules`, 8080/5173 포트 사용 여부.
- 옵션: `-Backend`, `-Frontend`(한쪽만), `-Window`(별도 Windows Terminal 탭에 실행해 로그를 직접 봄), `-NoWt`(`-Window`와 함께 쓰며 PowerShell 창), `-Stop`(8080/5173의 java·node 프로세스 종료, `-Backend`·`-Frontend`와 함께 쓰면 한쪽만).
- 아래 개별 실행 방법도 그대로 사용할 수 있다.

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

## 규칙
- **언어**: 사용자와의 모든 대화·설명·질문·작업 보고는 **한국어**로 한다. (코드 식별자, 명령어, 로그 원문은 그대로 둔다.)
- **작업 절차**: 본격적인 개발 전에 사용자와 상의한 내용을 바탕으로 개발할 내용을 전체적으로 정리해 보고하고, 사용자가 승인한 범위에 대해서만 개발을 진행한다.
  - 개발 범위(분량) 판단은 Claude가 한다. 내용이 너무 길면 Claude가 적절한 파트로 나눠 제안한다.
  - 파트별 순서: **보고 → 승인 → 개발 → 완료 후 개발된 사항 정리 및 커밋 메시지 작성**. 한 파트가 끝나고 다음 파트를 시작하기 전에 다시 보고·승인을 받는다.
  - 로드맵 항목을 작업할 때는 파트를 항목(또는 같은 P 등급 묶음) 단위로 나누고, 파트 완료 시 `docs/ROADMAP.md`를 `[x]`로 갱신한다. 이미 해결됐거나 현재 코드와 맞지 않는 항목은 수정하거나 넘기고 그 사실을 보고한다.
  - 승인 없이 범위를 넓히지 않는다. 진행 중 범위 밖 작업이 필요해 보이면 멈추고 먼저 제안한다.
  - 한두 줄 수정이나 질문 응답처럼 사소한 작업은 이 절차 없이 바로 처리한다.
- **Git**: 모든 git 쓰기 명령(add, commit, push 등)은 사용자가 직접 실행한다. Claude는 파일 수정 후 변경 파일 목록과 커밋 메시지 제안만 한다.
  - git 명령이 필요한 상황(커밋, 푸시, 브랜치, 충돌 해결, 되돌리기 등)이면 명령어만 던지지 말고, 이 프로젝트 구조(`backend/`, `frontend/`, `docs/`)와 현재 `git status` 상황에 맞춰 **실행 순서대로 단계별 안내**한다.
  - 각 단계는 명령어(코드 블록 하나에 한 명령) + 그 단계가 하는 일 한 줄. 되돌리기 어려운 명령(`reset --hard`, `push --force` 등)은 먼저 위험을 알리고 대안을 제시한다.
  - 커밋 메시지 제안에 `Co-Authored-By` 줄을 넣지 않는다 (사용자 요청, 추후 변경 가능).
  - **커밋 메시지 형식**: Conventional Commits `type: 한국어 요약` (기존 이력과 동일). type은 `feat` / `fix` / `docs` / `chore` / `refactor` / `test` / `style` / `perf`. 필요하면 scope를 쓴다 (`feat(backend): ...`). 요약은 한 줄, 마침표 없이 쓴다.
  - 커밋 메시지는 **긴 버전**(요약 + 변경 항목 상세 목록)과 **짧은 버전**(핵심만 1~2줄)을 함께 제공하고, 개발 범위에 따라 어느 쪽이 더 적합한지 이유와 함께 추천한다 (큰 기능·여러 파일 변경은 긴 버전, 사소한 수정·문서 변경은 짧은 버전).
  - 커밋은 가능하면 변경 성격별(backend / frontend / docs)로 나눠 add 대상을 구체적 경로로 지정하도록 안내한다.
- **기존 코드 처리**: 하위 `CLAUDE.md`의 규칙은 새 코드와 수정하는 코드의 목표 상태이며, 기존 코드는 아직 이를 따르지 않는 곳이 있다. 요청 없이 기존 코드를 대규모로 리팩터링하지 않고, 작업 중 건드리는 범위에서만 점진적으로 맞춘다. 규칙과 기존 코드가 충돌하면 임의로 판단하지 말고 어느 쪽을 따를지 사용자에게 먼저 묻는다.
- **Backend**: Spring Boot 3.x, JPA (MyBatis 허용), MySQL. 1,000행 이상 대용량 적재는 비동기 처리.
- **데이터 저장**: 사용자별 `CREATE TABLE` 금지. 메타데이터 테이블과 JSON 데이터 적재 테이블 분리 전략을 유지한다.
- **Frontend Styling**
  - 별도 CSS/SCSS 파일 작성 금지.
  - 모든 UI는 antd 컴포넌트(Table, Form, Upload, Card, message 등) + 인라인 `style` prop으로만 해결.
  - 순수 HTML 태그보다 antd 컴포넌트 우선.
- **데이터셋 소유**: 데이터셋은 항상 소유자(`owner_id`, 사번)와 선택적 프로젝트(`project_code`)를 가진다. 소유자는 요청 값이 아니라 로그인 사용자(세션)로 서버가 정한다. 조직은 소유자의 부서로 대신하며 `org_id`는 쓰지 않는다.
- **보안**: DB 비밀번호 등 시크릿은 절대 커밋하지 않는다 (`application-local.yml` 또는 환경변수 사용).
- **gitignore**: git에 올라가면 안 되는 파일(시크릿, 로컬 설정, 빌드 산출물, 임시·로그 파일 등)을 새로 만들어야 하면, 만들 때 해당 경로를 `.gitignore`에 함께 반영하고 작업 보고에 알린다. 예제 파일(`*.example`)을 같이 만드는 것을 우선 검토한다.
- **코딩 스타일**: 장황한 이론 설명보다 즉시 실행 가능한 코드 위주. 컴포넌트/API 단위로 모듈화.
  - 정말 중요한 부분이 아닌 한 주석을 넣는 것은 삼가한다.
  - 주석을 넣을 때에는 `~임`, `~를 의미` 등으로 서술하며, `~한다` 식의 서술은 금지한다.
  - 이모지나 가벼운 말투는 절대 사용하지 않는다 (코드, 주석, 문서, 보고 모두 해당).
- **문서**: 구조·API·흐름이 바뀌면 `docs/ARCHITECTURE.md`도 함께 갱신한다.
- **진척도 반영**: 세부 진행 상황은 `docs/ROADMAP.md`에만 기록한다 (항목 완료 시 `[x]`). 이 파일의 "현재 단계"는 단계 자체가 바뀔 때(예: MVP 종료, 모듈 A/D 착수)만 한 줄 갱신하고, 작업 보고에 알린다.
