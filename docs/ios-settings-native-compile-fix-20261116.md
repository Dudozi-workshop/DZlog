# iOS settings native compile fix — 2026-11-16

- Commit `030c13de` passed Unified Mainline and Shared Core iOS checks. Shared UI and iPhone device compilation failed before XCTest execution.
- Remove explicit `layout.weight` imports from help and permissions screens. Their Row/Column scopes already provide the public modifier; the explicit import resolved to an internal implementation property.
- Read notification payloads through nullable receivers in permissions and backup observers, matching the Kotlin/Native Foundation callback signature.
- Request full UI regression for the accumulated settings, metadata and startup changes. Completion is not awaited; no new passing native result is claimed here.
- PR #87 stays Draft and unmerged.
