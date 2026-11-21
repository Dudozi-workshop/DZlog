# iOS workspace backup batch — 2026-11-16

## Scope

- Connect both previously unavailable iOS settings routes: workspace export and import.
- Reuse the shared `.dzlog` codec, SHA-256 validation, size limit and collision-safe merge policy used by Android.
- Export through the iOS document picker and import only after validation and an explicit merge preview.

## Data contract

- Exported: table templates, cells, styles, rotating text, filename and folder rules.
- Excluded: photos, albums, current automatic counters, per-template capture placement/save mode/scope metadata and shared camera settings.
- Import adds new template IDs and collision-safe names. Existing templates, active selection and settings are never replaced.
- The persisted workspace is compared with the screen snapshot before the confirmed write; stale imports fail closed.

## Verification

- XCTest uses the same native notification bridge without opening the system picker. It exports the real fixture archive, selects that archive for import, verifies the 1 existing / 1 incoming / 1 renamed preview, confirms the merge and checks both catalog entries.
- Production uses `UIDocumentPickerViewController`; the test-only route cannot grant access or bypass validation.
- Request the full iOS UI, shared iOS and iPhone compile workflows after publishing without waiting for completion.
- PR #87 remains Draft and unmerged.
