# 서드파티 라이브러리

앱에 포함되는 서드파티 구성 요소 목록. 버전의 기준은 `gradle/libs.versions.toml`이다.

## 앱에 포함

| 이름 | 버전 | 라이선스 | 출처 | 용도 |
|---|---|---|---|---|
| @rhwp/core | 0.8.7 | MIT | https://www.npmjs.com/package/@rhwp/core (https://github.com/edwardkim/rhwp) | HWP/HWPX 파서·렌더러 (Rust + WASM). `viewer/hwp/src/main/assets/hwp/rhwp/` |
| docx-preview | 0.4.1 | Apache-2.0 | https://www.npmjs.com/package/docx-preview (https://github.com/VolodymyrBaydalka/docxjs) | DOCX 렌더러. `viewer/docx/src/main/assets/docx/lib/` |
| JSZip | 3.10.2 | MIT (MIT 또는 GPL-3.0 이중 라이선스 중 MIT 선택) | https://www.npmjs.com/package/jszip | docx-preview의 의존성. 같은 폴더 |
| 나눔명조 (Regular, Bold) | Google Fonts 배포본 | SIL OFL 1.1 | https://github.com/google/fonts/tree/main/ofl/nanummyeongjo | HWP 명조 계열 대체 폰트. `viewer/hwp/src/main/assets/hwp/fonts/` |
| androidx.pdf:pdf-viewer-fragment | 1.0.0-beta01 | Apache-2.0 | Google Maven | PDF 뷰어 (API 28 이상) |
| androidx.core:core-ktx | 1.19.1 | Apache-2.0 | Google Maven | |
| androidx.activity:activity-compose | 1.13.0 | Apache-2.0 | Google Maven | |
| androidx.compose (BOM: ui, material3 등) | 2026.02.01 | Apache-2.0 | Google Maven | UI |
| androidx.lifecycle (runtime, viewmodel-compose) | 2.11.0 | Apache-2.0 | Google Maven | |
| androidx.navigation:navigation-compose | 2.10.2 | Apache-2.0 | Google Maven | 타입 세이프 내비게이션 |
| androidx.fragment (fragment-ktx, fragment-compose) | 1.9.1 | Apache-2.0 | Google Maven | PdfViewerFragment 호스팅 |
| androidx.room (runtime, ktx) | 2.8.5 | Apache-2.0 | Google Maven | 최근 문서, 즐겨찾기 |
| androidx.datastore:datastore-preferences | 1.2.1 | Apache-2.0 | Google Maven | 설정 저장 |
| androidx.hilt:hilt-lifecycle-viewmodel-compose | 1.4.0 | Apache-2.0 | Google Maven | |
| androidx.webkit:webkit | 1.17.1 | Apache-2.0 | Google Maven | `WebViewAssetLoader`, WebView 버전 확인 |
| com.google.android.material:material | 1.13.0 | Apache-2.0 | Google Maven | androidx.pdf 뷰가 요구하는 Material3 테마 |
| com.google.dagger:hilt-android | 2.60.1 | Apache-2.0 | Maven Central | DI |
| org.jetbrains.kotlinx:kotlinx-coroutines-android | 1.11.0 | Apache-2.0 | Maven Central | |
| org.jetbrains.kotlinx:kotlinx-serialization-core | 1.9.0 | Apache-2.0 | Maven Central | 내비게이션 라우트 직렬화 |

## 빌드·테스트에만 사용 (앱에 포함되지 않음)

| 이름 | 버전 | 라이선스 | 용도 |
|---|---|---|---|
| KSP (com.google.devtools.ksp) | 2.3.12 | Apache-2.0 | Hilt, Room 코드 생성 |
| hilt-compiler, room-compiler | 위와 동일 | Apache-2.0 | 애너테이션 프로세서 |
| JUnit 4 | 4.13.2 | EPL-1.0 | 단위 테스트 |
| kotlinx-coroutines-test | 1.11.0 | Apache-2.0 | 단위 테스트 |
| pypdf | (샘플 생성 시 설치한 최신 버전) | BSD-3-Clause | `samples/make-office-samples.py`에서 암호 PDF 샘플 생성 |

## 참고
- `@rhwp/core`의 WASM에 정적으로 포함된 Rust 크레이트(wasm-bindgen, quick-xml, cfb, flate2, encoding_rs, usvg, svg2pdf, pdf-writer, image 등)는 패키지 README 기준 MIT / Apache-2.0 / BSD-3-Clause다. 전체 목록: https://github.com/edwardkim/rhwp/blob/main/THIRD_PARTY_LICENSES.md — 오픈소스 라이선스 화면(M7)을 만들 때 이 목록을 반영해야 한다.
- 아이콘(`core/ui/src/main/res/drawable/ic_*.xml`, `feature/home/src/main/res/drawable/ic_home_document.xml`)은 Google Material Icons(Apache-2.0)의 경로 데이터를 사용했다.
- JUnit의 EPL-1.0은 테스트 전용이며 앱에 포함되지 않는다.
- 나눔명조의 SIL OFL 1.1은 Guide의 허용 목록(Apache 2.0, MIT, BSD)에 없지만, Guide 4장이 폰트에 대해 따로 정한 "재배포가 허용된 한글 폰트(예: Noto Sans KR, 나눔 계열)"에 해당한다. 대체 매핑은 `docs/font-mapping.md`.
- androidx.test (ext-junit, espresso-core, Apache-2.0)는 `:viewer:web`의 기기 테스트에만 쓴다.
