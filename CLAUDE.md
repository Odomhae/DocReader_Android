# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

DocReader (`com.odom.docreader`) is a view-only Android document viewer for PDF, DOCX, HWP and HWPX, targeting a Google Play release. It works fully offline: documents never leave the device and the app has no `INTERNET` permission.

`Guide.md` (Korean) is the authoritative project brief: product scope, exclusions, design decisions, milestones M0–M7 and code rules. Read it before any non-trivial change. All milestones are implemented; remaining work is release hardening and real-device testing (`docs/release.md` lists what is still unverified).

## Commands

Gradle needs JDK 21. On this machine the `java` on PATH is 1.8 and `Android Studio/jbr` is broken, so set this first:

```bash
export JAVA_HOME="C:/Program Files/Android/Android Studio1/jbr"
```

Run all three before reporting work as done (Guide.md §7):

```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest
./gradlew lintDebug
```

Single module / class / method:

```bash
./gradlew :viewer:web:testDebugUnitTest
./gradlew :viewer:web:testDebugUnitTest --tests "com.odom.docreader.viewer.web.WebViewVersionTest"
./gradlew :viewer:web:testDebugUnitTest --tests "*.WebViewerViewModelTest.someMethod"
```

`:core:model` is a pure Kotlin/JVM module, so its task is `:core:model:test`, not `testDebugUnitTest` (it has no tests yet). `:viewer:hwp` and `:viewer:docx` have no tests either; their logic lives in JS assets and is only exercised on an emulator.

Instrumented tests (only `:viewer:web` has real ones — network blocking and large-file streaming):

```bash
ANDROID_SERIAL=emulator-5554 ./gradlew :viewer:web:connectedDebugAndroidTest
```

Always set `ANDROID_SERIAL` to an emulator for `connected*`/`install*` tasks. Real phones are attached over wireless adb and Gradle will otherwise install on them too.

Release: `./gradlew assembleRelease` (APK) or `bundleRelease` (AAB). R8 and resource shrinking are on. Signing is read from a git-ignored `keystore.properties` (template: `keystore.properties.example`); without it the build succeeds but produces `app-release-unsigned.apk`.

Sample documents live in `samples/`. Only self-made or openly licensed documents may go there. They are regenerated with:

```bash
node samples/make-samples.mjs viewer/hwp/src/main/assets/hwp/rhwp samples   # HWP/HWPX, uses the bundled rhwp
python samples/make-office-samples.py samples                                # DOCX/PDF, needs pypdf
```

`sample-password.pdf` opens with the password `docreader`; the `sample-corrupt.*` files exist to exercise the error state.

## Architecture

### Modules

```
:app                 entry point, nav graph, ACTION_VIEW intent handling
:core:model          DocumentFile, DocumentType, SortOrder (pure JVM)
:core:data           Room (recent/favourites), DataStore (settings), repositories
:core:files          SAF folder access, scanning, format detection, share / open-with
:core:ui             theme, shared composables and icons
:feature:home        file list, tabs, search, sort
:feature:settings    settings, open-source licences screen
:viewer:pdf          PDF viewer
:viewer:web          format-agnostic WebView host + JS bridge
:viewer:hwp          rhwp bundle + HWP/HWPX viewer page
:viewer:docx         docx-preview bundle + DOCX viewer page
```

Dependency direction is `app → feature / viewer → core`. Feature and viewer modules never depend on each other; the one exception is `:viewer:hwp` and `:viewer:docx` depending on `:viewer:web`.

Versions are all in `gradle/libs.versions.toml`. Hilt + KSP for DI, type-safe Navigation Compose routes (`app/.../navigation/Routes.kt`). minSdk 26, targetSdk 36, compileSdk 37, Java 11 bytecode; there is no convention plugin, so these are repeated in every module's `build.gradle.kts` and must be changed in all of them.

Assets from every module merge into one namespace, so each module keeps its files under its own top-level folder: `webhost/` (`:viewer:web`), `hwp/`, `docx/`, `licenses/` (`:feature:settings`). The JS engines are vendored files, not npm dependencies — there is no `package.json`. Upgrading one means replacing the files by hand and updating `docs/THIRD_PARTY.md`, the in-app licence list and the version noted below.

### Opening a document

Files are always `content://` Uris, never paths. Two entry points converge on one route:

- Home list → `ViewerRoute(uri, name, type)`.
- `ACTION_VIEW` from another app → `MainActivity` → `MainViewModel.onViewIntent` → `ExternalDocumentResolver` re-detects the real format from file name, MIME type and file signature (`DocumentTypeDetector`; HWP MIME types are unreliable) → `IncomingDocument` → `DocReaderNavHost` navigates. The document is added to recents only if the sender granted a persistable permission, since a temporary grant would be a dead entry on next launch.

`DocReaderNavHost` dispatches `ViewerRoute` to a viewer screen by `DocumentType`. The type travels as its enum name string, and both the nav host and `RoomLibraryRepository` tolerate an unknown name (unsupported screen / skipped row) rather than crashing.

### Web viewers (HWP, HWPX, DOCX)

`:viewer:web` owns everything native; a format module supplies only a `WebViewerConfig` (asset path of its HTML entry point, minimum WebView major version, whether the page does its own search) and its assets. The host must stay ignorant of formats.

- `createViewerWebView` serves everything from `https://appassets.androidplatform.net/` via `WebViewAssetLoader`. `/assets/…` maps to merged app assets; `/document/current` is streamed from the document Uri by `DocumentPathHandler`. Documents are never passed to JS as base64.
- Any request the asset loader does not own gets a 403, and navigation off that origin is refused (`ViewerUrls.isInternal`). File and content access are disabled. JS/WASM is bundled in assets, never loaded from a CDN.
- The bridge (`DocViewerBridge`) is deliberately tiny: `onLoaded(pageCount)`, `onPageChanged(page)`, `onFindResult`, `onError`. Native calls back through `window.docViewer.goToPage` and, when `pageHandlesSearch` is set, `find` / `findNext` / `clearFind`. The JS side of this contract is `viewer/web/src/main/assets/webhost/host.js`, which every viewer page imports; change both sides together.
- `onError`'s message is dropped on purpose because it can quote document content. Never log document content or file names.
- `WebViewerViewModel` gates on WebView version before loading anything (`CHECKING → LOADING → READY | FAILED`) and persists the current page in `SavedStateHandle` so rotation and process death restore the reading position.

Engine constraints that explain otherwise odd code:

- **HWP/HWPX**: `@rhwp/core` 0.8.7 (Rust → WASM, ~12 MB). Needs WebView 96+ (WASM reference-types). Pages are rendered lazily as SVG, so search is implemented in the page (`search.js`) rather than with WebView find-in-page. Nanum Myeongjo is bundled because Korean serif fonts are missing below API 28 and line breaking depends on real glyph widths; see `docs/font-mapping.md`.
- **DOCX**: docx-preview 0.4.1 + JSZip (used under MIT). Needs WebView 80+ (ES2020 syntax). Renders the whole document to DOM at once and uses WebView's find-in-page.
- Below the minimum WebView version the viewer shows an update notice plus "open with another app" instead of attempting to load. A stock API 26 emulator has WebView 69, so neither format opens there — that is expected, not a bug.

### PDF viewer

`PdfViewerScreen` branches on SDK level: API 28+ uses `androidx.pdf` (`pdf-viewer-fragment`, 1.0.0-beta01, minSdk 28); API 26–27 falls back to the `PdfRenderer`-based viewer in `viewer/pdf/.../legacy/`, which has no in-document search and cannot take a password.

## Project rules (from Guide.md)

- Out of scope for v1, do not build: any editing, format conversion, legacy DOC/XLS/PPT, login or cloud sync, `MANAGE_EXTERNAL_STORAGE`. Ask when scope is unclear.
- Dependencies: Apache-2.0, MIT or BSD only — **no GPL/AGPL** (e.g. MuPDF, iText). Check the licence before adding, then record name, version, licence and source in `docs/THIRD_PARTY.md`; the in-app licence list is `feature/settings/.../OpenSourceLibraries.kt` with texts in `feature/settings/src/main/assets/licenses/`.
- Verify current library versions, APIs and minSdk against official docs before writing code against them; the stack here is recent and moves quickly.
- If a rendering engine fails, report causes and alternatives and stop. Do not swap engines unilaterally.
- UI strings go in `strings.xml`; Korean is the default language and the English translation is maintained alongside.
- A ViewModel exposes a single `StateFlow<UiState>`; composables only render state.
- IO runs on the injected dispatcher (`@IoDispatcher` in `:core:files`), never `Dispatchers.IO` directly.
- Do not swallow exceptions; a failed open becomes a user-visible error state.
- Comments explain "why" only.
- No unused code, speculative abstractions or unrequested refactoring.
- Repositories, ViewModels and format detection get unit tests.
- Never imply affiliation with Hancom or Microsoft in the app name, icon or store text.
- Get confirmation before hard-to-reverse work: large dependency swaps, module restructuring, bulk deletion.
- Do not claim a check passed unless it was run; state plainly what could not be verified on an emulator or device.

## Emulator notes

- AVDs: `S26` (API 36, WebView 133) and `DocReader_API_26` (API 26, WebView 69). Both are slow; "System UI isn't responding" dialogs and janky-frame stats are emulator noise.
- With `-no-window`, `screencap` on API 26 returns a blank image. Check UI there with `uiautomator dump` instead.

## Further reading

- `docs/hwp-spike.md` — rhwp feasibility results, WASM feature requirements, load time and memory measurements
- `docs/docx-renderer.md` — why docx-preview, its limits
- `docs/font-mapping.md` — HWP font substitution
- `docs/release.md` — signing, intent filters, verified / unverified checklist, pre-launch to-dos
- `docs/THIRD_PARTY.md` — third-party inventory
