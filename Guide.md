# 문서 뷰어 앱 (PDF / DOCX / HWP)

Android용 보기 전용 문서 뷰어. Google Play 출시가 목표다.
이 파일은 Claude Code가 작업할 때 따르는 프로젝트 지침이자 작업 계획이다.

## 1. 제품 범위

### 1차 출시에 포함
- 지원 포맷: PDF, DOCX, HWP, HWPX
- 홈 화면: 전체/포맷별 탭, 최근 본 문서, 즐겨찾기, 파일명 검색, 정렬(이름/날짜/크기)
- 뷰어 공통: 확대/축소, 페이지 이동, 문서 내 텍스트 검색, 공유, 다크 모드
- 다른 앱에서 "연결 프로그램"으로 열기 (ACTION_VIEW 인텐트 필터)
- 오픈소스 라이선스 화면
- 완전 오프라인 동작 (문서를 외부 서버로 보내지 않는다)

### 1차 출시에서 제외 (만들지 말 것)
- 모든 편집 기능 (텍스트 수정, 주석, 서명)
- 포맷 변환, 스캔, 템플릿
- 구형 DOC, XLS/XLSX, PPT/PPTX
- 로그인, 클라우드 동기화
- MANAGE_EXTERNAL_STORAGE 권한

요청받지 않은 기능은 추가하지 않는다. 범위가 애매하면 먼저 질문한다.

## 2. 기술 스택

| 영역 | 선택 |
|---|---|
| 언어 / UI | Kotlin, Jetpack Compose, Material 3 |
| 구조 | 멀티모듈, MVVM, 단방향 데이터 흐름 (StateFlow) |
| DI | Hilt |
| 비동기 | Coroutines, Flow |
| 로컬 DB | Room (최근 문서, 즐겨찾기) |
| 설정 저장 | DataStore |
| 내비게이션 | Navigation Compose (타입 세이프 라우트) |
| 빌드 | Gradle Kotlin DSL, Version Catalog (`gradle/libs.versions.toml`) |
| SDK | minSdk 26, targetSdk와 compileSdk는 현재 최신 안정 버전 |

### 포맷별 렌더링 엔진
- **PDF**: Jetpack `androidx.pdf` (`pdf-viewer-fragment`). 작성 시점 기준 1.0.0-beta01.
- **HWP / HWPX**: rhwp의 `@rhwp/core` (Rust + WASM 파서/렌더러)를 assets에 번들해 WebView에서 실행.
- **DOCX**: JS 기반 DOCX 렌더러(후보: `docx-preview`)를 assets에 번들해 같은 WebView 호스트에서 실행.

### 라이브러리 사용 규칙
- 위 버전과 API는 이 문서 작성 시점 정보다. **코드를 쓰기 전에 공식 문서와 저장소에서 최신 버전, 실제 API, 최소 지원 SDK를 확인한다.** 기억에 의존해 API를 추측하지 않는다.
- `androidx.pdf`가 minSdk 26 전체를 지원하지 않으면, 미지원 기기에서는 플랫폼 `PdfRenderer` 기반의 단순 뷰어로 대체한다.
- 새 의존성을 추가하기 전에 라이선스를 확인한다. **GPL, AGPL 라이선스는 금지** (예: MuPDF, iText). Apache 2.0, MIT, BSD만 허용.
- 추가한 모든 서드파티 라이브러리는 `docs/THIRD_PARTY.md`에 이름, 버전, 라이선스, 출처를 기록한다.
- JS/WASM 번들은 CDN에서 불러오지 않는다. 반드시 assets에 넣어 로컬에서 로딩한다.

## 3. 모듈 구조

```
:app                 진입점, 내비게이션 그래프, 테마, 인텐트 처리
:core:model          DocumentFile, DocumentType 등 공용 모델
:core:data           Room, DataStore, 리포지토리
:core:files          SAF 폴더 접근, 파일 스캔, MIME/확장자 판별
:core:ui             공용 Composable, 디자인 토큰
:feature:home        파일 목록, 탭, 검색, 정렬
:feature:settings    설정, 오픈소스 라이선스
:viewer:pdf          androidx.pdf 래핑 화면
:viewer:web          공용 WebView 호스트, JS 브리지
:viewer:hwp          rhwp 번들, HWP 뷰어 화면
:viewer:docx         DOCX 렌더러 번들, DOCX 뷰어 화면
```

의존 방향: `app → feature / viewer → core`. feature와 viewer 모듈끼리는 서로 의존하지 않는다.
`:viewer:hwp`와 `:viewer:docx`만 `:viewer:web`에 의존한다.

## 4. 핵심 설계 결정

### 파일 접근
- SAF(`ACTION_OPEN_DOCUMENT_TREE`)로 사용자가 고른 폴더만 읽는다. `takePersistableUriPermission`으로 권한을 유지한다.
- 파일은 경로가 아니라 `content://` Uri로 다룬다.
- 외부 앱에서 넘어온 Uri는 권한이 일시적일 수 있으므로, 최근 문서에 기록할 때 권한 유지 가능 여부를 확인한다.
- 포맷 판별은 확장자와 MIME 타입을 함께 본다. HWP는 MIME이 일정하지 않으므로 확장자와 파일 시그니처로 보완한다.

### WebView 호스트 (`:viewer:web`)
- `WebViewAssetLoader`로 `https://appassets.androidplatform.net/` 아래에서 assets를 로딩한다.
- 문서 파일은 base64로 JS에 넘기지 않는다. `WebViewAssetLoader`의 커스텀 `PathHandler`로 Uri의 InputStream을 스트리밍하고, JS 쪽에서 `fetch`로 받는다.
- 보안 설정: `allowFileAccess = false`, `allowContentAccess = false`, 외부 네트워크 요청 차단, 외부 링크 내비게이션 차단.
- JS 브리지(`addJavascriptInterface`)는 최소한의 메서드만 노출한다: 로딩 완료, 페이지 수, 현재 페이지, 오류 보고.
- 호스트는 포맷을 모른다. 포맷별 모듈이 자신의 HTML/JS 번들 경로만 넘긴다.
- rhwp의 WASM은 WebView 96 이상이 필요하다. HWP/HWPX를 열기 전에 WebView 버전을 확인하고, 미만이면 업데이트 안내와 "다른 앱으로 열기"를 보여준다.

### Compose와 View 상호운용
- `androidx.pdf`와 WebView는 `AndroidView` 또는 `AndroidFragment`로 감싼다.
- 화면 회전과 프로세스 종료 후에도 읽던 위치가 복원되어야 한다.

### 폰트
- HWP 문서는 기기에 없는 한글 폰트를 자주 참조한다. 재배포가 허용된 한글 폰트(예: Noto Sans KR, 나눔 계열)를 번들하고 대체 매핑 표를 둔다.
- 폰트를 번들하기 전에 재배포 라이선스를 확인하고 `docs/THIRD_PARTY.md`에 기록한다.

## 5. 작업 계획

마일스톤 순서대로 진행한다. 각 마일스톤은 완료 기준을 모두 만족해야 끝난 것이다.
한 마일스톤이 끝나면 멈추고 결과를 보고한 뒤 다음으로 넘어간다.

### M0. HWP 렌더링 검증 (가장 먼저)
이 앱의 최대 위험이므로 뼈대보다 먼저 확인한다.
- 단일 모듈의 최소 앱에서 WebView + `@rhwp/core`로 assets에 넣은 샘플 HWP 1개를 화면에 띄운다.
- 완료 기준
  - 샘플 HWP와 HWPX가 각각 렌더링된다.
  - 네트워크를 끈 상태에서 동작한다.
  - 최신 API 에뮬레이터에서 동작한다. API 26에서는 WebView가 최소 버전(96) 이상이면 렌더링하고, 미만이면 크래시 없이 WebView 업데이트 안내를 보여준다. (2026-10-07 결정, 근거: `docs/hwp-spike.md`)
  - 문서 로딩 시간과 메모리 사용량을 `docs/hwp-spike.md`에 기록한다.
- 동작하지 않으면 원인과 대안을 정리해 보고하고 멈춘다. 임의로 다른 엔진으로 바꾸지 않는다.

### M1. 프로젝트 뼈대
- 3장의 모듈 구조, Version Catalog, Hilt, 테마(라이트/다크), 내비게이션 그래프.
- 완료 기준: `./gradlew assembleDebug` 성공, 빈 홈 화면이 뜬다.

### M2. 파일 목록 (홈)
- 폴더 선택, 지원 포맷 스캔, 탭/검색/정렬, 최근 문서와 즐겨찾기(Room).
- 완료 기준: 폴더를 고르면 지원 포맷 파일만 목록에 나오고, 앱을 재시작해도 폴더 권한과 최근/즐겨찾기가 유지된다. 빈 상태와 권한 없음 상태 화면이 있다.

### M3. PDF 뷰어
- `androidx.pdf` 연결, 페이지 표시, 확대/축소, 문서 내 검색, 공유.
- 완료 기준: 100쪽 이상 PDF가 끊김 없이 스크롤되고, 암호 걸린 PDF와 손상된 PDF에서 크래시 없이 안내가 나온다.

### M4. 공용 WebView 호스트
- 4장의 설계대로 `:viewer:web` 구현, M0의 프로토타입 코드를 여기로 옮긴다.
- 완료 기준: 20MB 이상 파일을 OOM 없이 넘길 수 있고, 외부 네트워크 요청이 차단되는 것을 테스트로 확인한다.

### M5. HWP 뷰어
- rhwp 번들 연결, 페이지 이동, 확대/축소, 폰트 대체.
- 완료 기준: 표, 이미지, 수식이 포함된 샘플 문서 5개 이상이 렌더링되고, 실패한 문서는 오류 화면과 "다른 앱으로 열기"를 제공한다.

### M6. DOCX 뷰어
- JS 렌더러 번들 연결. 후보 라이브러리의 최신 상태와 라이선스를 먼저 확인하고 선택 근거를 보고한다.
- 완료 기준: 표, 이미지, 머리글/바닥글이 포함된 샘플 문서 5개 이상이 렌더링된다.

### M7. 출시 준비
- ACTION_VIEW 인텐트 필터(PDF, DOCX, HWP, HWPX의 MIME과 확장자), 오픈소스 라이선스 화면, R8 설정, 릴리스 빌드 서명 구성, 앱 아이콘.
- 완료 기준: 파일 관리자 앱에서 네 가지 포맷 모두 이 앱으로 열리고, 릴리스 빌드가 R8 적용 상태에서 정상 동작한다.

## 6. 코드 규칙

- UI 문자열은 `strings.xml`에 둔다. 기본 언어는 한국어, 영어 번역을 함께 유지한다.
- ViewModel은 `StateFlow<UiState>` 하나를 노출하고, Composable은 상태를 받아 그리기만 한다.
- IO 작업은 주입받은 Dispatcher에서 실행한다. `Dispatchers.IO`를 직접 쓰지 않는다.
- 예외를 삼키지 않는다. 문서 열기 실패는 사용자에게 보이는 오류 상태로 전달한다.
- 주석은 "왜"만 쓴다. 코드가 말하는 내용을 반복하지 않는다.
- 사용하지 않는 코드, 미래를 위한 추상화, 요청받지 않은 리팩터링을 넣지 않는다.

## 7. 검증

작업을 마쳤다고 보고하기 전에 반드시 실행한다.

```
./gradlew assembleDebug
./gradlew testDebugUnitTest
./gradlew lintDebug
```

- 리포지토리, ViewModel, 포맷 판별 로직에는 단위 테스트를 작성한다.
- 실행하지 않은 검증을 통과했다고 쓰지 않는다. 에뮬레이터나 실기기가 없어 확인하지 못한 항목은 그대로 밝힌다.
- 테스트용 샘플 문서는 `samples/`에 두되, 저작권 문제가 없는 직접 만든 문서나 공개 라이선스 문서만 사용한다.

## 8. 주의 사항

- 앱 이름, 아이콘, 스토어 설명에서 한컴이나 Microsoft와 제휴한 것처럼 보이는 표현을 쓰지 않는다. "HWP", "Word"는 지원 포맷을 설명하는 용도로만 쓴다.
- 문서 내용, 파일명을 로그나 분석 도구로 보내지 않는다.
- 서명 키, `keystore.properties`, `local.properties`는 커밋하지 않는다.
- 되돌리기 어려운 작업(의존성 대규모 교체, 모듈 구조 변경, 파일 대량 삭제)은 실행 전에 확인을 받는다.
