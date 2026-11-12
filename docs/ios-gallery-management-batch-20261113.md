# iOS gallery management batch

Status: implemented locally; native compile and integration verification pending.

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

## Next batch

Persist empty logical folders, add folder creation/rename/delete, and reuse this destination picker. Folder deletion must define its effect on contained photo assets explicitly.
