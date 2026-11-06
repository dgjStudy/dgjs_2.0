# DGJS 2.0

엑셀·CSV 같은 정형 데이터를 올려 웹에서 조회하고 편집하며, 사원·부서·프로젝트 단위로 소유와 권한을 관리하는 경량 데이터 플랫폼입니다. 지자체 사업을 수주하는 가상의 SI/SM 업체 DGJS의 사내 데이터 플랫폼을 가정해 만든 프로토타입입니다.

<!-- 배포 후 여기에 데모 주소와 읽기용 계정 안내를 추가 -->
<!-- 화면 캡처 3~4장(로그인, 적재, 그리드, 관리 화면)을 docs/images/에 두고 여기에 연결 -->

## 주요 기능

- **데이터 적재**: 엑셀(xlsx, xls)과 CSV 업로드, 컬럼 타입 추정과 수정, 수동 스키마 생성. 대용량 파일은 비동기로 적재하고 진행률을 표시
- **조회·편집**: 서버 페이징 그리드, 컬럼 표시 선택, 행 추가·수정·삭제(타입별 검증)
- **사원·부서·프로젝트 관리**: 관리자가 계정을 발급하고, 임시 비밀번호는 한 번만 표시
- **로그인·세션**: 이메일과 비밀번호 로그인, 서버 세션, CSRF 보호, 연속 실패 시 잠금
- **소유·권한**: 데이터셋은 소유자와 프로젝트를 가지며, 수정은 소유자·프로젝트 참여자·관리자만 가능(조회는 로그인한 모두 가능). 모든 판정은 서버에서 수행

## 기술 스택

| 구분 | 사용 기술 |
|---|---|
| Backend | Java 17, Spring Boot 3.3.4, Spring Security, Spring Data JPA, Apache POI, springdoc(Swagger UI) |
| Database | MySQL |
| Frontend | React 19, Vite, Ant Design 6, React Router 7 |

## 설계 요점

- **사용자별 `CREATE TABLE`을 하지 않습니다.** 메타데이터(`dataset_meta`, `dataset_column`)와 행 데이터(`dataset_row`, JSON 저장)를 공통 테이블 3개에 나눠 저장해, 데이터셋이 늘어도 스키마가 변하지 않습니다.
- **대용량 xlsx도 메모리 사용량이 일정합니다.** SAX 스트리밍으로 읽고 업로드 파일은 임시 파일로 처리합니다.
- **권한 판정은 한 곳에 모읍니다.** `DatasetAccessPolicy`가 수정·삭제·프로젝트 연결 규칙을 담당해 규칙이 바뀌어도 수정 지점이 하나입니다.
- **데이터셋 소유자는 요청 값이 아니라 로그인 사용자로 서버가 정합니다.**

구조, 데이터 모델, API, 주요 흐름은 [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)에 정리되어 있습니다.

## 로컬 실행

사전 준비: JDK 17, Node.js, MySQL(`dgjs2_db` 스키마)

1. MySQL에 `dgjs2_db` 스키마를 만듭니다. 테이블은 서버 시작 시 자동 생성됩니다.
2. DB 접속 정보를 설정합니다. 아래 중 하나를 사용합니다.
   - `backend/src/main/resources/application-local.yml.example`을 `application-local.yml`로 복사해 값을 입력(git에 올라가지 않음)
   - 환경변수 `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`
3. 백엔드와 프론트를 실행합니다.

   Windows PowerShell에서는 프로젝트 루트에서 한 번에 실행할 수 있습니다.

   ```powershell
   .\dev.ps1
   ```

   개별 실행은 다음과 같습니다.

   ```bash
   cd backend && ./gradlew bootRun
   cd frontend && npm install && npm run dev
   ```

4. 접속 주소
   - 앱: http://localhost:5173
   - Swagger UI: http://localhost:8080/swagger-ui.html

### 초기 관리자

사원이 한 명도 없는 상태로 서버를 처음 시작하면 초기 부서와 관리자가 생성됩니다. 기본 이메일은 `admin@dgjs.local`이고, 비밀번호는 서버 로그에 한 번만 출력됩니다(`.dev-logs\backend.log`). 첫 로그인 때 비밀번호를 바꾸게 됩니다. 이메일과 비밀번호는 `application-local.yml`의 `app.seed.*`로 바꿀 수 있습니다.

### 테스트

```bash
cd backend && ./gradlew test --tests 'com.dgjs.backend.service.*' --tests 'com.dgjs.backend.config.*' --tests 'com.dgjs.backend.domain.*'
cd frontend && npm run lint && npm run build
```

`contextLoads` 테스트는 MySQL 연결이 필요해 위 명령에서 제외했습니다. 화면과 API를 직접 확인하는 절차는 [docs/QA_CHECKLIST.md](docs/QA_CHECKLIST.md), 샘플 파일은 [docs/testdata](docs/testdata)에 있습니다.

## 프로젝트 구조

```
dgjs_2.0/
├─ backend/    Spring Boot 서버
├─ frontend/   React 클라이언트
├─ docs/       아키텍처, 로드맵, QA 체크리스트, 용어집, 테스트 데이터
└─ dev.ps1     개발 서버 일괄 실행·종료 스크립트
```

## 개발 현황

| 모듈 | 내용 | 상태 |
|---|---|---|
| B | 데이터 적재·스키마 | 완료 |
| C | 조회·그리드 | 완료 |
| A | 사원·부서·프로젝트, 로그인, 소유·권한, 관리 화면 | 구현 완료, 점검 마무리 중 |
| D | 외부 연계 API | 예정 |

세부 진행 상황과 개선 목록은 [docs/ROADMAP.md](docs/ROADMAP.md)를 참고하세요.
