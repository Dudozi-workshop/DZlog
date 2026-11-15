# iOS pinch zoom and flash persistence batch

Status: implemented; accumulated native compile and UI verification pending publish.

## Implemented

- Added two-finger pinch zoom directly to the native AVFoundation preview surface.
- Pinch begins from the current hardware zoom, clamps to the active camera maximum and commits only when the gesture ends.
- The existing tool-panel zoom slider now commits its final value separately from continuous hardware updates.
- Camera zoom is stored in tenths and restored after leaving and reopening the camera.
- Flash mode (`off`, `auto`, `on`) is stored with the iOS workspace and restored on camera entry.
- Older workspaces default to 1.0x and flash off without changing their schema version.
- Zoom/flash preference changes invalidate stale session Undo snapshots, avoiding an unrelated preference rollback when deleting a photo.

## Regression coverage

- Expanded shared workspace codec coverage for zoom and flash persistence plus legacy defaults.
- The camera-free native preview fixture now accepts pinch gestures and exposes the committed scale.
- Added a native UI test for a 2.0x pinch and final-value commit.
- Existing touch-focus, live-table, gallery, PhotoKit and capture tests remain in the accumulated full suite.

## Verification boundary

- Simulator pinch verifies gesture routing and state commit, not optical image quality.
- Real hardware must confirm maximum zoom reporting, smoothness, front/back camera behavior and flash availability.
- Native CI is requested after publishing; completion is not awaited before continuing.
