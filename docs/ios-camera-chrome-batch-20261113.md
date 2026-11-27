# iOS camera chrome parity batch

Status: implemented; native compile and UI verification pending.

## Implemented

- Replaced the vertically stacked PoC controls with DZlog camera chrome based on the current Android hierarchy.
- The top row now keeps Back, camera identity, the next filename card and Capture Settings in one compact bar.
- The next filename card removes the extension and highlights the final automatic counter in green.
- The bottom row uses five stable slots: Gallery, Capture Tools, shutter, Quick Input and Undo.
- Undo is intentionally visible but disabled because iOS session deletion/rollback is not connected yet. It does not claim working parity.
- Autofocus, flash and zoom moved into one dark camera-tools panel. The compact zoom chip remains over the preview when the panel is closed.
- Existing AVFoundation calls, PhotoKit transaction handling, live table placement, quick input and settings navigation are reused without changing their persistence contracts.
- Opening camera tools temporarily disables table manipulation, preventing overlapping gestures.

## Regression coverage

- Updated the denied-camera-permission UI route to open Capture Tools before checking disabled autofocus and flash controls.
- The same route now asserts that Capture Tools remains available, shutter remains disabled and unimplemented Undo is disabled.
- Existing accessibility labels remain stable for Camera, Back, Capture Settings, Gallery, Quick Input and shutter.

## Verification boundary

- `git diff --check` and the iOS test script shell syntax check passed.
- The previous automatic-counter batch already has successful Shared Core iOS and Unified Mainline results; its iPhone compile and full UI runs were still in progress when this batch continued.
- Request iPhone ARM64 compile and the complete iOS UI suite after publishing this batch. Do not wait for completion.

## Remaining camera parity

- Connect recent-photo thumbnail content and session Undo/delete recovery.
- Add iOS grid/table visibility and assist-shutter controls only after their persistence and AVFoundation behavior are defined.
- Compare the camera chrome and live table geometry on an actual iPhone; simulator layout is not device certification.
