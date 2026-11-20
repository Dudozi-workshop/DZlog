# Android / iOS rotating phrase source audit — 2026-11-17

Scope: current PR #87 branch source. This is not a device or native-test pass.

| Behavior | Source finding |
| --- | --- |
| Quick-edit phrase set selection | Both platforms call common `selectQuickPhraseSet`. Changing the set resets only that cell's index, completed shots and override; selecting the same set preserves progress. |
| Quick-edit eligibility | Both use common quick-edit policies. Counters remain excluded. |
| Individual repeat interval | Shared editor exposes `everyOverride`, falling back to the set's `defaultEvery`. iOS already wires interval changes through `applyMockPhraseEvery`. |
| Individual phrase progress | Shared editor exposes current item and completed shots through `TableEditorPhraseProgressDialog`; iOS wires edits into its document draft. |
| Successful capture advancement | Common `PreviewPipeline` builds the per-cell progress patch using `CellPhraseProgressPolicy.captureUpdates`. iOS resolves captures through `buildCapturePreview`. End-to-end persistence and failed-capture behavior still require accumulated verification. |
| Quick-edit option presentation | Android uses a bounded bottom sheet. This batch moves iOS date/phrase options from the input list into the shared bottom sheet and exposes current selection visually and through semantics. |

## Remaining verification

- Open phrase options, dismiss without selecting, and confirm the draft is unchanged.
- Select the same set and preserve progress; select another set and verify only the chosen cell resets.
- Save/cancel/relaunch and verify the selected set, individual interval and completed shots.
- Successful capture advances progress once; failure/recovery must not advance it twice.
- Check long set lists, small screens, large text and VoiceOver.
- Compare full camera phrase-progress controls and latest approved Android changes separately; this audit does not certify complete UI parity or introduce a new all/individual control design.

Local verification: whitespace checks and iOS release metadata validation. Native compilation, XCTest and physical-device checks were not run in this batch. Do not wait for CI per the user's workflow.

## 2026-11-20 quick editing update

- Camera quick edit and table detail share the compact period sheet, current phrase, completed-shot slider,
  whole-set and individual periods, and explicit resolution of newly out-of-range progress in linked cells.
- Template JSON and backup validation support active and remembered per-item periods. Omitted lists
  retain the legacy fallback. Reordering a phrase moves its individual periods with it.
- The iOS quick editor replays phrase changes in order against its opening document. Cancel discards
  the draft, and the camera workspace verifies the opening snapshot before saving.
- Both iOS text-entry screens use the same persistent 20-item favorites list. A star toggles the full
  input immediately. Single-line chips and a pinned management button expose rename, reorder and deletion.
- Added shared policy and serialization regression cases and an iOS UI case for favorites surviving
  cancellation and relaunch. Native and simulator verification runs on the macOS checkpoint.
