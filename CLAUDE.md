# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 이 프로젝트는

DocReader(`com.odom.docreader`)는 PDF, DOCX, HWP, HWPX, TXT를 보기만 하는 Android 문서 뷰어이고 Google Play 출시가 목표다. 완전히 오프라인으로 동작한다. 문서는 기기 밖으로 나가지 않으며 앱에 `INTERNET` 권한이 없다.

`Guide.md`가 프로젝트의 기준 문서다. 제품 범위, 제외 항목, 설계 결정, 마일스톤 M0–M7, 코드 규칙이 들어 있으니 사소하지 않은 변경 전에는 먼저 읽는다. 마일스톤은 모두 구현됐고, 남은 일은 출시 준비와 실기기 검증이다 (`docs/release.md`에 아직 확인하지 못한 항목이 정리돼 있다).

## 명령어

Gradle은 JDK 21이 필요하다. 이 PC는 PATH의 `java`가 1.8이므로 먼저 설정한다.

```bash
export JAVA_HOME="C:/Program Files/Android/Android Studio1/jbr"
```

작업을 마쳤다고 보고하기 전에 세 가지를 모두 실행한다 (Guide.md §7).

```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest
./gradlew lintDebug
```

모듈 / 클래스 / 메서드 하나만 실행:

```bash
./gradlew :viewer:web:testDebugUnitTest
./gradlew :viewer:web:testDebugUnitTest --tests "com.odom.docreader.viewer.web.WebViewVersionTest"
./gradlew :viewer:web:testDebugUnitTest --tests "*.WebViewerViewModelTest.someMethod"
```

`:core:model`은 순수 Kotlin/JVM 모듈이라 태스크가 `testDebugUnitTest`가 아니라 `:core:model:test`다 (테스트는 아직 없다). `:viewer:hwp`, `:viewer:docx`, `:viewer:txt`, `:core:ui`도 테스트가 없다. 세 뷰어 모듈의 로직은 JS assets에 있어서 에뮬레이터에서만 확인할 수 있다. `MediaStoreDocumentScanner`도 실제 `MediaStore`가 필요해 단위 테스트가 없다.

계측 테스트는 `:viewer:web`에만 의미 있는 것이 있다 (네트워크 차단, 대용량 파일 스트리밍).

```bash
ANDROID_SERIAL=emulator-5554 ./gradlew :viewer:web:connectedDebugAndroidTest
```

`connected*`, `install*` 태스크는 반드시 `ANDROID_SERIAL`을 에뮬레이터로 지정한다. 실제 폰이 무선 adb로 연결돼 있으면 Gradle이 거기에도 설치한다.

릴리스: `./gradlew assembleRelease`(APK) 또는 `bundleRelease`(AAB). R8과 리소스 축소가 켜져 있다. 서명 정보는 git에서 제외된 `keystore.properties`에서 읽는다 (양식: `keystore.properties.example`). 파일이 없어도 빌드는 성공하지만 `app-release-unsigned.apk`가 나온다.

샘플 문서는 `samples/`에 있다. 직접 만든 문서나 공개 라이선스 문서만 넣을 수 있다. 다시 만들려면:

```bash
node samples/make-samples.mjs viewer/hwp/src/main/assets/hwp/rhwp samples   # HWP/HWPX, 번들된 rhwp 사용
python samples/make-office-samples.py samples                                # DOCX/PDF, pypdf 필요
```

`sample-password.pdf`의 암호는 `docreader`다. `sample-corrupt.*`는 오류 화면을 확인하기 위한 파일이다.

## 아키텍처

### 모듈

```
:app                 진입점, 내비게이션 그래프, ACTION_VIEW 인텐트 처리
:core:model          DocumentFile, DocumentType, SortOrder (순수 JVM)
:core:data           Room(최근 문서), DataStore(설정), 리포지토리
:core:files          저장소 접근 권한, 파일 스캔, 포맷 판별, 공유 / 다른 앱으로 열기
:core:ui             테마, 공용 Composable과 아이콘
:feature:home        파일 목록, 필터 칩, 검색, 정렬
:feature:settings    설정(글씨 크기), 오픈소스 라이선스 화면
:viewer:pdf          PDF 뷰어
:viewer:web          포맷을 모르는 WebView 호스트 + JS 브리지
:viewer:hwp          rhwp 번들 + HWP/HWPX 뷰어 페이지
:viewer:docx         docx-preview 번들 + DOCX 뷰어 페이지
:viewer:txt          TXT 뷰어 페이지
```

의존 방향은 `app → feature / viewer → core`다. feature와 viewer 모듈끼리는 서로 의존하지 않는다. 예외는 `:viewer:hwp`, `:viewer:docx`, `:viewer:txt`가 `:viewer:web`에 의존하는 것 하나뿐이다.

버전은 모두 `gradle/libs.versions.toml`에 있다. DI는 Hilt + KSP, 내비게이션은 타입 세이프 Navigation Compose 라우트(`app/.../navigation/Routes.kt`)다. minSdk 26, targetSdk 36, compileSdk 37, Java 11 바이트코드. 컨벤션 플러그인이 없어서 이 값들이 모듈마다 `build.gradle.kts`에 반복돼 있으므로 바꿀 때는 전부 바꿔야 한다.

모든 모듈의 assets는 하나의 네임스페이스로 합쳐진다. 그래서 모듈마다 자기 최상위 폴더 아래에만 파일을 둔다: `webhost/`(`:viewer:web`), `hwp/`, `docx/`, `txt/`, `licenses/`(`:feature:settings`). JS 엔진은 npm 의존성이 아니라 직접 넣은 파일이고 `package.json`은 없다. 엔진을 올리려면 파일을 손으로 교체하고 `docs/THIRD_PARTY.md`, 앱 안의 라이선스 목록, 아래에 적힌 버전을 함께 고친다.

### 문서 열기

파일은 항상 `content://` Uri로 다루고 경로는 쓰지 않는다. 진입점은 둘이고 하나의 라우트로 모인다.

- 홈 목록 → `ViewerRoute(uri, name, type)`
- 다른 앱의 `ACTION_VIEW` → `MainActivity` → `MainViewModel.onViewIntent` → `ExternalDocumentResolver`가 파일명, MIME 타입, 파일 시그니처로 실제 포맷을 다시 판별(`DocumentTypeDetector`. HWP는 MIME 타입을 믿을 수 없다) → `IncomingDocument` → `DocReaderNavHost`가 이동. 보낸 앱이 영구 권한을 준 경우에만 최근 문서에 넣는다. 임시 권한이면 다음 실행 때 열 수 없는 항목이 되기 때문이다.

`DocReaderNavHost`가 `DocumentType`에 따라 `ViewerRoute`를 뷰어 화면으로 보낸다. 타입은 enum 이름 문자열로 전달되고, 내비게이션 호스트와 `RoomLibraryRepository` 모두 모르는 이름을 만나면 크래시하지 않고 넘긴다 (미지원 화면 / 행 건너뛰기).

### 홈 목록과 저장소 권한

홈 목록은 기기 공용 저장소에 있는 지원 문서 전부다. 폴더 선택은 없다 (2026-10-12에 SAF 폴더 선택 방식에서 변경).

- `MediaStoreDocumentScanner`가 디렉터리를 훑지 않고 `MediaStore.Files`를 확장자로 한 번 조회한다. 그래서 Uri가 `content://media/external/file/<id>` 형태이고, 뷰어·최근 문서·공유가 스캔 방식과 무관하게 동작한다.
- 권한은 `StorageAccess`가 API 수준 차이를 감춘다. API 30 이상은 `MANAGE_EXTERNAL_STORAGE`(시스템 설정의 "모든 파일에 대한 액세스" 토글), API 26–29는 `READ_EXTERNAL_STORAGE` 런타임 권한이다. API 29는 매니페스트의 `requestLegacyExternalStorage`에 의존한다.
- 권한 요청 UI는 `feature/home/.../StorageAccessRequest.kt`에 있다. API 30 이상의 설정 화면은 토글을 켜도 스스로 닫히지 않아서, 권한이 생길 때까지 지켜보다가 `finishActivity`로 닫아 앱으로 돌아온다 (그러려고 설정 화면을 `startActivityForResult`로 연다). 결과를 콜백으로 받지 않는다. 홈 화면이 resume될 때마다 다시 스캔하므로(`LifecycleResumeEffect`) 설정 화면이나 권한 팝업에서 돌아오면 저절로 반영된다.
- 권한이 없으면 스캐너가 `ScanResult.AccessRequired`를 돌려주고 홈은 `StorageStatus.ACCESS_REQUIRED` 화면을 보여준다.
- 한계: 시스템이 아직 색인하지 않은 파일은 목록에 나오지 않는다. `Android/data`는 이 권한으로도 읽을 수 없다.
- 이 권한은 Play Console에서 별도 신고와 심사가 필요하다 (`docs/release.md`).

### 홈 화면 구성 (2026-10-14)

- 제목 줄이 없다. 맨 위 줄은 검색창과 정렬·설정 버튼이고, 권한이 없을 때도 설정에 갈 수 있도록 모든 상태에서 보인다.
- 그 아래 칩 한 줄: 최근, PDF, DOCX, HWP(HWPX 포함), TXT (`HomeFilter`). 하나만 선택되고, 다시 누르면 해제되어 기기 전체 목록이 된다. "전체" 칩은 일부러 없고, 최근과 포맷은 조합하지 않는다.
- 즐겨찾기는 2026-10-14에 없앴다. DB 버전은 개발 중이라 1로 둔 채 `isFavorite` 컬럼을 지웠으므로, 이전 빌드가 설치된 기기는 앱을 다시 설치해야 한다 (스키마 해시가 달라 Room이 열기를 거부한다). `documents` 테이블에는 연 문서만 있고 `lastOpenedAt`은 non-null이다.
- 최근을 켜면 정렬 설정을 무시하고 연 순서로 보이며, 행에 수정일 대신 연 시각을 보여준다 (`LibraryRepository.recentDocuments`가 `RecentDocument`로 연 시각을 함께 준다).
- 행 앞의 포맷 배지 색(`DocumentTypeBadge`)은 브랜드 청록과 겹치지 않게 골랐다.

### 웹 뷰어 (HWP, HWPX, DOCX, TXT)

네이티브 쪽은 전부 `:viewer:web`이 맡는다. 포맷 모듈은 `WebViewerConfig`(HTML 진입점의 asset 경로, 최소 WebView 메이저 버전, 페이지가 검색을 직접 처리하는지)와 자기 assets만 제공한다. 호스트는 포맷을 몰라야 한다.

- `createViewerWebView`가 `WebViewAssetLoader`로 모든 것을 `https://appassets.androidplatform.net/` 아래에서 제공한다. `/assets/…`는 합쳐진 앱 assets, `/document/current`는 `DocumentPathHandler`가 문서 Uri에서 스트리밍한다. 문서를 base64로 JS에 넘기지 않는다.
- asset 로더가 맡지 않는 요청은 403을 받고, 해당 origin을 벗어나는 이동은 거부한다(`ViewerUrls.isInternal`). 파일 접근과 콘텐츠 접근은 꺼져 있다. JS/WASM은 assets에 번들하고 CDN에서 불러오지 않는다.
- 브리지(`DocViewerBridge`)는 일부러 작게 유지한다: `onLoaded(pageCount)`, `onPageChanged(page)`, `onFindResult`, `onError`. 네이티브는 `window.docViewer.goToPage`로, `pageHandlesSearch`가 켜져 있으면 `find` / `findNext` / `clearFind`로 호출한다. 이 계약의 JS 쪽은 모든 뷰어 페이지가 import하는 `viewer/web/src/main/assets/webhost/host.js`다. 양쪽을 함께 바꾼다.
- `onError`의 메시지는 문서 내용을 인용할 수 있어서 일부러 버린다. 문서 내용과 파일명은 절대 로그에 남기지 않는다.
- `WebViewerViewModel`은 무엇이든 로딩하기 전에 WebView 버전부터 확인한다(`CHECKING → LOADING → READY | FAILED`). 현재 페이지를 `SavedStateHandle`에 저장해 화면 회전과 프로세스 종료 후에도 읽던 위치를 복원한다.

코드가 이상해 보이는 부분의 이유가 되는 엔진 제약:

- **HWP/HWPX**: `@rhwp/core` 0.8.7 (Rust → WASM, 약 12MB). WebView 96 이상이 필요하다 (WASM reference-types). 페이지를 SVG로 지연 렌더링하므로 검색을 WebView의 페이지 내 찾기가 아니라 페이지 안(`search.js`)에서 구현했다. API 28 미만에는 한글 명조 계열 폰트가 없고 줄바꿈이 실제 글리프 폭에 좌우되기 때문에 나눔명조를 번들했다. `docs/font-mapping.md` 참고.
- **DOCX**: docx-preview 0.4.1 + JSZip (MIT로 사용). WebView 80 이상이 필요하다 (ES2020 문법). 문서 전체를 한 번에 DOM으로 렌더링하고 WebView의 페이지 내 찾기를 쓴다.
- **TXT**: 라이브러리 없이 `TextDecoder`로 푼다. WebView 61 이상이 필요하다 (ES 모듈). 파일에 인코딩 정보가 없으므로 `txt/decode.js`가 UTF-16 BOM, 엄격한 UTF-8, 그래도 아니면 CP949(`euc-kr`) 순서로 판단한다. 쪽이 없어서 `WebViewerConfig.showPageBar`를 꺼 아래 막대를 숨기고, 그래서 읽던 위치도 복원되지 않는다.
- 최소 WebView 버전 미만이면 로딩을 시도하지 않고 업데이트 안내와 "다른 앱으로 열기"를 보여준다. 오래된 에뮬레이터 이미지에서 HWP가 열리지 않는 것은 버그가 아니라 의도된 동작이다.

### PDF 뷰어

`PdfViewerScreen`이 SDK 수준으로 갈린다. API 28 이상은 `androidx.pdf`(`pdf-viewer-fragment`, 1.0.0-beta01, minSdk 28)를 쓰고, API 26–27은 `viewer/pdf/.../legacy/`의 `PdfRenderer` 기반 뷰어로 대체한다. 대체 뷰어는 문서 내 검색이 없고 암호를 받을 수 없다.

### 쪽 표시 막대

모든 뷰어의 아래 막대는 `:core:ui`의 `PageBar` 하나다 (이전/다음, 현재 쪽 번호를 눌러 수정하면 그 쪽으로 이동). 입력 해석은 `:core:model`의 `resolvePageInput`이 하고, 범위를 벗어난 번호는 가장 가까운 쪽으로 보낸다.

- 웹 뷰어는 기존 브리지(`onPageChanged`, `goToPage`)를 그대로 쓴다.
- `androidx.pdf`는 프래그먼트의 `onPdfViewCreated`로 받은 `PdfView`에서 현재 쪽을 듣고 `scrollToPage`를 호출한다. 현재 쪽은 첫 보이는 쪽이 아니라 화면 가운데에 있는 쪽이다 (`scrollToPage`가 대상 쪽을 가운데에 맞추기 때문에 첫 보이는 쪽으로 세면 이전/다음이 멈춘다). 이 콜백은 1.0.0-beta01에서 `@ExperimentalPdfApi`라서 라이브러리를 올릴 때 시그니처가 바뀌었는지 확인한다.
- 구형 PDF 뷰어는 `LazyListState`의 첫 보이는 항목을 현재 쪽으로 쓴다.
- 막대는 번호를 수정하는 동안에만 키보드 위로 올라온다. 그래서 액티비티가 `adjustResize`다.

### 글씨 크기

설정 화면에서 고르는 4단계 배율(`FontScale`, `SettingsRepository`에 저장)이 앱의 Compose UI 글씨에만 적용된다. `MainActivity`가 `AppearanceViewModel`에서 읽어 `DocReaderTheme(fontScaleMultiplier)`에 넘기고, 테마가 `LocalDensity.fontScale`에 시스템 값과 곱한다. 문서 본문(WebView, PDF 페이지)과 `androidx.pdf`의 View 기반 UI는 바뀌지 않는다.

### 읽기 설정 (종이색, 밝기)

뷰어 안에서만 적용된다. 앱 UI는 라이트 고정이고 색 반전(다크) 모드는 없다. 앱 색은 앱 아이콘 배경색 `#00695C` 하나로 고정이고 다이내믹 컬러는 쓰지 않는다 (`core/ui/.../theme/Color.kt`, `androidx.pdf` 뷰용으로 `themes.xml`의 `colorPrimary`도 같은 색).

- 상태는 `:app`의 `ReadingSettingsViewModel`(액티비티 범위) 하나가 갖고, `ViewerDestination`이 뷰어 화면에 `paperColor`와 `onReadingSettings`만 넘긴다. 뷰어 모듈은 `:core:data`에 의존하지 않는다.
- 세피아는 포맷과 무관하게 문서 영역 위에 곱하기 블렌드로 색을 덮는 `Modifier.sepiaPaper`(`:core:ui`) 하나로 처리한다. 뷰어 페이지의 CSS/JS는 건드리지 않는다.
- 밝기는 `ViewerDestination`이 창의 `screenBrightness`를 바꾸고 뷰어를 나가면 되돌린다. "시스템 밝기 사용"이 켜져 있으면 값을 건드리지 않는다.
- 저장은 `ReadingSettingsRepository`(DataStore). 슬라이더는 드래그가 끝날 때 한 번만 저장한다.

## 프로젝트 규칙 (Guide.md에서)

- 1차 출시 범위 밖이므로 만들지 않는다: 모든 편집 기능, 포맷 변환, 구형 DOC/XLS/PPT, 로그인이나 클라우드 동기화. 범위가 애매하면 먼저 묻는다.
- 의존성은 Apache-2.0, MIT, BSD만 허용한다. **GPL/AGPL 금지** (예: MuPDF, iText). 추가 전에 라이선스를 확인하고 이름, 버전, 라이선스, 출처를 `docs/THIRD_PARTY.md`에 기록한다. 앱 안의 라이선스 목록은 `feature/settings/.../OpenSourceLibraries.kt`, 전문은 `feature/settings/src/main/assets/licenses/`에 있다.
- 라이브러리의 현재 버전, API, minSdk를 공식 문서에서 확인한 뒤 코드를 쓴다. 이 프로젝트의 스택은 최신이고 빠르게 바뀐다.
- 렌더링 엔진이 동작하지 않으면 원인과 대안을 보고하고 멈춘다. 임의로 엔진을 바꾸지 않는다.
- UI 문자열은 `strings.xml`에 둔다. 기본 언어는 한국어이고 영어 번역을 함께 유지한다.
- ViewModel은 `StateFlow<UiState>` 하나만 노출하고 Composable은 상태를 그리기만 한다.
- IO는 주입받은 디스패처(`:core:files`의 `@IoDispatcher`)에서 실행한다. `Dispatchers.IO`를 직접 쓰지 않는다.
- 예외를 삼키지 않는다. 열기 실패는 사용자에게 보이는 오류 상태가 된다.
- 주석은 "왜"만 쓴다.
- 쓰지 않는 코드, 미래를 위한 추상화, 요청받지 않은 리팩터링을 넣지 않는다.
- 리포지토리, ViewModel, 포맷 판별 로직에는 단위 테스트를 작성한다.
- 앱 이름, 아이콘, 스토어 문구에서 한컴이나 Microsoft와 제휴한 것처럼 보이게 하지 않는다.
- 되돌리기 어려운 작업은 먼저 확인받는다: 의존성 대규모 교체, 모듈 구조 변경, 대량 삭제.
- 실행하지 않은 검증을 통과했다고 쓰지 않는다. 에뮬레이터나 기기에서 확인하지 못한 것은 그대로 밝힌다.

## 에뮬레이터 메모

- `adb`와 `emulator`는 PATH에 없다. `$LOCALAPPDATA/Android/Sdk/platform-tools/adb.exe`, `$LOCALAPPDATA/Android/Sdk/emulator/emulator.exe`를 쓴다.
- AVD는 `S26`(API 36, WebView 133), `DocReader_API_26`(API 26, WebView 69), `Pixel_3a_API_30`이 있다 (2026-10-14 `emulator -list-avds` 기준). `DocReader_API_26`은 WebView가 69라 웹 뷰어 포맷은 전부 업데이트 안내 화면이 나온다.
- 2026-10-13부터 어떤 에뮬레이터도 부팅되지 않는다. emulator 37.2.12가 "HAXM is no longer supported"로 종료하고(`-accel off`도 같음), WHPX나 AEHD 드라이버가 설정돼 있지 않다. 기기 확인은 가속 설정을 고치거나 실제 폰을 쓰도록 사용자에게 요청해야 한다.
- `-no-window`로 띄우면 API 26에서 `screencap`이 빈 이미지를 준다.
- 에뮬레이터가 느리다. "System UI isn't responding" 대화상자와 프레임 지연 통계는 에뮬레이터 탓이다.
- 화면 확인은 `uiautomator dump`로 텍스트를 읽는 방법이 가장 안정적이다.
- 모든 파일 액세스를 테스트로 회수할 때 `appops set <패키지> MANAGE_EXTERNAL_STORAGE deny`는 효과가 없다 (설정 화면에서 허용하면 uid 모드로 기록돼 그쪽이 우선한다). 설정 화면에서 토글을 끈다.

## 더 읽을 문서

- `docs/hwp-spike.md` — rhwp 검증 결과, 필요한 WASM 기능, 로딩 시간과 메모리 측정
- `docs/docx-renderer.md` — docx-preview를 고른 이유와 한계
- `docs/font-mapping.md` — HWP 폰트 대체
- `docs/release.md` — 서명, 인텐트 필터, 저장소 권한, 확인한 것 / 확인하지 못한 것, 출시 전 할 일
- `docs/THIRD_PARTY.md` — 서드파티 목록
