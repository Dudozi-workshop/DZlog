# iOS gallery ordering and folder discovery · 2026-11-16

## Implemented in this batch
- Photo sort: newest, oldest and name, matching the existing Android choices in `LogFolderScreen.kt` / `GalleryPhotoSort`. Sort a copy, keep selection by Photos asset ID, and use that same order in previous/next navigation.
- Timestamp ties are deterministic. Photos IDs are opaque strings, not Android MediaStore sequence numbers; unknown Photos timestamps sort last in both date directions. Name comparison uses invariant lowercase.
- Child folders: all / with photos / empty and name / recent-photo ordering, matching Android `GalleryFolderFilters.kt`. Counts and latest date include descendants and remain independent of the favorites filter. A sibling named `ab` never contributes to `a/`.
- Folder rows show indexed photo counts including descendants. Missing/inaccessible indexed entries remain records, not proof of an accessible photo; these controls do not delete or rewrite storage. Empty-photo filtering is not permission to delete a folder with children.
- Preview and child-folder list heights are bounded to leave room for the new controls on portrait iPhones; final visual parity is not claimed.

## Coverage
- Four new common Kotlin test cases cover date/null/tie ordering, case-invariant names, immutable input/asset identity, subtree boundaries, empty folders and recent ordering.
- Existing gallery XCTest cases now exercise sort menus, selection preservation, empty-folder filtering and folder recent sort. Existing media scope retained.
- Local checks passed: `git diff --check`, iOS shell syntax, 15 CI-reuse Node tests, all 29 selected XCTest names exist.
- Kotlin tests and XCTest were authored but not run locally: this Linux workspace has no Kotlin compiler/Gradle installation or Xcode. No claim of native compile or runtime success.

## CI snapshot before work
Head `5d0199f`: shared core and Android succeeded; iPhone compile and UI queued. No wait, polling or manual rerun. Publish this combined batch once; no merge/deployment.

## Current migration scope, replacing the stale Nov 2 inventory
### Implemented, not necessarily release-verified
- Production startup/default templates/first selection; shared table editor and template management.
- Three capture save modes, persisted placement/compositing, focus/exposure lock, zoom/flash, quick inputs, assist shutter and latest-capture undo.
- Gallery folder/grid/favorites, original/result switching, single-photo sharing/move/delete, image zoom/retry and now photo/folder ordering.
- Permissions, capture feedback, help, credits, backup export and explicit preview/merge import are wired to real screens, not unavailable placeholders.

### Major implementation gaps still visible against Android
- Gallery selection mode, select-all and batch move/delete. iOS currently offers one-photo actions and only empty-folder deletion.
- Adaptive grid columns/pinch density and scroll-anchor retention; dedicated recent/default-storage gallery sections. iOS still has a fixed 3-column grid.
- Viewer swipe paging, hideable chrome, and photo metadata/info sheet. iOS still uses previous/next controls and persistent chrome.
- Latest rotating-phrase whole/individual behavior is deliberately excluded until Android's independent work is verified final.

### Verification gates, not missing-feature counts
- Android/iOS rendered-image comparisons for Korean fonts, line layout, cell styles/merges, orientation and placement. iOS still uses its separate Skia renderer.
- Latest iPhone compile plus complete relevant UI tests; physical sensor/limited Photos permissions/cancel flows, BOTH save/undo/interruption recovery, actual backup/share sheet, narrow-screen and gesture QA.
- Real-device evidence and explicit approval before promoting or merging Draft PR #87. A numeric completion percentage would misrepresent these gates.
