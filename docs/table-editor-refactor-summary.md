# Table Editor Refactor Summary (Step 1–2)

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
- Unsaved back-press 판단: `TableEditorExitCoordinator`
- 모드 전환 시 transient/selection 정리: `TableEditorModeTransitionHandlers`
- 파일명/경로 슬롯 list 조작/정리: `TableEditorSlotListHandlers`
- 슬롯 draft → 도메인 draft 반영: `TableEditorSlotDraftHandlers`

## 3) Next steps (recommended order)

1. **LayoutTabActions 정리 (Phase 2 계속)**  
   - 파일명/경로 슬롯 관련 액션을 `TableEditorSlotUiHandlers`(가칭)로 묶어,
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

