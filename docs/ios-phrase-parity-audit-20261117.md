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
