# iOS gallery density, scroll retention and photo information · 2026-11-16

## Implementation
- Grid density now supports 2–5 columns (default 3), selectable from the gallery header and with a two-finger pinch. Policy and gesture handling mirror the existing Android `GalleryGridColumnsPolicy`/`GalleryGridPinchModifier`: cumulative 18% steps, 8% jitter threshold, single-finger scroll/taps left alone. Selection/processing disables pinch.
- Grid/list scroll states live outside the viewer branch, so opening a photo and returning does not recreate them. Density changes and grid/list switches carry the first visible asset ID and pixel offset to the new layout. Folder/favorites scope changes create fresh scroll states, avoiding offsets from unrelated photos. This is first-visible-item anchoring, not a claim of exact pinch-centroid anchoring or persistent preferences across app relaunch.
- Read-only viewer information shows indexed filename/logical folder/role, Photos creation time in the device's local timezone, and decoded display-image dimensions. Unknown values say unavailable. No invented EXIF/location/filesize and no Photos modifications.
- Kept density in the existing header to avoid another full controls row on narrow screens.

## Verification
- Seven common policy tests added covering normal/boundary/invalid scales; source comparison confirmed policy and gesture body match Android except names/imports. Android source unchanged.
- Existing XCTest expanded for 3→5→2 column menu changes, retention through viewer/selection/list switches, and photo information. Tests do not prove pinch or nonzero scroll-anchor behavior.
- Local checks: whitespace diff, all iOS shell script syntax, 15 CI-reuse Node tests, 29 XCTest selection names resolve.
- Kotlin/Native compile, new common tests and XCTest could not run locally. Long-gallery anchor behavior, live pinch arbitration, date localization and small-screen layout still require native execution.

## CI / remaining
At start, previous head `e340e76` had all four workflows running. No failures yet, no waiting/polling/manual reruns. Publish this batch once, no merge/deploy.
Still pending: viewer swipe paging/hideable chrome, dedicated recent/default-storage entry parity, final rotating-phrase parity, matched Android/iOS rendering evidence and complete native/physical-device QA. Exact gesture parity and durable gallery preferences are not certified by this batch.
