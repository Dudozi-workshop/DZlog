# PR #100 — Android V2 table editor / camera / photo QA

**Status:** Draft PR, not approved for integration. All tests must pass on the PR's current HEAD.
**App package:** standalone debug QA via `-PdzlogStandaloneQa=true -PdzlogQaInstanceTag=tableparity` (`com.dudoziworkshop.dzlog.qa.tableparity`); do **not** overwrite the production DZlog app or its data.

## Automated gates
- [ ] `:app:compileDebugKotlin` and the focused V2/camera/merged grid tests passed at current HEAD.
- [ ] `:app:testDebugUnitTest :app:assembleDebug` passed at current HEAD, against the latest integration branch.
- [ ] The isolated QA APK is present as an artifact of the successful full-regression run.
- [ ] Review 43 deleted V1 files vs retained active V2 and shared slot policies; no unresolved imports/reflection or build failures.
- [ ] The PR branch includes the latest `integration/dzlog-20261010` changes without overwriting them.

## On-device QA
Use a fresh template and a copy of an existing template in the isolated QA app; never change production app data.

| Scenario | Actions | Acceptance |
| --- | --- | --- |
| 2×2 basic table | Set text/numeric cells; open V2 detail then camera | Same text values, bold style, relative font scale, alignment and cell edges |
| Merged root | Merge top row 2 cells, enter a long label; view editor/camera/photo | Label occupies full merged span; no internal split line; other grid lines remain |
| Vertical merge | Merge one column across two rows | No internal horizontal line; remaining cells retain their borders |
| Content-driven layout | Long/multiline text in one cell; compare V2, camera, saved photo | Same relative row/column proportions; no unexpected clipping on merged root |
| Custom row/col sizes | Drag row/column boundaries; save and reopen | Same selected-cell touch area, final ratios and camera/photo output |
| Appearance | Change alpha/background/text scale, alignment, grid on/off | Detail/camera/saved photo agree; changes do not reset template values |
| Undo and redo | Merge/unmerge, resize, edit, undo/redo and save | Correct restoration and persistence after reopening |
| Quick edits | Modify counter/rotation on camera and reopen table | No stale or overwritten values; existing quick-edit functionality preserved |
| Placement/capture | Test normal and rotated placement, 3:4 and 9:16 | Camera and photo have consistent geometry and placement |

## Evidence and release gate
- Capture paired screenshots of V2 details and camera preview, plus one saved watermarked photo for each major case.
- Record any differing values, bounding boxes, text wraps, or visual artifacts, with template and device info.
- Do not merge PR #100 until full CI passes and a tester signs off the Android device QA.
- **iOS migration PR #87 and production Android app are out of scope.**
