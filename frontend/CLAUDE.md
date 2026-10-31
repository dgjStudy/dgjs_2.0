# Frontend 규칙 (React + antd)

> `frontend/` 하위 파일을 다룰 때 자동으로 읽힌다. 공통 규칙은 루트 `CLAUDE.md` 참고.

## 환경
- React 19, Vite, antd 6, @ant-design/icons, react-router-dom 7, JavaScript/JSX (TypeScript 아님)
- 실행/검증: `npm run dev`, `npm run lint` (oxlint), `npm run build`

## 스타일링
- 별도 CSS/SCSS 파일 작성 금지
- 모든 UI는 antd 컴포넌트 + 인라인 `style` prop으로만 해결
- 순수 HTML 태그보다 antd 컴포넌트 우선
- 반복되는 스타일 객체는 컴포넌트 파일 상단 상수로 뽑는다 (CSS 파일 대신).

## 파일 / 컴포넌트 구조
- `src/` 구성
  - `pages/` — 라우트 단위 페이지 (`DatasetListPage`, `DatasetDetailPage`, `UploadPage`)
  - `components/` — 재사용 컴포넌트·모달
  - `api/` — 백엔드 호출 함수
  - `constants/` — 카테고리·컬럼 타입 옵션 등 공용 상수 (중복 정의 금지)
  - `utils/` — 날짜 형식 변환 등 UI와 무관한 순수 함수 (`formatDate.js`)
  - `auth/` — 현재 로그인 사용자 정보 컨텍스트(`AuthProvider`, `useAuth`). 인증 화면·라우트 가드는 `pages/`·`components/`에 둠
  - `theme/` — antd 테마 토큰(`theme.js`). 색·모서리 같은 공통 값은 하드코딩하지 말고 `theme.useToken()`으로 읽는다
- 컴포넌트 파일은 PascalCase.jsx, 함수·변수는 camelCase, 훅은 `use` 접두사. 컴포넌트 파일당 `export default` 하나.
- 파일 하나가 길어지면(권장 기준 약 300줄) 분리를 먼저 검토한다. `App.jsx`에 새 코드를 더 쌓지 않는다.
- 라우트: `/login`(레이아웃 없음), `/data`, `/data/:id`, `/upload`, `/me`. 새 페이지는 라우터에 등록한다. `/login` 외에는 모두 `RequireAuth` 아래에 둔다.
- npm 패키지는 사용자 승인 없이 추가하지 않는다.

## API 호출
- 백엔드 호출은 `src/api/` 의 함수로만 한다 (컴포넌트에서 직접 fetch 금지)
- `src/api/`의 함수는 `fetch`를 직접 쓰지 않고 `apiClient.js`의 `apiFetch`/`toError`를 쓴다. 세션 쿠키 전달, CSRF 헤더 첨부, 401 처리가 여기서 이루어진다.
- Base URL을 하드코딩하지 않는다. Vite `server.proxy`로 `/api`를 백엔드(8080)에 넘기고 상대경로 `/api/...`를 쓴다.
- API 함수는 실패 시 `Error`를 throw한다. 백엔드 오류 응답에 `message`가 있으면 그것을 `Error.message`로 쓴다.
- 호출하는 쪽(페이지/컴포넌트)에서 `try/catch` 후 antd `message.error(...)`로 사용자에게 알린다. 빈 `catch` 금지.

## 상태 관리 / antd 사용 규칙
- 전역 상태 라이브러리는 쓰지 않는다. `useState`/`useReducer` + props로 해결하고, 서버 데이터는 페이지에서 로드해 내려준다.
  - 예외: 현재 로그인 사용자 정보에 한해 React Context(`auth/`)를 쓴다. 다른 값은 Context로 늘리지 않는다.
- 폼은 antd `Form` + `Form.useForm()`을 사용한다 (직접 `onChange` 상태 관리 지양).
- 그리드는 antd `Table`의 서버사이드 페이징(`pagination` + `onChange`)을 사용한다. 전체 행을 한 번에 받지 않는다.
- 로딩은 `loading`/`Spin`, 알림은 `message`/`notification`/`Modal.confirm`으로 처리한다 (`alert()`, `confirm()` 금지).
- 폴링은 언마운트 시 반드시 `clearInterval`/`clearTimeout`으로 정리한다.
- **antd v6 deprecated API 금지** — 새 코드와 수정하는 코드에서 쓰지 않는다 (설치된 antd 6.6.5 타입 정의에서 deprecated 확인됨).
  - `Space`의 `direction` → `orientation`
  - `Alert`의 `message` → `title`
  - `Spin`의 `tip` → `description`
  - 그 외 의심되면 `node_modules/antd/es/<컴포넌트>/*.d.ts`의 `@deprecated` 표기로 확인한다.
- 검증: 수정 후 `npm run lint`와 `npm run build`가 통과해야 한다. 브라우저에서 직접 확인하지 못했으면 그 사실을 보고한다.
