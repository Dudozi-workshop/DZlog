# iOS recent thumbnail and session Undo batch

Status: implemented; native compile, PhotoKit prompt and UI verification pending.

## Recent capture

- The first camera bottom slot now renders the latest successful capture thumbnail, preferring the processed result and falling back to the original.
- The thumbnail is read through the existing indexed PhotoKit repository; arbitrary Photos assets remain inaccessible to the app-private gallery adapter.
- After Undo, the slot reloads the previous session capture or returns to the gallery icon.

## Session Undo contract

- A successful camera save records an in-memory session entry containing its capture ID plus the exact workspace before and after the commit.
- Undo is enabled only for the latest capture from the current camera session.
- BOTH mode deletes its result and original asset IDs in one PhotoKit change transaction and removes both index entries and favorites in one durable index snapshot.
- Workspace rollback occurs only after PhotoKit confirms deletion. It restores the exact previous counter, table patch and phrase state.
- Saving settings, quick values, table placement, switching template or an external automatic-counter advance clears stale session Undo entries rather than rolling those later edits back.

## Interrupted Undo recovery

- A durable Undo journal is written before requesting PhotoKit deletion.
- PhotoKit cancellation leaves photos and workspace unchanged and clears the journal.
- If assets are deleted but workspace persistence fails, the journal remains and Home recovery finishes the workspace rollback on the next entry.
- Inaccessible assets or unrelated workspace edits fail closed; additional capture/settings changes remain blocked while recovery is unresolved.

## Tests added

- Undo coordinator: confirmed deletion, cancellation, persistence failure/relaunch recovery, inaccessible assets and unrelated workspace edits.
- PhotoKit index: original/result removal together, favorite cleanup, unrelated capture preservation and duplicate Undo rejection.
- Existing camera-permission UI coverage continues to assert Undo is disabled before the first successful session capture.

## Verification boundary

- Local `git diff --check` and shell syntax validation are required before publishing.
- Request Shared Core iOS tests, iPhone ARM64 compile and full iOS UI after publishing; do not wait for their completion.
- Physical-device QA must approve/cancel the Photos deletion prompt and verify the thumbnail, counter and table state after BOTH-mode Undo.
