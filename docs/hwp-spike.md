# M0. HWP 렌더링 검증 결과

- 측정일: 2026-10-07
- 엔진: `@rhwp/core` 0.8.7 (MIT), `rhwp.js` 454KB + `rhwp_bg.wasm` 11.8MB를 assets에 번들
- 호스트: 단일 모듈 `:app`, `WebView` + `WebViewAssetLoader` (`https://appassets.androidplatform.net/assets/hwp/viewer.html`)
- 샘플: `samples/sample.hwp`(13.8KB), `samples/sample.hwpx`(7.7KB). `samples/make-samples.mjs`로 직접 생성한 1쪽짜리 한글/영문 텍스트 문서

## 결론

**최신 API에서는 동작하고, API 26 에뮬레이터에서는 동작하지 않는다.** 완료 기준 4개 중 1개가 미충족이므로 M0는 완료가 아니다.

| 완료 기준 | 결과 |
|---|---|
| 샘플 HWP, HWPX가 각각 렌더링된다 | API 36에서 충족. API 26에서 실패 |
| 네트워크를 끈 상태에서 동작한다 | 충족 (API 36, 비행기 모드) |
| API 26 에뮬레이터와 최신 API 에뮬레이터 모두에서 동작한다 | **미충족** — API 26 실패 |
| 로딩 시간과 메모리 사용량 기록 | 충족 (아래) |

## API 26 실패 원인

API 26 에뮬레이터(`system-images;android-26;google_apis;x86`)의 WebView는 Chrome **69.0.3497.100**이고, 이 이미지에서는 갱신할 수 없다.

```
AsyncCompile: Wasm decoding failed: return count of 4 exceeds internal limit of 1 @+36
```

`rhwp_bg.wasm`이 WebAssembly MVP 이후에 추가된 기능을 요구한다. binaryen으로 기능을 하나씩 끄고 검증해 확인한 필수 기능:

| 필수 WASM 기능 | Chrome 지원 시작 버전 (문헌 기준, 실측 아님) |
|---|---|
| sign-extension | 74 |
| bulk-memory, nontrapping-fptoint | 75 |
| multi-value | 85 |
| reference-types | 96 |

따라서 문헌상 최소 요구치는 **WebView 96 이상**으로 추정된다. 실측으로 확인한 것은 69 실패, 133 성공 두 지점뿐이다.

이것은 Android 버전이 아니라 WebView 버전의 문제다. Android 8.0 실기기는 WebView가 Play 스토어로 갱신되므로 96 이상일 가능성이 높지만, 이번 검증에서는 확인하지 못했다(갱신된 WebView를 가진 API 26 환경이 없음).

참고: 뷰어 글루 코드의 `globalThis`(Chrome 71+) 문제도 먼저 나왔으나 `window`로 바꿔 해결했다. `rhwp.js` 자체의 문법/모듈 로딩은 Chrome 69에서 통과했고 WASM 디코딩에서 멈췄다.

## 대안 (결정 필요)

1. **최소 WebView 버전 게이트**: minSdk 26은 유지하되, HWP/HWPX를 열 때 WebView 버전을 확인해 기준 미만이면 "Android System WebView 업데이트" 안내와 "다른 앱으로 열기"를 보여준다. 코드 변경이 가장 작다. API 26 완료 기준은 "WebView를 갱신한 API 26"으로 다시 정의해야 하고, Play 스토어 이미지(`google_apis_playstore`)에서 WebView를 갱신해 실측해야 한다.
2. **rhwp를 구형 WASM 타깃으로 직접 빌드**: rhwp 소스(Rust)를 multi-value / reference-types 등을 끈 설정으로 다시 빌드한다. Chrome 69에서도 돌 가능성이 있으나 빌드 파이프라인을 직접 유지해야 하고, 업스트림이 이 구성을 지원하는지 확인되지 않았다.
3. **minSdk 상향**: WebView 하한을 보장하지는 못하므로 단독으로는 해결책이 아니다. 1번과 함께 쓸 때만 의미가 있다.
4. **엔진 교체**: Guide 지침에 따라 임의로 진행하지 않았다.

## 결정 (2026-10-07)

대안 1(최소 WebView 버전 게이트)로 진행한다. `WebViewVersion.kt`가 WebView 메이저 버전을 읽어 96 미만이면 문서를 로딩하지 않고 업데이트 안내 상태로 바꾼다. 버전을 읽지 못하면 막지 않고 시도한다(실패는 뷰어 페이지가 그대로 보고한다).

- 단위 테스트 5개 통과 (`WebViewVersionTest`)
- API 36: 게이트 추가 후에도 HWP, HWPX 렌더링 유지 확인
- API 26(WebView 69): 로그로 게이트 동작(`WebView 69.0.3497.100 is below 96`)과 프로세스 생존은 확인했다. **안내 문구가 화면에 그려지는지는 확인하지 못했다** — 창 없는 모드로 찍은 스크린샷이 빈 화면이었고, 창 모드 재확인은 하지 못했다.
- WebView를 96 이상으로 갱신한 API 26 환경에서의 렌더링은 여전히 미확인이다.

## 측정 (API 36 에뮬레이터)

환경: AVD `S26`, `system-images;android-36;google_apis;x86_64`, RAM 2GB, WebView 133.0.6943.137, debug 빌드, 호스트 Windows 11 + HAXM. 프로세스를 강제 종료한 뒤 콜드 스타트로 측정했다.

- 전체: `loadUrl` 호출부터 JS가 완료를 보고할 때까지 (Kotlin 측 시계). WebView 프로세스 기동 포함
- 초기화: WASM 다운로드(assets) + 컴파일 + 인스턴스화
- 파싱: `new HwpDocument(bytes)` + `pageCount()`
- 렌더링: 전 페이지 `renderPageSvg()` + DOM 삽입 + 1프레임

### 비행기 모드 (네트워크 차단, 3회)

| 샘플 | 전체 ms | 초기화 | 읽기 | 파싱 | 렌더링 |
|---|---|---|---|---|---|
| HWP | 5671 / 4854 / 2411 | 875 / 1520 / 719 | 69 / 38 / 13 | 948 / 506 / 360 | 736 / 323 / 199 |
| HWPX | 4299 / 2527 / 3453 | 618 / 390 / 647 | 21 / 11 / 15 | 544 / 424 / 690 | 260 / 101 / 233 |

### 네트워크 연결 상태 (부팅 직후, 3회)

| 샘플 | 전체 ms | 초기화 | 파싱 | 렌더링 |
|---|---|---|---|---|
| HWP | 12086 / 21130 / 5787 | 3472 / 11957 / 1376 | 1544 / 1503 / 631 | 864 / 1111 / 353 |
| HWPX | 16371 / 11644 / 8334 | 3148 / 3048 / 1397 | 3018 / 858 / 1307 | 2027 / 510 / 653 |

부팅 직후 측정이라 에뮬레이터 자체 부하가 크게 섞였다(같은 문서가 2.4초~21초). 수치는 상한 참고용이며, 가장 안정된 구간은 전체 2.4~3.5초, 초기화 0.4~0.7초다. 실기기 측정은 하지 않았다.

### 메모리 (HWPX 1쪽 로딩 직후, `dumpsys meminfo`)

| 프로세스 | PSS | RSS |
|---|---|---|
| 앱 (`com.odom.docreader`) | 149 MB | 259 MB |
| WebView 렌더러 (`sandboxed_process0`) | 135 MB | 273 MB |

1쪽짜리 텍스트 문서 기준이다. 렌더러의 대부분은 11.8MB WASM의 컴파일 결과와 선형 메모리로 보이나 분해 측정은 하지 않았다. 큰 문서에서의 증가량은 M4/M5에서 측정해야 한다.

## 그 밖에 확인한 것

- 매니페스트에 `INTERNET` 권한 없이 동작한다. `WebViewAssetLoader`가 가로채지 않은 요청은 403으로 막는다.
- 문서는 base64가 아니라 `fetch`로 받는다 (이번에는 assets 경로. Uri 스트리밍 `PathHandler`는 M4).
- 폰트를 번들하지 않아 기기 기본 폰트로 대체됐다. 글자는 정상 표시됐고, 원본 대비 조판 차이는 평가하지 않았다(M5).
- rhwp는 페이지마다 글자 단위 `<text>`로 SVG를 만든다. 4줄짜리 문서 1쪽이 약 70KB SVG였다. 긴 문서는 전 페이지를 한 번에 DOM에 넣지 말고 보이는 페이지만 렌더링해야 할 것으로 보인다.

## 확인하지 못한 것

- 갱신된 WebView를 가진 API 26 환경, 실기기
- 표, 이미지, 수식이 들어간 문서와 여러 쪽 문서 (M5 범위)
- 화면 회전, 프로세스 종료 후 복원
- lint 경고 17건은 남아 있다. 스파이크 코드 관련은 `MissingOnRenderProcessGone`(M4에서 처리 필요), 나머지는 템플릿의 미사용 색상과 버전 업데이트 알림이다.

## 스크린샷

- `hwp-spike/api36-hwpx.png` — API 36, HWPX
- `hwp-spike/api36-hwp-offline.png` — API 36, HWP, 비행기 모드 (화면의 "System UI isn't responding"은 에뮬레이터 부하로 뜬 시스템 대화상자이며 앱과 무관)

## 이후 변경 (M4, M5)

스파이크 코드는 M4에서 `:viewer:web`(공용 호스트)과 `:viewer:hwp`(뷰어 페이지, rhwp 번들)로 옮겼고 `app/src/debug`의 스파이크는 삭제했다. 아래 재현 명령은 그 시점 이전 기준이며 지금은 동작하지 않는다. 지금은 홈 화면에서 HWP 파일을 열면 된다.

## 재현

```
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.odom.docreader/.MainActivity --es sample sample.hwp   # 또는 sample.hwpx
adb logcat -s HwpSpike
```
