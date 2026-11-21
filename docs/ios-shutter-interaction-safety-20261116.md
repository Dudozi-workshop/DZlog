# iOS shutter interaction safety — 2026-11-16

## Scope

- Recheck live busy state inside the shared main/assist shutter callback. Two queued taps before UI recomposition no longer submit a second capture whose failure could reset the first capture's busy indicator.
- Clear the native in-flight callback when a synchronous photo submission throws, allowing a later retry instead of leaving the camera permanently busy.
- Treat post-submission haptic/UI feedback as best-effort so a feedback error cannot report the submitted photo as a failed capture.
- Disable table/focus/pinch manipulation behind result review and disable native preview gestures while the camera-tools panel is open.
- Hide the floating assist shutter during saving/result review, preventing position persistence changes during an in-flight workspace save.

No persistence schema, Android behavior, sequential capture, exposure settings or photo transaction rules are changed.

## Validation

Local whitespace/shell syntax and 15 CI reuse-policy tests pass. The existing native media tests remain scheduled through PR #87. This environment has no Apple SDK or actual sensor, so native compilation, rapid physical shutter/assist tap behavior, submission-error recovery and preview interaction on iPhone are explicitly still unverified. Do not treat source inspection as a runtime test.

Device checks: rapid main/assist taps must yield one in-flight capture without a false idle state; a failed submission must allow retry; review and tool panels must not pass gestures to the preview; closing them must restore controls and a previously locked exposure.

PR #87 remains Draft and unmerged. CI completion is not awaited.
