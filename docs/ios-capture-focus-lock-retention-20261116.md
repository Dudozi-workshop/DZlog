# iOS capture lock retention — 2026-11-16

## Defect and change

`focusActive` becomes false while capturing or editing the live table. The native preview previously cleared its AE-lock flag, position and controls during that temporary suspension even though AVFoundation's exposure mode remained locked.

- Temporarily hide and disable focus controls while preserving a locked position, AE-lock state and brightness.
- Restore the compact locked control when input resumes. A first tap expands brightness; subsequent refocus still preserves brightness.
- Keep the existing unlocked-control clearing behavior. Camera disposal and its existing native reset path are unchanged.
- No Android, file format, counter or photo storage behavior changes.

## Coverage and limits

Extend the native focus XCTest to suspend/re-enable controls, perform a real simulator PhotoKit save, and check retained lock, callback brightness and slider brightness afterward. The test remains in media and full UI suites.

Local whitespace, shell syntax, test-selector checks and 15 CI reuse-policy tests passed. Linux has no Apple SDK; native compile/XCTest and physical-camera exposure quality are not locally verified. Keep PR #87 Draft and unmerged, and do not wait for CI completion.
