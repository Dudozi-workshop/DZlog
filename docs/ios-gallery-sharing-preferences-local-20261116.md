# Local continuation: gallery preferences and lossless sharing · 2026-11-16

## Prepared without waiting on CI
- Persist the gallery's 2–5 column density in a dedicated versioned NSUserDefaults key, matching Android's persistent gallery-grid setting. Do not write workspace, capture settings or Photos index. Missing/corrupt values safely use default 3; out-of-range values normalize.
- Share original resource bytes and indexed filename via a unique temporary file URL instead of UIImage re-encoding. Reject unsafe filenames with a safe fallback. This preserves the bytes supplied by the Photos repository; it does not invent or rewrite EXIF.
- Swift reports completed/cancelled/failure. Kotlin remains busy while the share sheet is active and reports the actual outcome rather than claiming it opened on a silent failure. Queued duplicate taps are guarded.
- Clean up only the current share operation's temporary folder after completion/error. Force-killed-app leftovers remain in OS temporary storage and rely on OS cleanup; no broad deletion sweep added.

## Validation limits
Two native preference tests added for reopen persistence, normalization and unrelated-key preservation. Existing gallery XCTest no longer assumes a fresh density preference and now checks it after relaunch. Native tests and Swift/UIKit behavior are not executed locally; share-sheet cancel, extension handoff, original byte/filename delivery and temporary-file lifecycle need native QA. No actual photo sharing was performed.

Local checks: diff, shell syntax, 15 CI-reuse Node tests and 29 XCTest selector names. These are not a substitute for native compilation or UI tests.

## Publication boundary
This work is combined locally with recent/default entries, cancellation safety and bounded headers. Remote remains `4e0f86b`. At the one end-of-batch snapshot: Android succeeded, iPhone compile had finally started, shared core/UI still queued. Do not cancel the newly started native compile just to publish this increment. No CI waiting/polling/manual rerun, merge or deployment.
