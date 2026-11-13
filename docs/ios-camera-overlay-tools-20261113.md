# iOS camera overlay tools batch

Status: implemented; native CI requested with the accumulated M3-A changes.

## Implemented

- Fixed the latest native compile blocker by using the explicit Skia image decoder for the recent-session thumbnail.
- Added the remaining Android camera-tool toggles to the iOS camera panel:
  - thirds grid show/hide;
  - live table preview show/hide without changing the saved result;
  - movable floating assist shutter.
- Kept the grid and table in the native preview surface so they stay aligned above the AVFoundation preview.
- The floating shutter uses normalized coordinates, remains partially visible at the edges and saves its position after a drag.
- Grid, table visibility, assist-shutter enablement and assist-shutter position persist in the versioned editor workspace with backward-compatible defaults.
- Camera preference writes invalidate session Undo snapshots so deleting a photo cannot unexpectedly roll a later preference change back.

## Regression coverage

- Extended the shared workspace codec test with camera preferences and legacy-default decoding.
- Added a native UI route that toggles all three tools, verifies the floating shutter is disabled when capture is unavailable and verifies the settings after leaving and reopening the camera.
- Existing camera permission-denial, PhotoKit, gallery, quick-input and live-table tests remain unchanged.

## Verification boundary

- `git diff --check` passes.
- Local Gradle execution remains unavailable because the managed environment cannot download the Gradle distribution.
- Native iPhone ARM64 compilation and simulator UI execution are delegated to GitHub Actions.
- A successful compile/UI result is not physical-device camera, touch, image-quality or signed-installation evidence.
