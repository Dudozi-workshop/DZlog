# iOS touch focus, exposure and AE lock batch

Status: implemented; accumulated native compile and UI verification requested.

## Confirmed behavior carried to iOS

- Tapping the camera preview selects a new autofocus and auto-exposure point.
- Every newly selected focus point resets exposure compensation to neutral.
- The compact focus ring exposes an AE lock control and a short, number-free brightness slider.
- The brightness slider is 78pt wide, matching the approved reduced Android control length.
- When AE is locked, the first preview tap only expands brightness controls at the existing focus point; the following tap explicitly moves autofocus while retaining the exposure lock.
- Capturing a photo and 2.8 seconds without interaction compact transient controls. A locked ring remains visible in compact form.
- Leaving the camera restores continuous autofocus, continuous auto exposure and neutral exposure compensation.

## Native implementation

- AVFoundation now receives preview-layer converted focus/exposure points rather than uncorrected view ratios.
- AE lock, exposure target bias and autofocus changes are made under the existing device configuration lock.
- Focus controls live in the same native preview surface as the live table, avoiding Compose/UIKit touch and z-order conflicts.
- Table move/resize handles, the lock button and brightness slider are excluded from preview focus taps.

## Regression coverage

- The live-table simulator fixture supplies deterministic focus/exposure callbacks without pretending to have camera hardware.
- Native UI coverage verifies normal focus, AE lock, compact locked state, first-tap brightness expansion and second-tap locked refocus.
- Existing live-table move, resize, rotation and PhotoKit save routes remain in the same full UI suite.

## Verification boundary

- Simulator tests verify interaction state and accessibility, not sensor focus convergence or image brightness.
- iPhone ARM64 compilation verifies Apple API bindings only after CI completes.
- Actual focus success/failure timing, exposure stability and touch placement still require physical-iPhone QA.
