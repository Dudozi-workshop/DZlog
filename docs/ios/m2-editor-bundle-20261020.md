# M2 표 편집 전체 흐름 묶음

기준 #76 3e8d4048. 사용자 요청에 따라 패널별 승인/반복 검사를 중단하고 내용·구조·스타일·저장설정을
기존 편집 화면으로 연결해 한 묶음으로 검수한다. 새 PR/별도 iOS 편집기를 만들지 않는다.

## 이번 구현

- 기존 TableEditorV2Screen의 UI·세션·선택·4탭·셀 편집/타입 선택·날짜/순환문구·저장 규칙·
  미리보기 Canvas·수정 이력·Undo/Redo·이탈 확인/저장 실패 처리를 sharedUi로 이동한다.
- 기존 선택/구조 삽입·삭제·병합/복원·슬롯 처리·Undo 순수 엔진과 슬롯 모델은 shared로 이동해
  Android/iOS가 한 코드를 사용한다. 계산 규칙을 iOS에 복사하지 않는다.
- Android 원래 TableEditorV2Screen 시그니처는 어댑터로 유지한다. 기존 DataStore 설정 flow,
  MediaStore 기반 번호 초기화/동기화, 기존 저장 콜백·시스템 BackHandler·Toast는 app에 남긴다.
  sharedUi 플랫폼 인터페이스로 같은 호출을 연결한다. 기존 Android 사진/DB/설정 구조 불변.
- UUID는 기존 플랫폼 UUID 함수를 공통 wrapper로 재사용한다. 날짜는 기존 CaptureTime/formatter를
  사용하며 Android 기본 locale/timezone/시각을 유지한다. 기존 tick 공식/포맷 상수도 이동한다.
- 재정렬 UI는 기존 libs.reorderable(3.0.0)을 sharedUi에서도 사용한다. 버전을 올리지 않는다.
- iOS 기본 진입은 기존 홈이며 표 편집 버튼에서 같은 4탭 편집기로 이동한다. 기존 미리보기
  검사는 --preview-dialog/--large-table로 명시한다. 분리 확인 화면을 더 추가하지 않는다.
- iOS 편집 문서는 기존 savedTableTemplatesToJson/FromJson으로 저장하며 scope/저장 방식/다음 번호
  등 편집 메타데이터만 문서 wrapper로 묶는다. NSUserDefaults의 한 값에 저장하고 재실행 시 복원한다.
  .dzlog 백업 Codec을 저장용으로 오용해 번호를 초기화하지 않는다. .dzlog 형식 변경 없음.
- 문서 파싱 실패 시 기존 값을 덮어쓰지 않도록 편집을 잠근다. 저장 실패는 기존 공통 저장
  실패 처리로 전달하고 draft를 유지한다. 정상 저장 시에만 공통 coordinator가 저장 완료로 표시한다.
- iOS 사진 기준 번호 동기화는 비활성화하고 미연결 상태를 표시한다. 실제 사진 저장/권한/카메라는
  후속 묶음이다. 저장경로 미리보기의 Android 표기는 기존 논리 규칙이며 iOS 물리 경로 검수가 아니다.

## 묶음 검수

기존 홈/템플릿 미리보기/가로·세로/저장 정보 검사를 유지한다. 분리 스타일·구조2개는 전체 편집
검사1개로 대체한다. 총6개 예정이며 아래를 실제 조작으로 확인한다.

1. 홈→표 편집→셀 내용 입력.
2. 구조 탭 행 추가→Undo/Redo와 실제 행 경계 확인.
3. 스타일 탭 투명 배경→투명도 행 숨김.
4. 저장설정에서 파일명 직접입력 규칙 추가·값 편집·미리보기 확인.
5. 공통 저장→홈 복귀→앱 종료/재실행→입력/행/스타일/규칙 유지.

통합 검사만 별도 iOS test preference key를 사용하고 그 key만 초기화한다. 일반 편집 데이터는
초기화하지 않는다. 앱 재실행 후 실제 표시와 상태를 검증하며 이벤트 요청만으로 PASS 처리하지 않는다.

## 검증 상태와 제한

- 정적 확인: 이동 파일 원문/공개 범위 비교, common→Android import 역참조 없음, diff check,
  기존 CI 판정 규칙14개 PASS.
- 새 편집 문서 Codec에 기존 counter seed/ID/스타일/스코프 보존과 손상/미지원 버전 거부2개만 추가한다.
  기존 구조/세션/저장 회귀 검사를 복제하지 않는다.
- Android/shared JVM·iOS core·UI 빌드/실제 조작은 CI 확인 전 미검증이다. 이번은 공통 엔진/상태/
  저장 경계가 바뀌어 전체 회귀 대상이며 패널별로 반복 실행하지 않는다.
- 정식 디자인 승인·실기기·140%/다기기·AX/VoiceOver·사진 저장·iOS 물리 경로/앨범/카메라는
  미검증이다. 표 편집 묶음의 구현과 자동 검증/사용자 검수 완료를 구분한다.
- T2.2 전체 화면 연결은 진행 중으로 유지한다. 기존 온보딩/튜토리얼/출시 integration 및 보호 PR은
  건드리지 않고 같은 Draft #76에 누적한다. 사용자 요청대로 빌드 대기 없이 실행 링크를 전달한다.

## 2026-10-20 빌드 실패 수정

8df89a5의 Android 39407396840, iOS core 39407396835, UI 39407396899는 모두
테스트 실행 전 공통 코드 컴파일에서 실패했다. 테스트 실패나 UI 검수 결과로 기록하지 않는다.

- Android: common CellId.kt에 일반 함수를 추가하면서 jvmMain/CellId.kt와 CellIdKt JVM facade가
  중복 생성됐다. 새 wrapper만 TableElementId.kt로 분리하고 UUID actual 구현/호출 정책은 유지한다.
- iOS core/UI: 이동된 구조 엔진의 JVM Map.putIfAbsent가 common에서 해석되지 않았다.
  non-null TableCellState map의 getOrPut으로 바꿔 기존 좌표가 있으면 최초 root를 유지한다.
- 두 수정 뒤 Android/shared JVM·iOS core·UI 검사는 재실행 대기이며 PASS가 아니다.
  로컬은 Java 21/Gradle 도구가 없어 컴파일하지 못하며 CI에서 확인한다.

### 다음 컴파일 단계 경계 수정

7c99e9fc: iOS core 39408585742는 실제 컴파일/시뮬레이터 테스트 단계 성공. Android
39408585847/UI 39408585689는 sharedUi 컴파일 실패로 실제 편집 조작 전 중단됐다.
- 누락된 기존 RotatingPhraseSetCreationPolicy를 shared로 이동한다. 공개 범위 외 본문 동일.
  기존 Android 정책 테스트는 같은 클래스를 계속 검사한다.
- isImeVisible 및 DialogProperties.decorFitsSystemWindows는 Android actual로 보존한다.
  iOS는 IME inset으로 표시 여부를 구하고 지원되는 공통 full-width DialogProperties를 사용한다.
- 수정 후 재검사 전이며 전체 표 편집 흐름은 미검증이다. 동일 Draft #76 유지.

### c930e078 결과 / 편집 검사 동선 수정

- Android 39412082095: 실제 전체 unit step 및 billing 검증 성공.
- iOS core 39412082242: 실제 컴파일/시뮬레이터 테스트 step 성공.
- UI 39412082263: iPhone/시뮬레이터 shared UI framework 및 UIKit host 빌드 성공, PNG cmp 유지.
  실제 조작6개 중5개 성공, 편집 묶음1개 실패. artifact12259793923.
- 실패 위치 Swift168: 셀 선택으로 패널을 연 뒤 실제 입력칸을 누르기 전에 native
  TextView/TextField를 찾았다. AX 기록에는 셀과 하단 입력칸이 같은 label의 Button으로
  각각 있고 셀 편집 닫기/입력창 확대도 표시되어 있다. 입력 전 실제 하단 입력칸을 터치하도록
  검사 동선만 수정한다. 앱 코드는 바꾸지 않는다. 입력·구조·스타일·저장/재실행은 아직 미검증.
- 변경은 iOS 검사/기록뿐이므로 Android/core는 동일 입력 성공 근거 재사용 대상이다.
  UI 재검사는 기존 정확한 framework 캐시를 사용 가능하며 최종 결과 전 PASS 처리하지 않는다.

### e6105510 후속 입력 검사 수정

UI 39424110997: 기존5개 성공, 편집1개 실패(Swift178). 하단 입력칸 터치로 키보드가
열리고 편집 레이아웃이 IME 높이로 줄어든 것은 실제 AX 로그로 확인됐다. 하지만 포커스 후에도
Compose 입력칸은 Button이며 native TextField/TextView가 없어 검사 대기 조건이 잘못됐다.
앞선 입력칸 터치 수정만으로는 부족했다. 키보드 존재 확인 후 app.typeText로 실제 입력을 보내고
화면 OCR로 입력 반영을 확인한다. 파일명 입력도 같은 Compose 입력 방식으로 맞춘다.
앱 코드는 변경하지 않으며 구조/스타일/저장·재실행 완료를 소급 PASS 처리하지 않는다.
Android39424110866/core39424110857은 이전 동일 성공 입력 근거 재사용이며 신규 전수검사 아님.
