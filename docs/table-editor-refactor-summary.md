# Table Editor Refactor Summary (Step 1–3)

## 1) Current SSOT / Layering

- `ui/table/TableEditorScreen`
  - Owns Compose state, wiring, and `LayoutTabContent` invocation.
  - Delegates save / back / transient-state policies to feature-layer helpers.
- `feature/table/editor/coordinator`
  - `TableEditorSaveCoordinator`: table template/style/placement save transaction (with rollback).
  - `TableEditorExitCoordinator`: back-press → effect (`ExitNow` / `OpenUnsavedChangesDialog`).
- `feature/table/editor/handlers`
  - `TableEditorTransientStateHandlers`: clear file-name / path transient UI state.
  - `TableEditorModeTransitionHandlers`: bottom-panel mode transition effects.
  - `TableEditorSlotListHandlers`: slot list remove/move/filter-by-cell policies.
  - `TableEditorSlotDraftHandlers`: apply normalized slot UI draft back to `TableTemplateState`.

## 2) Screen responsibilities (after this patch)

`TableEditorScreen` is responsible for:

- Owning `remember { ... }` Compose state for:
  - Template / style / placement
  - Inline edit / selection / undo manager
  - File-name / path slot UI state
  - Counter preview / scope sync state
- Wiring:
  - `LayoutTabUiState` construction
  - `LayoutTabActions` lambdas → handler/coordinator 호출
  - Dialog host composition (`TableEditorUnsavedChangesHost`, format/rotating dialogs, etc.)
- Coordinating:
  - On save: call `TableEditorSaveCoordinator.persist(...)` and apply result (undo clear, snapshot reset, toast, `onBack`).
  - On back: call `TableEditorExitCoordinator.onBackPressed(...)` and either show dialog or exit.

정책/판단 로직은 아래와 같이 외부로 이동했습니다.

- Save 트랜잭션 정책: `TableEditorSaveCoordinator`
- Save 성공/실패 후 상태 반영 규칙: `TableEditorSaveResultApplier`
- save 쪽의 얇은 `buildSaveApplyInput(...)` wrapper는 제거했고, 호출부에서 `TableEditorSaveApplyInput(...)`를 직접 구성한다.
- Unsaved back-press 판단: `TableEditorExitCoordinator`
- 모드 전환 시 transient/selection 정리: `TableEditorModeTransitionHandlers`
- bottom panel mode change 결과 해석/반영 규칙: `TableEditorBottomPanelModeChangeResolver`
- 파일명/경로 슬롯 list 조작/정리: `TableEditorSlotListHandlers`
- 슬롯 draft → 도메인 draft 반영: `TableEditorSlotDraftHandlers`

## 3) Next steps (recommended order)

1. **LayoutTabActions 정리 (Phase 2 계속)**  
   - 우선 Screen 본문에 직접 펼쳐진 `LayoutTabActions(...)` 초대형 람다 블록을 builder/helper 호출 형태로 접어, 본문에서 wiring 밀도를 낮춘다.
   - 이후 파일명/경로 슬롯 관련 액션을 `TableEditorSlotUiHandlers`(가칭)로 묶어,
     - 입력: 현재 `templateState` + relevant UI state + 액션 타입
     - 출력: next `templateState` + 필요한 transient/selection 변경
   - Screen은 handler 호출 + state apply만 남김.

2. **Counter preview / scope sync coordinator (Phase 3 준비)**  
   - 파일명/경로 scope signature 계산, sync trigger 정책을 `feature/table/editor/coordinator/*`로 이동.
   - Compose 쪽에는 `remember`/`LaunchedEffect` wiring만 유지.

3. **Undo / inline-edit 정책 handler로 이동**  
   - `requestSelectCell`, `requestBottomPanelModeChange`, undo snapshot push/apply 규칙을
     `feature/table/editor/handlers` 내 별도 파일로 이동.
   - Screen은 "언제 handler를 호출하는지"만 책임.

## 4) Regression checklist (quick)

- [ ] Unsaved=false 에서 back → `onBack` 바로 호출, undo stack clear 유지.
- [ ] Unsaved=true 에서 back → Unsaved dialog 노출, Save/Discard/Cancel 동작 이전과 동일.
- [ ] Save 성공 시:
  - [ ] template/style/placement snapshot 초기화
  - [ ] undoManager 초기화 및 revision 증가
  - [ ] "저장됨" toast 노출
  - [ ] `onTemplateChange` 호출 후 exit 플래그에 따라 `onBack` 호출.
- [ ] 파일명/경로 슬롯:
  - [ ] remove/move 시 연속된 슬롯 구조 유지(normalize 정책 유지).
  - [ ] 삭제된 셀 참조가 슬롯에서 제거되는 동작 유지.
  - [ ] 모드/슬롯 전환 시 transient UI state(clearDraft 여부 포함) 동작 유지.



## 5) Inline edit policy (latest)

상세 기준 문서는 루트의 `table_editor_inline_policy.md`다.
이 summary는 Screen 책임과 구조 관점에서 핵심만 요약한다.

- 셀 편집 값의 SSOT는 `TableTemplateState`다.
- `onEditingValueChange` → `TableEditorInlineEditActions.applyInlineValueChange(...)`가 값을 즉시 template에 반영한다.
- `inlineEdit.editingValue`는 저장 전 데이터 본체가 아니라 CELL_EDIT 입력 UI 상태다.
- undo snapshot은 셀 편집 세션당 1회만 생성하며, 세션 시작 기준은 첫 `onValueChange`다.
- 현재 최소 inline 상태는 `inlineEdit`, `inlineEditSessionState`, `editSessionOriginalCellState` 세 가지다.
- `commitIfNeeded(...)`는 값 반영 본체가 아니라 세션 종료 / validation / counter conflict 처리다.
- counter 값 파싱 helper와 counter commit resolver는 분리하고, commit 시점의 검증 / conflict / normalize / latch 계산만 resolver가 담당한다.
- CELL_EDIT 패널 하단 저장/되돌리기 버튼은 제거했고, 하단 액션은 메인 3버튼(저장 / 초기화 / 언두)으로 통일한다.
- 초기화 의미는 화면 진입 시점 전체 복귀 + undo stack clear다.
- inline 결과 해석, counter conflict / committed counter 적용, template apply mode 분기는 feature/editor helper가 담당하고, Screen은 현재 state/context 브리지와 applied state 반영 쪽으로 더 얇게 유지한다.
- inline 입력 조립은 이제 `TableEditorInlineActionCoordinator` 안에서 `TableEditorInlineEditApplyInput(...)`를 직접 구성한다.
- Screen은 별도 `buildInlineEditApplyInput(...)` helper를 유지하지 않고, 현재 context/binding 제공과 applied state 반영에 집중한다.
- inline binding이 묶는 외부 의존성은 다음과 같다.
  - undo 기반 template apply (`applyTemplateWithUndo`)
  - direct template apply (`updateTemplateDraft`)
  - counter conflict UI update
  - committed counter 반영 bridge
