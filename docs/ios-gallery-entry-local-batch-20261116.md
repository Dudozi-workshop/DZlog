# Local-only follow-up: recent/default gallery entries · 2026-11-16

## Status
Prepared locally after published head `4e0f86b`. Deliberately NOT published: that head's Android workflow was running and shared core/iPhone/UI were queued at the initial check. No failed result was observed. Keep the remote head unchanged so those jobs can run; do not interpret local checks as native CI completion.

## Prepared implementation
- Dedicated recent-capture entry using all indexed Photos assets newest-first, including original-only captures and both roles. Return goes back to the gallery rather than exiting it.
- Dedicated virtual default-storage entry contains only direct `Pictures/DZlog/` photos. The direct root `original/` collection is a separate explicit entry; custom-folder descendants are excluded. These are views, not moves/renames.
- Common classification policy mirrors Android `GalleryDefaultStoragePolicy` and recent ordering. Its recent-preview helper limits to nine; the iOS entry in this batch opens the full recent collection, not a new nine-thumbnail home strip.
- Successful moves leave virtual entry mode before showing the destination, preventing a stale scope from hiding moved photos.
- Static review found a same-asset loading race: PhotoKit's non-cancellable callback bridge could resume an old cancelled load and overwrite a newer state. Added an explicit coroutine cancellation check after read and before decode/state publication.

## Verification
Five common policy tests and XCTest entry/count/return assertions added. Local diff/shell checks, 15 Node CI-reuse tests and 29 XCTest selector-name checks pass. Kotlin/Native/XCTest not executed here. No Photos data mutation, merge, deploy, remote commit or PR-body update for this local batch.

## Follow-up: bounded controls on small screens
The fixed-header risk is now addressed locally: gallery controls sit in an independently scrollable column capped at 75% of available height minus the inter-region gap. The photo grid/list retains at least the remaining quarter. Folder lists keep their own 100dp bound, so nested scrolling has a finite size. Header scroll state is kept while visiting the viewer and reset for a new folder/favorites/virtual scope. This is a source-level fix, not a claim of visual or accessibility approval; native small-screen checks remain.

At the next single CI snapshot, published head `4e0f86b` had Android success, while all three iOS workflows were still queued. No failure to fix was reported. Keep this combined local work unposted until those native jobs have an opportunity to run; do not cancel them merely to publish another small change.
