# iOS gallery filter selection — 2026-11-16

- Reconcile the selected list preview when favorites filtering or favorite membership changes, not only when photos or the folder change.
- Preserve explicit original/result companion switching while the viewer is open. Reapply the active filter on return to the list.
- Clear stale selection and show an empty-favorites message after removing the last favorite; switching back to all photos restores a valid preview.
- Extend the existing favorites/persistence/folder XCTest to cover a non-favorite companion selection before filtering, removal of the final favorite, and return to all photos.
- No PhotoKit write contract, Android source, saved photo, counter or workspace schema changes.

Local whitespace, shell syntax and selected XCTest method checks passed. Native XCTest and compilation require macOS CI and are not claimed as executed locally. Keep PR #87 Draft and unmerged; do not wait for CI completion.
