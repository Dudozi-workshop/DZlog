# iOS locked-refocus parity — 2026-11-16

## Scope

- Carry the approved November 15 camera behavior to iOS: moving focus while AE is locked preserves the chosen exposure compensation.
- Preserve the existing two-tap interaction. The first locked tap opens brightness controls; the second moves autofocus without resetting native exposure bias or the slider.
- Unlocked refocus continues to reset compensation to neutral. Timer duration and other camera behavior are unchanged in this batch.
- Android, shared persistence formats, existing photos and counters are untouched.

## Regression coverage

- The native preview fixture now exposes the bias received by its camera callback, not only the slider state.
- The existing XCTest sets nonzero bias, compacts/reopens controls, refocuses twice while locked, checks both callback bias and slider retention, then unlocks and checks neutral compensation on refocus.
- Include the focus/exposure and existing pinch-zoom tests in the bounded media test scope. Previously those tests ran only in full UI regression.

## Verification boundary

- Local whitespace, shell syntax, Python syntax and CI reuse-policy tests are checked.
- This Linux environment has no Xcode or Apple SDK. Native compilation and XCTest execution remain pending; sensor focus/exposure quality still requires physical-iPhone QA.
- The baseline `6c526ef0` has successful shared-core, Android and iPhone compile workflows. Its UI workflow reports failure with a cancelled simulator job; the log endpoint returned BlobNotFound, so no XCTest failure is asserted from that run.
- Keep PR #87 Draft and unmerged. Do not wait for CI after submitting the accumulated batch.
