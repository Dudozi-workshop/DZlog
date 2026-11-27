# iOS capture aspect batch — 2026-11-16

## Scope

- Add persisted 3:4, 9:16 and 1:1 capture ratios to the iOS capture settings.
- Use the same ratio for the live AVFoundation preview, table overlay viewport and saved photo pixels.
- Apply a centered, no-upscale crop before ORIGINAL, RESULT or BOTH payloads are planned and written to PhotoKit.

## Compatibility

- The existing Android `CaptureAspect` contract now lives in shared common code without changing its values or default.
- Existing iOS workspaces without `captureAspectMode` decode to 3:4.
- A source already matching the selected ratio keeps its original encoded bytes. A required crop is normalized upright and encoded once at high JPEG quality before saving or table composition.

## Verification

- Common tests cover ratio mapping, safe legacy fallback and exact crop dimensions.
- The configured capture fixture checks saved dimensions against the selected ratio for all save modes.
- The native raster check exercises all three center crops, and XCTest verifies 1:1 survives camera return and app relaunch.
- Local Gradle execution remains unavailable because the wrapper distribution cannot be downloaded in this environment. Native and Android CI are the compile/test authority for this batch.
