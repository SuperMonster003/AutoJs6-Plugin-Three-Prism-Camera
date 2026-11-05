<!--suppress HtmlDeprecatedAttribute, HttpUrlsUsage -->

<div align="center">
  <p>
    <picture>
      <source srcset="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera/blob/master/app/src/main/res/mipmap-night/ic_launcher.png?raw=true" media="(prefers-color-scheme: dark)" />
      <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera/blob/master/app/src/main/res/mipmap/ic_launcher.png?raw=true" alt="autojs6-plugin-three-prism-camera-ic-launcher" border="0" width="128" />
    </picture>
  </p>

  <p>CameraX 기반 사진 촬영, 연속 프레임 분석, 뷰파인더 미리보기, 프레임을 OCR, 바코드, YOLO 등 인식 모듈에 바로 전달 가능</p>

  <p>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera/releases"><img alt="GitHub release (latest by date)" src="https://img.shields.io/github/v/release/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera?label=Release"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera/issues"><img alt="GitHub closed issues" src="https://img.shields.io/github/issues/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera?color=A24232&label=Issues"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera/blob/master/LICENSE"><img alt="GitHub License" src="https://img.shields.io/github/license/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera?color=534BAE&label=License"/></a>
  </p>
</div>

******

### 언어 (Languages)

******

현재 README.md 는 다음 언어를 지원합니다:

- [简体中文 [zh-Hans]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera/blob/master/.readme/README-zh-Hans.md)
- [繁體中文 (香港) [zh-Hant-HK]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera/blob/master/.readme/README-zh-Hant-HK.md)
- [繁體中文 (台灣) [zh-Hant-TW]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera/blob/master/.readme/README-zh-Hant-TW.md)
- [English [en]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera/blob/master/.readme/README-en.md)
- [Français [fr]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera/blob/master/.readme/README-fr.md)
- [Español [es]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera/blob/master/.readme/README-es.md)
- [日本語 [ja]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera/blob/master/.readme/README-ja.md)
- 한국어 [ko] # 현재
- [Русский [ru]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera/blob/master/.readme/README-ru.md)
- [العربية [ar]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera/blob/master/.readme/README-ar.md)

******

### 소개

******

3-Prism Camera는 AutoJs6의 카메라 플러그인입니다. Jetpack CameraX를 바탕으로 스크립트에 사진 촬영, 연속 프레임 분석, 뷰파인더 미리보기를 제공합니다. 실시간 프레임은 AutoJs6의 `ImageWrapper`로 스크립트에 전달되므로, `ocr`, `barcode`, `yolo` 등 기존 모듈에 그대로 넘겨 실시간 코드 스캔, 문자 인식, 객체 탐지를 구현할 수 있습니다.

플러그인이 카메라를 보유하고 Binder로 AutoJs6와 통신합니다: 실시간 프레임은 공유 메모리 링 버퍼로 전달되고 (Android 7.0 - 8.0에서는 임시 파일), 사진은 JPEG로 AutoJs6에 전달되어 디코딩되거나 그대로 저장되며, 미리보기는 스크립트 화면의 `SurfaceView`에 직접 렌더링되므로 픽셀이 Binder를 거치지 않습니다. 세션이 열려 있는 동안 카메라 포그라운드 서비스가 실행되어, 스크립트가 백그라운드에 있거나 화면이 꺼져 있어도 프레임을 계속 받을 수 있습니다.

> 현재는 개발 미리보기 버전입니다: AutoJs6 플러그인 센터는 플러그인을 인식하지만, 사진 촬영, 실시간 프레임, 미리보기, 촬영 화면, `camera` 스크립트 모듈은 아직 제공되지 않습니다. 아래 내용은 1.0.0의 설계 목표이며, 진행 상황은 로드맵을 참고하세요.

******

### 기능 하이라이트

******

- 사진: EXIF 방향에 맞게 바로 세운 `ImageWrapper`를 반환하거나, 원본 JPEG (EXIF 포함)를 지정한 경로에 저장합니다; 플래시 모드, 화질, 해상도를 설정할 수 있습니다.
- 실시간 프레임: RGBA 프레임은 기본 640x480, 최대 1920x1080이며 기본적으로 화면 방향에 맞게 바로 세웁니다; 처리가 밀리면 프레임을 버리고 개수를 세며, 쌓아 두지 않습니다.
- 미리보기: ui 스크립트나 플로팅 창의 `SurfaceView`를 세션에 넘기면 뷰파인더가 표시되며, 실시간 프레임 및 사진 촬영과 동시에 사용할 수 있습니다.
- 제어: 렌즈 선택, 해상도, 목표 프레임 속도, 손전등, 플래시 모드, 줌, 노출 보정, 탭하여 초점 맞추기를 지원하며 각 범위와 현재 상태를 읽을 수 있습니다.
- 촬영 화면: `camera.capture({ ui: true })`로 플러그인 자체 촬영 화면을 열고, 사용자가 셔터를 누르면 스크립트가 사진을 받으며 취소하면 `null`을 반환합니다.
- 백그라운드 지원: 세션 동안 카메라 포그라운드 서비스가 실행되어 백그라운드나 화면이 꺼진 상태에서도 프레임이 전달됩니다; 스크립트가 끝나거나 AutoJs6가 중지되면 카메라를 자동으로 해제합니다.

******

### 사용 방법

******

1. [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera/releases)에서 APK를 내려받아 설치합니다. 설치 패키지는 하나이며 모든 기기에서 사용할 수 있습니다.
2. 플러그인을 열고 안내에 따라 카메라 권한을 허용합니다; 스크립트에서 `camera.requestPermission()`을 호출해 권한 화면을 열 수도 있습니다.
3. 스크립트에서 `camera` 모듈을 사용하려면 AutoJs6를 내부 버전 6640 (6.8.1) 이상으로 업데이트하고, 플러그인 센터에서 이 플러그인을 활성화하세요.

> 플러그인은 Android 7.0 (API 24) 이상 기기를 지원합니다. 모든 기기에서 쓰는 APK는 하나뿐입니다; CameraX와 Compose에 딸린 소량의 네이티브 라이브러리 (합계 약 200 KB)는 16 KB 메모리 페이지에 맞춰 정렬되어 있어, 16 KB 메모리 페이지를 쓰는 새 기기에도 설치할 수 있습니다.

******

### 스크립트 API

******

스크립트는 전역 모듈 `camera` (`$camera`로도 사용 가능)로 이 기능을 사용합니다 (1.0.0 설계 초안이며, 정식 내용은 릴리스 버전의 문서를 따릅니다):

```javascript
// Take one photo and keep the original JPEG.
let photo = camera.capture({ path: files.join(files.getSdcardPath(), "prism.jpg") });
toastLog(photo.getWidth() + "x" + photo.getHeight());
photo.recycle();

// Scan barcodes from live frames; a frame is valid only inside the callback.
let session = camera.open({ resolution: "1280x720" });
session.analyze((img, meta) => {
    let codes = barcode.detect(img);
    if (codes.length > 0) {
        toastLog(codes[0].rawValue);
        session.close();
    }
});
events.on("exit", () => session.close());

// Let the user take the picture on the plugin's capture screen; cancelling returns null.
let shot = camera.capture({ ui: true });
```

- `camera.capture(options)`는 열기, 촬영, 닫기를 한 번에 수행합니다; `camera.open(options)`는 독점 세션을 열며, 세션 객체는 `preview`, `analyze`, `capture`, `torch`, `zoom`, `exposure`, `focusAt`, `state`, `close`를 제공합니다.
- 프레임 수명: `analyze` 콜백의 `img`는 콜백이 실행되는 동안에만 유효하며 콜백이 반환되면 즉시 회수됩니다; 콜백 밖에서 사용하려면 `img.clone()`을 호출하고, 다 쓴 뒤 직접 `recycle()`하세요.
- `camera.list()`는 렌즈와 해상도, 줌 범위, 노출 범위를 나열합니다; `camera.capabilities()`는 계약 버전, 기능 목록, 권한 상태를 반환합니다.
- 세션은 동시에 하나만: 두 번째 `open`은 `CAMERA_BUSY`로 실패하며 점유자를 알려 줍니다; 스크립트가 끝나면 세션은 자동으로 닫힙니다.

******

### 자주 묻는 질문

******

#### 카메라를 AutoJs6에 내장하지 않고 플러그인으로 만든 이유는 무엇인가요?

카메라에는 CameraX와 포그라운드 서비스 권한이 필요합니다. 플러그인으로 두면 카메라가 필요 없는 사용자는 그 용량과 권한을 부담하지 않아도 되고, 플러그인은 AutoJs6와 별도로 업데이트할 수 있습니다.

#### 스크립트가 백그라운드에서도 프레임을 받을 수 있나요?

네. 세션 동안 카메라 포그라운드 서비스가 실행되며, 알림에 카메라 사용 중임이 표시됩니다. 일부 제조사 시스템은 백그라운드 카메라를 추가로 제한하므로, 호환 상황은 로드맵의 기기 매트릭스를 참고하세요.

#### 플러그인이 사진을 갤러리에 저장하나요?

아니요. 플러그인은 저장소 권한을 요청하지 않으며 시스템 갤러리에도 쓰지 않습니다; 스크립트가 `path`를 넘긴 경우에만 AutoJs6가 해당 경로에 사진을 씁니다.

******

### 권한과 보안

******

플러그인은 권한 범위를 최대한 좁게 유지합니다:

- 보호된 진입점: 기능 서비스와 활성화 진입점은 AutoJs6 플러그인 권한 (`org.autojs.permission.PLUGIN`)으로 보호되어 다른 앱이 직접 호출할 수 없습니다; 포그라운드 서비스와 투명 시작 화면은 내보내지 않습니다.
- 카메라: `CAMERA`와 카메라 유형 포그라운드 서비스 권한만 요청합니다; 카메라는 세션 동안에만 사용하며, 세션이 닫히거나 스크립트가 끝나거나 AutoJs6가 중지되면 즉시 해제합니다.
- 네트워크와 저장소: 네트워크와 저장소 권한을 요청하지 않습니다; 프레임과 사진은 플러그인과 AutoJs6 사이에서만 오가며 시스템 갤러리에 기록되지 않습니다.
- 독점 사용: 세션은 동시에 하나만 열 수 있으며 (스크립트 또는 플러그인 촬영 화면), 점유자는 오류 메시지에 표시됩니다.

******

### 플러그인 인터페이스

******

다음 정보는 AutoJs6 호스트와 플러그인 개발자를 위한 것으로, 호스트는 이 식별자들로 플러그인을 발견하고 기능을 협상합니다:

```text
application id: io.github.supermonster003.autojs6.plugin.three.prism.camera
plugin id: three-prism-camera
engine: camera
variant: default
info service: org.autojs.plugin.INFO (category three-prism-camera)
capability service: org.autojs.plugin.CAMERA (category camera)
wake action: org.autojs.plugin.action.WAKE
minimum host build: 6640
native libraries: libimage_processing_util_jni.so, libsurface_util_jni.so (CameraX), libandroidx.graphics.path.so (Compose); one APK for every ABI, stored uncompressed and 16 KB page-aligned
```

******

### 개발 로드맵

******

계획, 설계 결정, 진행 상황은 ROADMAP.md의 체크리스트로 관리합니다. 체크되지 않은 항목은 계획이며 현재 버전의 기능이 아닙니다.

- [ROADMAP.md 보기](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera/blob/master/ROADMAP.md)

******

### 릴리스 기록

******

#### v1.0.0

_2026/11/05_

- `힌트` 개발 미리보기: AutoJs6 플러그인 센터가 플러그인을 인식할 수 있습니다. 사진 촬영, 실시간 프레임, 뷰파인더, 촬영 화면, camera 스크립트 모듈은 아직 제공되지 않습니다
- `힌트` AutoJs6 6.8.1 (6640) 이상이 필요합니다 (정확한 최소 버전은 호스트 쪽 변경이 반영된 뒤 기입됩니다)
- `힌트` 현재 아이콘은 후보 디자인이며 이후 바뀔 수 있습니다
- `기능` 플러그인 골격: Wake 활성화 프로토콜, 플러그인 센터 식별 서비스, 카메라 기능 서비스 (임시)
- `기능` 모든 기기에 쓰는 APK 하나만 제공되며, CameraX와 Compose에 딸린 네이티브 라이브러리는 16 KB 메모리 페이지에 맞춰 정렬되어 있습니다
- `기능` 10개 언어의 README, 플러그인 센터 안내, 변경 기록을 JSON 원본에서 일괄 생성
- `의존성` Jetpack CameraX 1.6.2 추가 (카메라 열기, 사진 촬영, 프레임과 뷰파인더 제공)
- `의존성` Jetpack Compose BOM 2026.09.00 추가 (플러그인 화면)
- `의존성` AndroidX Activity Compose 1.12.2 추가
- `의존성` AndroidX Core KTX 1.16.0 추가

##### 더 많은 릴리스 기록은 다음을 참고

* [CHANGELOG.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera/blob/master/app/src/main/assets/doc/CHANGELOG-ko.md)

******

### 빌드

******

이 절은 소스에서 플러그인을 빌드하려는 개발자를 위한 것입니다.

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest
.\gradlew.bat :app:appendDigestToReleasedFiles
py .python\generate_markdown.py --check
py .python\generate_icon_studio.py --check
py .python\check_repository.py
```

******

### 라이선스

******

프로젝트 코드는 [Mozilla Public License 2.0](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera/blob/master/LICENSE)으로 배포됩니다. 서드파티 구성 요소의 라이선스는 서드파티 고지를 참고하세요.

- [제3자 고지 (THIRD_PARTY_NOTICES.md)](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera/blob/master/THIRD_PARTY_NOTICES.md)

******

### 관련 링크

******

- AutoJs6 프로젝트: https://github.com/SuperMonster003/AutoJs6
