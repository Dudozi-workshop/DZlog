# Counter Refactor Summary (Step 7)

## 1) Current SSOT

Counter behavior is now centered on the following shared layer:

- `feature/counter/CounterRequestResolver`
  - Normalizes screen input into final counter request input.
  - Owns saveMode axis mapping contract.
- `feature/counter/CounterFacade`
  - Shared entry for read/write (`read`, `setManualNext`, `clearManualNext`, etc.).
  - UI does not call domain counter store/policy directly.
- `feature/counter/CounterSyncDecider`
  - Pure sync decision function for apply/guard behavior.

## 2) Layer responsibilities

- `feature/counter/*`
  - Shared counter contract + normalization + engine adapter + sync decision.
- `ui/camera/counter/*`
  - Camera-specific sync reason/event helpers (`detect -> read -> decide/apply` support).
- `ui/table/counter/*`
  - Table-specific UI candidate/manual handling and scope sync helpers.
- `ui/home/*`
  - Read-only consumer path for preview counter display.

## 3) saveMode contract

- `BOTH` and `WATERMARK_ONLY` are normalized to the same watermark axis.
- `ORIGINAL_ONLY` is normalized to a separate original axis.
- Stream separation is based on normalized request input (path/prefix/scanPrefix + effective mode), not on ad-hoc saveMode string concatenation.

## 4) Table manual input policy

- Table manual counter edit is **UI candidate by default**.
- It does not contaminate shared SSOT unless explicitly persisted.
- Auto reset returns to facade readback (media-based) value.
- Initial `1` flicker is prevented by sync gate before first valid readback.

## 5) Camera sync policy

Camera counter sync is handled with:

1. detect reason
2. read resolved next (`CounterFacade.read`)
3. decide/apply (`CounterSyncDecider`)

Core reasons used in camera flow:

- `CAPTURE_COMMITTED`: same-stream forward event.
- `UNDO_COMMITTED`: downward sync allowed.
- `SAVE_MODE_CHANGE` / stream-key change: stream switch handling.
- `RESUME`: same-stream downward guard retained.

## 6) Regression checklist (quick)

- [ ] Home/Table/Camera show consistent counter progression for same stream.
- [ ] `BOTH` and `WATERMARK_ONLY` stay on same counter axis.
- [ ] `ORIGINAL_ONLY` stays on separate axis.
- [ ] Camera capture success auto-advances next value.
- [ ] Camera undo success auto-restores next from media readback.
- [ ] Table manual input stays UI candidate only by default.
- [ ] Table auto reset returns to media/facade readback value.
- [ ] Table initial entry/resume has no temporary `..._1` flicker before first sync.
