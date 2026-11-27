# iOS production startup batch — 2026-11-16

## Scope

- Stop using the synthetic blueberry `현장 기록` fixture for the normal iOS app launch.
- Seed the same usable `기본 촬영` starter definition as Android only when no existing v2 workspace or recoverable v1 editor document exists.
- Keep every existing UI-test fixture route unchanged and add an isolated production-startup test key.
- Move the platform-neutral starter catalog into shared common code so Android and iOS cannot drift into different starter layouts or save rules.

## Data safety

- `IosWorkspaceStore` still loads the v2 workspace first and the v1 editor document second. The new seed is only the final first-launch fallback.
- Existing users who previously saved the sample template keep their saved data; this batch does not rename, replace or delete it.

## Verification

- A dedicated XCTest launches the production starter route and verifies that `기본 촬영` is present while `현장 기록` and `블루베리 관찰 기록` are absent.
- The test is included in the bounded `navigation` validation scope and in full UI regression.
- PR #87 remains Draft and unmerged.
