# iOS gallery management batch

Status: implemented and published; combined native verification requested and not awaited.

## Batch 1 — individual photo deletion and movement

- Expanded viewer offers deletion of the selected photo only. Its original/result companion remains independently indexed.
- The app asks for confirmation, then uses the existing PhotoKit deletion transaction. A cancelled or unsuccessful PhotoKit request retains the index entry. Successful deletion removes the index entry and cached thumbnail and returns to the list.
- The move picker lists existing logical folders, their ancestors and the DZlog root. Moving changes the durable DZlog index only, not a Photos album or resource filename.
- Moving rejects traversal, out-of-root paths, missing entries and destination filename conflicts.
- Gallery mutations serialize with repository capture writes and reconcile pending PhotoKit saves before proceeding. Direct index mutations reject an outstanding capture journal.
- Added native index tests for relaunch persistence, companion preservation, conflict/path validation and pending-journal protection. These tests have not been executed in this batch.

## Validation queue

- Local `git diff --check` passed.
- Keep the existing 30-test success checkpoint at `6113465a` separate from this unverified batch.
- Accumulate folder management and share/favorite work before requesting a combined native validation run.
- Native integration must exercise PhotoKit delete approval/cancellation, selected-role deletion and companion survival, move/relaunch restoration, same-name rejection and limited permission failures. Use synthetic test assets only.
- No native build or CI run requested for this local batch. Do not mark it verified.

## Batch 2 — logical folder management

- Added create and rename dialogs to folder browsing. Rename updates descendant folders and photo paths together.
- Delete is restricted to empty non-root folders. Photo/subfolder contents block deletion; no PhotoKit assets are deleted by folder operations.
- Empty folders persist after relaunch and appear in the photo move picker.
- Gallery index v2 stores photo records and explicit folders in one snapshot; legacy v1 records are read until the first mutation migrates them. The capture pending journal keeps its existing format.
- Added native tests for folder relaunch, subtree rename, protected root, duplicate/invalid names, nonempty deletion and legacy migration. Not executed yet.
- Local whitespace validation passed. Native compilation and combined integration remain pending.

## Next batch

## Batch 3 — sharing and favorites

- The expanded viewer can open the native iOS share sheet for the selected readable image.
- Favorites persist by PhotoKit asset ID, survive moves and folder renames, and are removed when that asset is deleted.
- Added favorite toggling, star markers and an all/favorites list filter.
- The native host validates the shared bytes as an image before presenting `UIActivityViewController`, including an iPad popover anchor.
- Expanded index tests cover favorite relaunch, move preservation, removal and pending-journal protection.

## Accumulated validation checkpoint

- Gallery batches 1–3 are ready for one combined native compile/test request.
- The request should include shared iOS index tests, iPhone ARM64 compilation and the existing complete UI regression. Share destinations still require physical-device QA.
- Start CI after publishing these commits and continue with the next migration work without waiting for completion.
- CI was requested at `96f58451`; the four relevant workflows were observed in progress, then migration work continued without polling for completion.
