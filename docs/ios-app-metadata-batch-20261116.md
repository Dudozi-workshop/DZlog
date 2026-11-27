# iOS app metadata batch — 2026-11-16

## Scope

- Replace the settings footer placeholder `iOS 개발 빌드` with the installed bundle's real short version and build number.
- Reuse the same `버전 x.y.z (build)` label on the credits screen so both information surfaces remain consistent.
- Keep version ownership in `Info.plist`; shared UI only receives the resolved display label.

## Verification

- The existing settings/help/credits XCTest verifies `버전 0.0.1 (1)` in both settings and credits for the current iOS test bundle.
- PR #87 remains Draft and unmerged.
