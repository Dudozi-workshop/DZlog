# M2 홈·템플릿 관리·전체 설정 연결 묶음

기준 e3cf21aa, 같은 Draft PR #76에 누적한다. 편집 묶음 집중 PASS797baaef/39436474811은
기존 근거로 보존하고 새 구현의 컴파일/조작 결과와 구분한다.

## 구현

- Android SettingsRootScreen의 표시 본문/섹션/품질 선택/스위치/브랜드/버전을
  SettingsRootContent로 이동한다. 새 iOS 설정 UI를 별도 제작하지 않는다.
- Android DataStore flow·품질 저장 busy/실패 Toast·BackHandler·권한/피드백 화면 이동·
  Google Play 결제/복원은 기존 어댑터에 유지한다. Pro 섹션은 동일 slot 본문으로 남는다.
  사진 품질의 Android CameraX 타입은 app에 유지하고 UI 경계는 기존 저장값0/1/2를 쓴다.
- 기존 카탈로그 이름변경/복제/삭제 순수 규칙을 shared의 기존 TableTemplateCatalog에
  공통 함수로 분리해 Android/iOS가 호출한다. 기존 Android quota gate·활성 fallback·
  ViewModel 정렬/영속 저장 순서를 유지하며 사진/DB/.dzlog 형식은 변경하지 않는다.
- iOS 홈 설정/템플릿 관리/편집/저장 정보→저장설정 이동을 기존 공통 UI로 연결한다.
  카탈로그 생성/선택/이름변경/복제/삭제와 각 문서의 번호·스코프·스타일을 저장한다.
  신규 템플릿은 Android처럼 미저장 draft이며 저장 전 폐기하면 목록에 남기지 않는다.
- iOS 문서는 신규 collection key(v2)에 저장한다. v2가 없을 때만 기존 v1 편집 문서를
  읽어온다. 기존 v1 값은 덮어쓰거나 지우지 않고 보존한다. 손상된 v2가 있으면 v1로
  조용히 초기화하지 않고 편집/저장을 잠근다. 모든 문서 strict decode와 ID 중복/
  활성 ID 유효성을 확인해 손상 항목이 다음 저장 때 누락되지 않게 한다.
- 품질/보조 셔터/Undo 저장경로 경고 선택은 iOS 설정 문서에 저장하고 재실행 복원한다.
  이는 향후 카메라에서 사용할 설정의 저장이며 현재 iOS 사진 품질/셔터 동작의 완성을 뜻하지 않는다.
- iOS 권한 관리·촬영 소리/진동 재생·백업 파일 가져오기/내보내기·도움말/크레딧 상세는
  이번 연결 범위에서 미완료다. 공통 설정 화면에서 누르면 사용 불가 안내를 표시한다.
  플랫폼 기능을 성공한 것처럼 표시하지 않으며 Android의 기존 목적지는 유지한다.

## 검사 방식

- Draft 기본 navigation 묶음: 새 홈/카탈로그/설정/재실행1개와 기존 전체 편집/저장1개,
  총2개만 실행한다. 일반 화면5개를 수정마다 반복하지 않는다.
- 수동 editor는 기존 편집1개, 수동 full/최종 Draft 해제는 전체7개. 부분 success는
  전체 QA 근거로 재사용하지 않는 기존 판정 규칙을 유지한다.
- 공통 테스트3개: collection+설정/번호/legacy 보존, 손상/중복/활성 ID 거부,
  이름변경/복제/삭제 fallback. 기존 UI·구조 회귀 테스트를 복제하지 않는다.
- 로컬 CI 판정15개 PASS, YAML/Bash 문법 및 mock xcode 명령에서 navigation2개 선택,
  host1회 빌드, full 기본값 유지 PASS. 원본 Pro slot 보존/공통 플랫폼 import 없음 확인.
- Android/shared JVM·iOS core·UI 빌드·실제 navigation/편집 조작은 CI 결과 전 미검증.
  이번은 공통 저장·정책/Android 경계가 바뀌므로 Android/core 회귀 대상이다.
- 실기기·정식 디자인 승인·AX/VoiceOver·다기기·실제 카메라/사진/앨범/권한/백업은
  미완료. T2.2 진행 중·Draft·미병합 유지. 출시 통합/튜토리얼 브랜치는 변경하지 않는다.

## 최초 CI 결과와 컴파일 수정 · 2026-10-21

- af79e8a6 iOS core 39478452802 성공.
- Android 39478452791 / UI 39478452800은 동일한 SettingsRootContent.kt:178
  컴파일 오류로 중단. 테스트 assertion 실패가 아니며 Android 단위·iOS 조작은 미실행이다.
- 함수 값으로 받은 onChangeAssistShutter를 ::로 참조하던 한 줄을 직접 전달로 수정한다.
  콜백 동작/상태/저장 정책은 그대로다. 다른 함수 참조는 기존 로컬 함수임을 확인했다.
- 이 수정의 컴파일/실제 조작은 새 CI 대기. core는 입력이 동일하므로 기존 성공 재사용 대상.
  최초 묶음의 Android 전체 검사는 아직 완료되지 않아 기존 성공으로 대체할 수 없다.
  iOS 조작은 navigation 집중2개를 유지하며 전체7개를 실행하지 않는다.
