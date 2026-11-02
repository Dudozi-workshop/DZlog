# DZlog iOS M3-A — Android UI/UX parity audit (2026-11-02)

## Scope / evidence

- Source branch: `feat/ios-m3a-capture-media-contract-20261021`, Draft PR #87. Android reference: `integration/dzlog-20261010`.
- **Code-level comparison only.** This report does not certify screenshot/pixel parity or real-device UX.
- Audit changes **no app behavior**. Do not merge PR #87 or enable RESULT/BOTH solely because a compile or simulator test passes.

## Current route map

| Area | iOS actual route / component | Android reference | Finding |
| --- | --- | --- | --- |
| Home | `MainViewController` -> `HomeNavigationPoc` -> shared `HomeContent` | `AppNavigation` / `HomeScreen` | Shared presentation, platform-specific bindings still incomplete |
| Table editing | shared `TableEditorV2Content` | `TableEditorV2Screen` | Shared content, behavioral and visual parity not proven |
| Templates | shared `TableTemplateListContent` | `TableTemplateListScreen` | Shared content, flow/accessibility QA still necessary |
| Settings | shared `SettingsRootContent` | `SettingsScreen` | Several actions explicitly mapped to `unavailable()` |
| Camera | `CameraViewController` -> `IosCameraCapturePoc` | `CameraScreen`, `CameraTopBar`, `CameraPreviewArea`, `CameraBottomControls` | **P0: testing UI, not Android-equivalent camera UI** |
| Gallery | `IosPhotoKitGalleryScreen` | `LogFolderScreen`, `LogGridScreen`, `LogViewerScreen` | **P1: read-only flat list, no folder/grid/management parity** |

## Confirmed gaps and safeguards

### P0 — Production camera entry and UI

1. The camera is a minimal AVFoundation preview with status labels and `테스트 사진 촬영` button; Android's camera top/bottom controls, table drag/resize, quick-value editor, settings overlay, zoom/focus/flash/undo flows are not present in the iOS composable.
2. `CameraViewController` does **not** supply `tablePlacement`; its default is `null`. The newly added compositing adapter is therefore not invoked by the normal camera route.
3. `IosCameraWorkspaceBridge.prepare` only accepts `SaveMode.ORIGINAL_ONLY`. New `EditorDocument` defaults to `SaveMode.BOTH` and a null nextCounter; the initial workspace can have no active template. Consequently new installs need state configuration to reach a successful camera save; the normal default capture flow is not production-ready.
4. Do not remove the ORIGINAL_ONLY gate until persisted placement, preview rendering, and save/rollback/recovery can be proven together.

### P0 — Table result rendering parity

1. iOS uses a separate Skia renderer, not Android's Canvas `WatermarkDraw`; currently uses `Font(null, size)` and simpler text layout.
2. Merged cells are explicitly rejected (`rowSpan != 1 || colSpan != 1`). Android supports richer cell rendering.
3. Background/text/grid size and alignment, weighted cells, crop/orientation, and rotation require image-based Android/iOS comparison using identical fixtures.
4. Current session saves from one resolved preview, which protects timestamp/counter consistency; this does not prove that the resulting pixels match Android.
5. CUSTOM 90-degree geometry was recently patched; verify all corner, 0/50/100%, tall/narrow photo cases with tests.

### P1 — Gallery, settings and home surface

1. iOS gallery is read-only: linear list, one 190dp preview, and refresh/back. Android uses folder navigation, grid and dedicated viewer with photo actions. Gallery parity is **not** achieved.
2. iOS settings currently routes permissions, capture-feedback, credits, guide, export and import to `unavailable()`. These are not fully implemented.
3. iOS home saved-photo info still displays `사진 저장 연결 전` / PoC placeholder paths, and the recent-photo time text is a static label. Replace with real resolved data.
4. Default launch seeds from `previewFixture()`; first-launch/onboarding must be audited separately before claiming production-equivalent flows.

### Test reliability / CI

- At audit time the previous commit `ccfa3da9` passed Android integration, iOS Core and iPhone ARM64 compile; iOS simulator UI failed.
- The failing assertion was `PreviewInteractionTests.swift:59`, which expected the **exact** album text `사진 3개`. The new BOTH PhotoKit test adds two assets, making an absolute global count brittle. Commit `a5dc68ad` changes this to a >=3 count assertion; **its passing status must be rechecked**.
- Xcode simulator and CI can validate behavior but cannot alone certify native camera geometry, image fidelity or visual parity on an iPhone.

## Next implementation sequence

1. **Visual baseline**: gather Android reference screenshots and iOS simulator captures for home, editor, template list, settings, camera, gallery; create a per-control map. Do not use mock drawings as evidence.
2. **Camera UI composition**: implement matching layout hierarchy (top bar, preview with interactive table, bottom capture controls, quick edit, settings) in small slices while preserving existing AVFoundation/PhotoKit adapters.
3. **Saved placement contract**: connect actual persisted user configuration to `IosCaptureTablePlacement`; avoid hardcoded defaults that could silently alter user results.
4. **Render parity**: matched Android/iOS image fixtures for cells, fonts, background, weights, merged cells, positions, 0/90-degree rotation and camera image orientation. Keep unsupported cases blocked.
5. **Capture enablement / gallery parity**: separately exercise ORIGINAL_ONLY, WATERMARK_ONLY, BOTH; verify PhotoKit two-role receipt, counter update, restart recovery and gallery flows; then perform actual iPhone QA.

## Release / merge gate

- [ ] Documented UI screen/control parity and before/after screenshots
- [ ] No development-only labels/buttons on user camera screen
- [ ] Real saved placement values reflected in camera preview and final photo
- [ ] Merged-cell/style/orientation parity (or explicit UI restriction)
- [ ] Three save modes and interrupted PhotoKit recovery tested
- [ ] Gallery navigation/actions audited against current Android
- [ ] Latest iOS ARM64 + scoped UI + shared tests all pass
- [ ] Actual iPhone visual and interaction QA completed
- [ ] User approval before merging Draft PR #87

**No current user approval is needed to continue the audit.** No release or merge authorization is implied.
