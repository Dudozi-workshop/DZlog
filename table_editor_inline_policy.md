# Table Editor Inline Policy

이 문서는 테이블 에디터의 inline 편집 정책 기준 문서다.
정책 충돌이 생기면 이 문서를 임의로 덮어쓰지 말고 먼저 확인한다.

## 1. SSOT

셀 편집의 실제 데이터 SSOT는 항상 `TableTemplateState`다.
`inlineEdit.editingValue`는 저장 전 데이터 본체가 아니라 CELL_EDIT 입력 UI가 들고 있는 현재 문자열 상태다.

## 2. 값 반영 방식

셀 편집 입력은 `onEditingValueChange` → `TableEditorInlineEditActions.applyInlineValueChange(...)` 경로를 통해 즉시 template에 반영된다.
값 반영 본체는 commit 시점이 아니라 입력 시점이다.

## 3. undo 세션 정책

한 셀 편집 세션에서 undo snapshot은 1회만 생성한다.
세션 시작 기준은 첫 `onValueChange` 시점이다.
같은 세션의 추가 입력은 undo snapshot을 다시 만들지 않는다.
셀 편집도 메인 undo의 대상이다.

## 4. commitIfNeeded의 역할

`commitIfNeeded(...)`는 값 반영 본체가 아니다.
현재 정책에서 이 함수는 세션 종료, validation, counter conflict 처리에 집중한다.
현재 counter 값 파싱 helper와 counter commit resolver는 분리되어 있으며, parsing은 입력/commit 공용 helper가 맡고 commit 시점의 검증 / conflict / normalize / latch 계산은 resolver가 맡는다.

다음 상황에서 세션 종료 보조로 사용된다.
- 다른 셀 선택 시
- 패널 모드 변경 시
- 전체 저장 전
- 초기화 직전
- undo 적용 전후 세션 state 정리

## 5. 하단 액션 체계

CELL_EDIT 패널 하단의 저장/되돌리기 버튼은 제거했다.
하단 액션 체계는 메인 3버튼으로 통일한다.

- 저장 = 전체 저장
- 초기화 = 화면 진입 시점 전체 복귀 + undo stack clear
- 언두 = 셀 편집 포함 전체 편집 한 단계 되돌리기

셀 편집의 별도 save/revert 개념은 유지하지 않는다.
복구 개념은 메인 undo 기준으로 통일한다.

## 6. 세션 상태와 스냅샷

`editSessionOriginalCellState`는 선택 셀 기준 세션 원본 스냅샷 보조 상태다.
현재는 CELL_EDIT 세션 중 선택 셀 원본 보존과 snapshot 동기화 보조 판단에만 사용한다.
이 상태 역시 template SSOT를 대체하지 않는다.

## 7. 변경 원칙

- 즉시 반영 정책을 임의로 commit 반영 구조로 되돌리지 않는다.
- 메인 3버튼 통일 정책을 임의로 깨지 않는다.
- 첫 `onValueChange` 기준 undo snapshot 정책을 임의로 바꾸지 않는다.
- 예외 UX가 필요해 보이면 먼저 확인한 뒤 결정한다.


## 8. 현재 최소 inline 상태 구성

현재 inline 관련 최소 상태는 다음 세 가지다.
- `inlineEdit`: 현재 CELL_EDIT 입력 UI 상태 (`editingCellId`, `editingValue`)
- `inlineEditSessionState`: 첫 `onValueChange` 이후 undo snapshot 1회 push 여부를 추적하는 세션 상태
- `editSessionOriginalCellState`: 선택 셀 기준 원본 스냅샷 보조 상태

`editSessionSnapshotCellId`는 별도 상태로 유지하지 않는다.
셀 식별은 `editSessionOriginalCellState?.cellId`로 충분하며, 같은 정책을 더 적은 상태로 유지한다.


## 9. Screen 책임 경계

`TableEditorScreen`은 inline 상태 보관, 현재 context 추출, 외부 의존성 브리지, applied state 반영에 집중한다.
inline 결과 해석, counter conflict / committed counter 적용, template apply mode 분기는 feature/editor helper가 맡고, Screen은 helper 호출을 조율하는 쪽에 가깝게 유지한다.
save 쪽의 얇은 `buildSaveApplyInput(...)` wrapper는 제거했고, inline 쪽도 별도 `buildInlineEditApplyInput(...)` helper 없이 `TableEditorInlineActionCoordinator`가 `TableEditorInlineEditApplyInput(...)`를 직접 조립한다.
Screen은 inline binding/context 제공과 applied state 반영에 집중하며, 현재 binding이 묶는 외부 의존성은 아래와 같다.
- `applyTemplateWithUndo`
- `updateTemplateDraft`
- counter conflict UI update
- committed counter 반영 bridge

## 10. Build recovery note (2026-03-24)

- 이번 변경은 리팩터링 확장 단계가 아니라 compileDebugKotlin 복구를 위한 연결 보정 단계다.
- Screen private/local 구현을 외부 binder가 직접 참조하지 않도록 `withDataType`를 공용 top-level helper로 정리했다.
- 얇은 신규 helper 추가보다 기존 binder/coordinator wiring 복구를 우선했다.
- weight reset은 별도 wrapper/helper를 재도입하지 않고 Screen action wiring에서 직접 복구하는 방식을 사용했다.

## 11. Screen thinning note (wrapper trim + action-axis naming)

- bottom panel mode 변경은 별도 build wrapper 없이 `requestBottomPanelModeChange(...)`에서 resolver input을 직접 구성한다.
- Screen은 상태 소유/브리지/reflect 책임을 유지하고, 판단은 resolver/binder에 둔다.
- 액션 축 명확화를 위해 selection+inline 편집 binder 명을 `TableEditorSelectionInlineEditingActionBinder`로 맞췄다.
- 현재 축 구분:
  - selected cell domain action: selected cell binder + `selectedCellActionBindings`
  - selection/focus action: `selectionActionBindings`
  - inline editing action: `inlineEditingActionBindings`

## 10. Build recovery note (2026-03-24)

- 이번 변경은 리팩터링 확장 단계가 아니라 compileDebugKotlin 복구를 위한 연결 보정 단계다.
- Screen private/local 구현을 외부 binder가 직접 참조하지 않도록 `withDataType`를 공용 top-level helper로 정리했다.
- 얇은 신규 helper 추가보다 기존 binder/coordinator wiring 복구를 우선했다.
- weight reset은 별도 wrapper/helper를 재도입하지 않고 Screen action wiring에서 직접 복구하는 방식을 사용했다.

## 11. Screen thinning note (wrapper trim + action-axis naming)

- bottom panel mode 변경은 별도 build wrapper 없이 `requestBottomPanelModeChange(...)`에서 resolver input을 직접 구성한다.
- Screen은 상태 소유/브리지/reflect 책임을 유지하고, 판단은 resolver/binder에 둔다.
- 액션 축 명확화를 위해 selection+inline 편집 binder 명을 `TableEditorSelectionInlineEditingActionBinder`로 맞췄다.
- 현재 축 구분:
  - selected cell domain action: selected cell binder + `selectedCellActionBindings`
  - selection/focus action: `selectionActionBindings`
  - inline editing action: `inlineEditingActionBindings`

## 10. Build recovery note (2026-03-24)

- 이번 변경은 리팩터링 확장 단계가 아니라 compileDebugKotlin 복구를 위한 연결 보정 단계다.
- Screen private/local 구현을 외부 binder가 직접 참조하지 않도록 `withDataType`를 공용 top-level helper로 정리했다.
- 얇은 신규 helper 추가보다 기존 binder/coordinator wiring 복구를 우선했다.
- weight reset은 별도 wrapper/helper를 재도입하지 않고 Screen action wiring에서 직접 복구하는 방식을 사용했다.

## 11. Screen thinning note (wrapper trim + action-axis naming)

- bottom panel mode 변경은 별도 build wrapper 없이 `requestBottomPanelModeChange(...)`에서 resolver input을 직접 구성한다.
- Screen은 상태 소유/브리지/reflect 책임을 유지하고, 판단은 resolver/binder에 둔다.
- 액션 축 명확화를 위해 selection+inline 편집 binder 명을 `TableEditorSelectionInlineEditingActionBinder`로 맞췄다.
- 현재 축 구분:
  - selected cell domain action: selected cell binder + `selectedCellActionBindings`
  - selection/focus action: `selectionActionBindings`
  - inline editing action: `inlineEditingActionBindings`

## 10. Build recovery note (2026-03-24)

- 이번 변경은 리팩터링 확장 단계가 아니라 compileDebugKotlin 복구를 위한 연결 보정 단계다.
- Screen private/local 구현을 외부 binder가 직접 참조하지 않도록 `withDataType`를 공용 top-level helper로 정리했다.
- 얇은 신규 helper 추가보다 기존 binder/coordinator wiring 복구를 우선했다.
- weight reset은 별도 wrapper/helper를 재도입하지 않고 Screen action wiring에서 직접 복구하는 방식을 사용했다.

## 11. Screen thinning note (wrapper trim + action-axis naming)

- bottom panel mode 변경은 별도 build wrapper 없이 `requestBottomPanelModeChange(...)`에서 resolver input을 직접 구성한다.
- Screen은 상태 소유/브리지/reflect 책임을 유지하고, 판단은 resolver/binder에 둔다.
- 액션 축 명확화를 위해 selection+inline 편집 binder 명을 `TableEditorSelectionInlineEditingActionBinder`로 맞췄다.
- 현재 축 구분:
  - selected cell domain action: selected cell binder + `selectedCellActionBindings`
  - selection/focus action: `selectionActionBindings`
  - inline editing action: `inlineEditingActionBindings`
