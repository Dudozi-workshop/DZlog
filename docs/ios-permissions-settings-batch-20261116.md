# iOS permissions settings batch — 2026-11-16

## Scope

- Replace the unavailable iOS `권한 관리` route with live camera and Photos authorization status.
- Explain why each permission is used without triggering a prompt merely by opening settings.
- Add explicit refresh and an AppDelegate-owned route to the DZlog page in iPhone Settings.

## Behavior

- Camera status reports allowed, denied, restricted or not yet requested.
- Photos read/write status additionally distinguishes limited access.
- Permission prompts remain attached to first use of camera or Photos; this screen is read-only.
- Returning from iPhone Settings can be followed by `상태 새로고침` to update both labels.

## Verification

- XCTest verifies both permission rows, the policy explanation, refresh, and the system-settings route button.
- The existing Swift permission gate remains the only code that may request camera access.
- Full native CI is requested after publishing; completion is not awaited locally.
