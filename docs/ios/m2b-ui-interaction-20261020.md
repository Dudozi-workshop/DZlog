# M2-B · iOS preview interaction verification

기준: M2-A #73 `ae07495c8cad16af4cee450023db4358b9221cee`.

- 기존 sharedUi 공통 dialog/theme 구현은 변경하지 않는다.
- 최소 UIKit 호스트를 동일한 앱 ID/실행 진입점으로 Xcode 프로젝트에 연결한다.
  공통 framework는 먼저 Gradle로 빌드한다. 현재 프로젝트는 simulator 검증용이며
  실제 개발 기기 signing·Archive·배포 설정 완료가 아니다.
- XCTest로 실제 iOS 접근성 노드를 찾아 닫기→재열기→선택→재열기/닫기 후 선택
  유지, 큰 합성 표의 가로/세로 드래그로 마지막 열/행 노출을 검사한다.
  숨겨진 표 셀 노출 없이 단순 drag 호출만 성공한 경우 PASS로 처리하지 않는다.
- 큰 표는 iOS PoC의 `--large-table` 인자에서만 사용하고 메모리에만 존재한다.
  Android 공통 화면·기존 사진/DB/설정·active template 저장은 변경하지 않는다.
- 버튼 hittable/화면 안 위치는 기본적인 가림 확인이다. 전체 Safe Area·140% 글자·
  VoiceOver·다기기·실기기·사용자 시각 승인·전체 화면 이동 완료와 구분한다.
- CI에서 xcresult(스크린샷/접근성 계층 포함) 및 실행 로그를 보관한다.
  최종 PASS/FAIL 근거와 HEAD는 PR/기존 Notion 문서에 기록한다. Draft·미병합 유지.
- Compose 1.7의 기본 접근성 동기화는 서비스 실행 여부에 의존한다. 화면은 표시되지만
  XCTest 계층이 비는 것을 확인해 PoC 호스트에 Always 동기화를 명시한다.
  Android 및 공통 dialog/theme에는 적용하지 않는다. 프로젝트는 arm64 simulator 전용이다.

실행 순서:

```sh
bash ./gradlew -PdzlogSharedOnly=true -PdzlogUiPoc=true :sharedUi:linkDebugFrameworkIosSimulatorArm64
bash scripts/ios/test-ui-poc.sh
```

Xcode에서 `iosApp/DZlogUiPoc.xcodeproj` → `DZlogUiPoc` scheme을 열어 simulator 실행/검사를
재현할 수 있다. 홈/템플릿/표 편집/설정/갤러리 전체 연결은 후속 작은 패치로 진행한다.

참고: [Compose 접근성/XCTest 연계](https://blog.jetbrains.com/kotlin/2024/02/compose-multiplatform-1-6-0-release/),
[Apple XCUIElement](https://developer.apple.com/documentation/xcuiautomation/xcuielement).
[Compose 1.7 접근성 동기화 설정](https://kotlinlang.org/docs/multiplatform/compose-ios-accessibility.html#choose-the-tree-synchronization-option).
