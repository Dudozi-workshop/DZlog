# iOS global capture feedback batch — 2026-11-16

## Scope

- Replace the unavailable iOS global `촬영 효과음 및 진동` route with a real screen.
- Share the persisted `captureHapticEnabled` value with camera capture settings.
- Allow haptic enable/disable and a preview without opening the camera.
- State the iPhone shutter-sound policy explicitly instead of presenting a false mute or volume control.

## Behavior

- Enabling haptics previews the medium impact only after the workspace write succeeds.
- A failed workspace write leaves the previous value visible and shows an error.
- Mandatory/system shutter sound remains controlled by iPhone settings and regional policy.

## Verification

- XCTest disables haptics from global settings and verifies the value after app relaunch.
- The existing configured-camera test verifies the same value through camera return and relaunch.
- Native CI is requested through the accumulated full iOS suite; execution is not awaited locally.
