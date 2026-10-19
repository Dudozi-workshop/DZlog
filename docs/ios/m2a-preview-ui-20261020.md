# M2-A · existing template preview UI PoC

기준: M1-D `6566f9fd333c1817a9d0caf24827abc91a5346da`.

## 재사용 범위

- 기존 TableTemplateReadOnlyPreviewDialog를 sharedUi/commonMain으로 이동한다.
  본문/버튼/색상/크기/스크롤/콜백은 동일하고 모듈 경계 때문에 visibility만 public이다.
- DDZColor/DDZTypography/DDZSpacing/DDZTheme은 원문 그대로 이동한다.
  Android는 sharedUi의 같은 구현을 사용한다. 기존 Android 전용 TableRender,
  Bitmap/Canvas/사진 저장/미디어·DB/설정·온보딩/튜토리얼은 수정하지 않는다.
- 기존 TableLayoutCalculator와 templatePreviewCellBounds를 shared core로 이동한다.
  root 선택/covered backing cell 제외 규칙을 resolveTableRootCells로 한 번만 구현하고
  Android editor resolveStructure도 이를 사용한다. 나머지 편집 동작은 유지한다.
- 기존 geometry 테스트2개를 commonTest로 이동, covered backing cell 사례1개 추가.
  전체 Android 편집·렌더·백업/촬영 회귀 및 공통 iOS 테스트로 검증한다.

## 빌드 경계

- sharedUi는 Android library + iPhone/simulator Compose UI module이다.
  기존 app 전체를 다른 앱으로 재작성하지 않는다. Android는 이 모듈을 직접 사용한다.
- Kotlin2.0.21/AGP8.11.1/기존 Compose BOM 유지. Compose Multiplatform1.7.0을
  현재 Kotlin/Jetpack 계열과 맞춰 고정한다. Android 의존 그래프의 실제 해석 결과는
  CI에서 확인하며 확인 전 버전 영향이 없다고 단정하지 않는다.
  https://github.com/JetBrains/compose-multiplatform/releases/tag/v1.7.0
- 기존 -PdzlogSharedOnly core 검사는 sharedUi를 구성하지 않는다.
  -PdzlogSharedOnly=true -PdzlogUiPoc=true는 Android SDK 없이 native UI만 구성한다.
- iPhone 대상 main compile과 simulator framework link를 수행한다. simulator PoC는
  동적 framework와 최소 UIKit AppDelegate로 실행한다. Swift는 호스트 역할만 한다.
- iosApp/PoCApp과 sharedUi/iosMain의 합성 fixture는 PoC 전용이다.
  기존 사진/DB/active template/설정 또는 production 앱 ID를 읽거나 쓰지 않는다.

## 증빙과 판정

- macOS CI에서 실제 framework/Swift host를 빌드하고 simulator install/launch,
  프로세스 생존 및 screenshot을 확인한다. 단순 컴파일 성공과 UI 실행을 구분한다.
- screenshot 및 launch/process/app log를 Actions artifact에 보관한다.
  launch/screenshot은 클릭·스크롤 제스처 검증이 아니다. 자동 캡처를 직접 확인하고,
  폰트/정렬/스크롤/선택·닫기 등 시각·상호작용 승인 범위는 별도로 기록한다.
- OS 기본 글꼴은 Android/iOS에서 다를 수 있다. 크기/weight/token 원문 재사용과
  픽셀 동등성은 구분한다. 140% 글자/다양한 화면/접근성 검수는 후속이다.
- 앱 전체 iOS UI·카메라·저장/권한/갤러리·실기기·배포 완료를 뜻하지 않는다.
  미검증을 PASS로 표시하지 않고 Draft·미병합 유지한다.
