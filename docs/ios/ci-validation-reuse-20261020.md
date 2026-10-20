# iOS migration CI reuse · 2026.10.20

기준: M2-B #74 bfb1a0a7. 전수 테스트 목록/앱 운영 코드는 유지한다.

## 확인한 병목

M2-B 수정5회에서 Android/core/UI 작업15개가 실행됐다. Android 작업은245~324초,
iOS core183~253초, UI433~761초. UI 안에서 프레임워크203~356초,
별도 첫 실행97~179초, Xcode 검사 단계65~226초가 소요됐다.
소요 시간은 runner 실행 시간이며 앱 촬영/저장 성능 측정값이 아니다.

## 변경

- PR의 누적 diff가 계속 경로 필터에 걸리는 문제를 workflow 내부 입력 비교로 보완한다.
  같은 PR/저장소/base의 최근24시간 내 성공 결과와 플랫폼별 Git blob·경로·mode·빌드
  입력 SHA-256이 같을 때만 재사용한다. base가 head의 조상이 아닐 때는 재사용하지 않는다.
- 앱/공통 로직/빌드 설정 변경 시 해당 전체 검사를 실행한다. Android 전수 목록을
  임의 선택형 테스트로 바꾸지 않는다. push 검사는 항상 전체 실행한다.
- 실패·다른 PR/base·잘린 Git tree·권한/API 오류·새로운 불명 파일은 재사용하지 않는다.
  workflow/guard 코드 변경도 입력에 포함한다. 재사용 summary에는 이전 근거 링크를
  남기고 이번 실행에서 테스트하지 않았음을 명시한다.
- UI 테스트만 바뀌면 Android/core 성공 결과는 재사용하고 UI 검사는 실행한다.
  native framework는 같은 PR/input hash/Xcode·Java/OS·architecture의 exact cache만
  사용한다. restore-key fallback은 없다. compile 성공 직후 저장하므로 뒤의 UI 테스트
  실패 때문에 같은 framework를 다시 컴파일할 필요가 없다. 캐시 오류는 새 빌드로 보완한다.
- 첫 화면 실행/캡처와 실제 조작 검사를 Xcode 흐름1회로 합친다. 기존 수동 launch
  스크립트는 보존한다. 첫 실행 foreground 확인 및 screenshot을 xcresult에 남긴다.

## 검증

- 로컬 Node 정책 검사: 같은 입력 재사용, UI 변경 시 Android/core 분리, 공통/빌드/불명
  입력 변경, 이전 실패/다른 PR·base·저장소/오래된 근거 거부, API/tree 오류 시 전체 실행.
- 실제 CI에서 최초 기준 검증 후 UI 테스트 파일만 수정해 Android/core 재사용 및 native
  framework exact cache hit, UI 실제 검사 실행을 확인한다. 측정 전 절감률을 확정하지 않는다.
- 설정 변경이므로 최초에는 전체 검사가 필요하다. 실제 warm 실행 결과/HEAD는 PR과 기존
  Notion 마이그레이션 문서에 기록한다. Draft·미병합, Android 기준 d6e53c9a 및 #37/#62 보호.

앱 실행 성능·실기기 촬영/저장·접근성 AX 판정 미해결 사항은 CI 최적화와 별개다.


## 표시 UI 변경 시 Android 전수 검사 축소 · 2026.10.20

사용자 요청: 공통 표시 패널을 옮기거나 다듬을 때마다 Android607/shared JVM61개를 반복하지 않는다.

- 전체 Android 입력이 동일하면 기존처럼 검사 재사용. 입력이 달라도 검토된 표시 패널 목록의
  이동/수정뿐이고 unit 관련 입력 hash가 같으면 Android 앱·테스트 소스만 컴파일한다.
  실행 task는 :app:compileDebugKotlin :app:compileDebugUnitTestKotlin이다.
- 표시 패널은 guard 내부의 구체적인 경로 목록이다. Android/sharedUi 양쪽 경로를 제외해
  같은 패널의 모듈 이동을 인식한다. Android 어댑터/세션/상태/데이터/정책/번호·이름·경로/
  실제 저장/테스트/빌드/검사 설정/불명 파일은 unit 입력에서 제외하지 않는다.
- 같은 PR/base/저장소·최근24시간 성공·조상 기준을 유지한다. 성공 후보 최대20개 중 전체
  unit step이 실제 success인 job이 있는 실행만 근거로 사용한다. compile-only/REUSE job은
  전체 테스트 성공 근거로 취급하지 않는다. API/입력 불명은 전체 검사로 복귀한다.
- push와 Draft 해제 ready_for_review 시 전체 검사를 강제한다. 최초 정책 변경과 이번
  TableEditorPolicy 상수 이동은 unit 입력 변경이므로 이번1회는 전체 검사 대상이다.
  이후 같은 표시 패널만 바꾸는 커밋은 컴파일만 한다. iOS 실제 조작 검사는 별도로 유지한다.
- summary에 이전 근거와 이번 unit 테스트 미실행을 명시한다. 컴파일 성공을 새607/61개
  통과로 보고하지 않는다. 실제 절감 시간은 CI 결과 확인 전 확정하지 않는다.
- 로컬 정책 검사14개/원본 비교/diff/YAML 정적 확인. 실제 compile-only CI 분기 검증은
  최초 기준 성공 후 다음 표시 UI 변경에서 확인할 예정이며 아직 PASS가 아니다.
