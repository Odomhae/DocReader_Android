# 출시 준비 (M7)

## 릴리스 빌드

```
./gradlew assembleRelease     # APK
./gradlew bundleRelease       # Play 업로드용 AAB
```

- R8 축소와 리소스 축소가 켜져 있다 (`isMinifyEnabled`, `isShrinkResources`).
- `app/proguard-rules.pro`에는 추가 규칙이 없다. 라이브러리들이 제공하는 규칙만으로 아래 확인 항목이 모두 동작했다.
- 릴리스 APK는 약 10MB다 (debug는 약 26MB).

## 서명

서명 정보는 프로젝트 루트의 `keystore.properties`에서 읽는다. 이 파일과 키스토어(`*.jks`, `*.keystore`)는 `.gitignore`에 들어 있어 커밋되지 않는다.

1. 업로드 키를 만든다. 예: `keytool -genkeypair -v -keystore release.jks -keyalg RSA -keysize 2048 -validity 10000 -alias upload`
2. `keystore.properties.example`을 `keystore.properties`로 복사해 값을 채운다.
3. `./gradlew assembleRelease`를 실행하면 서명된 `app-release.apk`가 나온다.

`keystore.properties`가 없으면 빌드는 성공하지만 `app-release-unsigned.apk`가 나온다. **실제 업로드 키는 아직 만들지 않았다.** 키와 비밀번호는 소유자가 직접 만들고 보관해야 한다.

## "연결 프로그램"으로 열기

`MainActivity`가 `ACTION_VIEW`를 두 가지 방식으로 받는다.

| 필터 | 조건 |
|---|---|
| MIME 타입 | `application/pdf`, DOCX MIME, HWP 3종(`application/x-hwp`, `application/haansofthwp`, `application/vnd.hancom.hwp`), HWPX 3종(`application/hwp+zip`, `application/haansofthwpx`, `application/vnd.hancom.hwpx`) |
| 확장자 | MIME이 무엇이든 Uri 경로가 `.pdf` `.docx` `.hwp` `.hwpx`(대문자 포함)로 끝나는 경우 |

- `content://` Uri만 받는다. `file://`은 최신 Android에서 다른 앱이 넘길 수 없고, 받더라도 저장소 권한 없이는 읽을 수 없다.
- 실제 포맷은 열 때 파일명, MIME, 파일 시그니처로 다시 판별한다. 지원하지 않는 파일이면 안내 화면이 나온다.
- 보낸 앱이 영구 권한을 준 경우에만 최근 문서에 기록한다. 임시 권한뿐이면 기록하지 않는다(다음 실행 때 열 수 없는 항목이 되기 때문).
- 한계: 경로에 파일명이 없는 Uri(예: `content://media/external/file/123`)가 일반 MIME(`application/octet-stream`)으로 오면 이 앱이 목록에 나타나지 않는다.

## 저장소 권한

홈 목록은 기기 전체의 문서를 보여준다 (2026-10-12에 폴더 선택 방식에서 변경).

| Android | 권한 | 허용 방식 |
|---|---|---|
| 11 이상 (API 30+) | `MANAGE_EXTERNAL_STORAGE` | 시스템 설정의 "모든 파일에 대한 액세스" 토글 |
| 10 (API 29) | `READ_EXTERNAL_STORAGE` + `requestLegacyExternalStorage` | 런타임 권한 팝업 |
| 8–9 (API 26–28) | `READ_EXTERNAL_STORAGE` | 런타임 권한 팝업 |

- 목록은 `MediaStore.Files`에서 확장자로 조회한다. 시스템이 아직 색인하지 않은 파일(방금 adb로 넣은 파일 등)은 색인될 때까지 나오지 않는다.
- `Android/data`, `Android/obb`는 이 권한으로도 읽을 수 없다.
- **Play Console에서 "모든 파일 액세스 권한" 신고서를 내야 한다.** 핵심 기능이 기기 전체의 문서를 찾아 여는 문서 뷰어라는 점과, SAF 폴더 선택으로는 여러 폴더에 흩어진 문서를 한 번에 보여줄 수 없다는 점을 근거로 적는다. 심사에서 반려될 수 있다.
- 스토어 등록 정보와 개인정보처리방침에 이 권한을 쓰는 이유와 문서를 기기 밖으로 보내지 않는다는 점을 적는다.

## 확인한 것 (API 36 에뮬레이터, R8 적용 릴리스 빌드, 임시 테스트 키로 서명)

저장소 권한 방식으로 바꾸기 전에 확인한 항목이다. 목록 표시는 아래 "확인하지 못한 것"을 참고한다.

- PDF, HWP, DOCX 뷰어 열기
- 설정 → 오픈소스 라이선스 목록 → 라이선스 전문 표시
- 시스템 파일 앱에서 열기:

| 파일 | 파일 앱이 보낸 MIME | 결과 |
|---|---|---|
| PDF | `application/pdf` | 선택 목록에 DocReader 표시, 열림 |
| DOCX | DOCX MIME | 바로 열림 |
| HWP | `application/x-hwp` | 바로 열림 |
| HWPX | `application/octet-stream` | 확장자 필터로 선택 목록에 표시, 열림 |
| TXT | - | DocReader가 목록에 나오지 않음 |

### 저장소 권한 방식 (API 30 에뮬레이터, debug 빌드, 2026-10-12)

- 첫 실행 시 권한 안내 화면 → 버튼으로 "모든 파일에 대한 액세스" 설정 화면 이동 → 허용 후 돌아오면 목록 표시
- `Download`와 `Documents` 하위 폴더의 문서가 모두 나오고, 지원하지 않는 파일(`.txt`)은 나오지 않음
- 목록에서 PDF, DOCX 열기, 공유 시트 표시

## 확인하지 못한 것

- 저장소 권한: API 26–29의 런타임 권한 흐름(`READ_EXTERNAL_STORAGE`, API 29의 `requestLegacyExternalStorage`), API 31 이상, SD 카드, R8 적용 릴리스 빌드, 공유받은 앱이 실제로 파일을 읽는지, 설정에서 권한을 회수한 뒤 안내 화면으로 돌아가는지
- 실기기, 삼성 "내 파일" 등 다른 파일 관리자 (MIME을 다르게 보낼 수 있다)
- API 26에서의 릴리스 빌드
- 실제 업로드 키로 서명한 빌드, AAB, Play Console 업로드
- 메일 첨부, 메신저 등 임시 권한만 주는 앱에서 열기

## 출시 전에 남은 일

- **앱 아이콘**: 지금 아이콘은 종이 한 장 모양의 임시 아이콘이다. 흔한 문서 아이콘 형태라 다른 문서 앱과 비슷해 보일 수 있으므로 고유한 디자인으로 바꾸는 것을 권한다.
- **앱 이름**: `DocReader` 그대로다. 스토어 표기명과 한국어 이름은 정하지 않았다.
- **버전**: `versionCode 1`, `versionName 1.0`.
- **targetSdk**: 36이다. Play 정책이 요구하는 수준인지 업로드 시점에 확인해야 한다.
- 스토어 등록 정보(설명, 스크린샷, 개인정보처리방침)는 만들지 않았다. 설명에 한컴이나 Microsoft와 제휴한 것처럼 보이는 표현을 쓰지 않는다.
