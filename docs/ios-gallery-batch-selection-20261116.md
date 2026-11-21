# iOS gallery multi-selection and batch actions · 2026-11-16

## Implemented
- Explicit selection mode in grid/list, individual checkboxes, select all visible accessible photos, clear selection, selected count and cancel/back to exit.
- Selection is intersected with the current folder/favorites/availability scope. Hidden or inaccessible photos cannot be silently included in a subsequent batch. Sorting does not replace IDs.
- Move/delete dialogs freeze the selected IDs and show the count; cancelling does not modify Photos or index. Non-selected original/result companions are never added implicitly.
- Batch deletion uses one PhotoKit change transaction and updates the index/favorites only after success. Limited Photos access keeps the existing fail-closed behavior if any target cannot be resolved.
- Batch move validates every ID, destination, pending-recovery state and both internal/destination name conflicts before one logical-index snapshot. It does not rename Photos resources or albums. Same-name original/result pairs cannot be merged into one folder: the UI explains the conflict, and the repository independently rejects it without partial changes.
- Processing disables selection and scope changes. Failure/cancel keeps selection for retry; success reloads the gallery. This implements app functionality only: no actual user assets were moved/deleted during development.

## Tests and limits
- Three iOS index tests added: all-or-none validation and reopen persistence; internal/destination collisions and pending recovery blocking; explicit selected deletion, unselected original preservation, favorites cleanup and idempotent index cleanup.
- Existing gallery XCTest extended with select-all/clear, move/delete dialog cancellation, scope-change selection cleanup and photo-count preservation. It deliberately does not confirm a destructive Photos operation.
- Local: diff check, iOS shell syntax, Node CI-reuse tests (15 passed), and consistency of 29 selected XCTest names.
- New native tests are authored, not executed locally. Linux has no Xcode/Kotlin Native toolchain. Real system delete confirmation/cancellation, file conflicts, large selections and physical-device layout still need native QA.

## CI and scope
Starting head `184a667`: Android/shared-core running; iPhone compile/UI queued. Did not wait or poll. One combined publication; no merge/deploy. Rotating phrases unchanged.

Main remaining implementation gaps: adaptive gallery grid/scroll-anchor behavior, dedicated recent/default-storage sections, viewer paging/chrome visibility/info sheet, and separately finalized rotating-phrase parity. Rendering comparison and full latest CI/physical-device QA remain release gates; batch operations are not proof of migration completion.
