# Combined gallery continuation and selected-file sharing · 2026-11-16

## New work in this slice
Android `GalleryShare.kt` supports one or multiple selected URI items. The iOS selection screen now offers sharing the exact selected assets in one native share sheet, with the same completion/cancellation handling as the single-photo path.

- Freeze and validate selection before reads; read each requested resource, fail the entire request if one cannot be read. No implied original/result companions.
- Both Kotlin and Swift bound this first byte-array based implementation to 50 files / 64MiB total. Larger selections show a split-selection instruction. This is a documented iOS limit, not a claim of unrestricted Android parity.
- All native payloads are validated before temp-file creation. Each original filename lives in its own numbered subdirectory under a unique operation directory, so same-named original/result photos cannot overwrite each other.
- Share original bytes via file URLs, clean only this operation's temp directory on completion/error. No actual user sharing or storage mutation was performed while developing.

## Combined publication
This batch includes previously local `9d07265` (recent/default entries and cancellation safety), `1a8a3ba` (bounded scrollable header), and `b067a4f` (persistent density and byte-preserving sharing). Publish their entire cumulative tree, not only this last commit's file list. Earlier local-only docs record their status at the time; this combined publication supersedes that unposted status.

## Verification boundary
The one starting CI snapshot of prior published `4e0f86b`: Android passed, iPhone compile running, shared core/UI queued. No failure was reported. No waiting or repeated polling. Grouping multiple independent slices into one publication reduces churn, but superseded runs may still be cancelled by existing workflow concurrency.

Local diff/shell checks and 15 CI-reuse Node tests pass; selected XCTest names exist. Two common budget tests and selection-control assertions added. Prior slices add entry/preference tests and gallery relaunch assertions. None of the new Kotlin/Native, Swift/UIKit, sharing, layout or XCTest behavior was executed locally. Native compilation, system share cancellation, duplicate filenames in receiving apps, temp-file lifecycle and memory bounds need actual verification.

No merge/deploy, Android production or rotating-phrase changes. Remaining release work is principally exact visual/gesture parity, latest complete native CI and physical-device camera/Photos/rendering/recovery validation, not proof of completion from local checks.
