# iOS project parity batch · 2026-11-20

Android reference: `integration/dzlog-20261010` project registry, picker, list and detail flow.

## Implemented in this batch

- Added a versioned iOS project registry with the same core rules as Android: creation does not select,
  only open projects can be selected, general/project template memory is independent, completing an
  active project returns to general capture, and metadata deletion never deletes Photos assets.
- Added strict registry encoding and round-trip validation in a separate iOS preference value. Existing
  editor workspaces and Android DataStore values are unchanged.
- Added the home project selector, general/open-project picker, project list, create/rename/complete/
  reopen/delete-metadata actions and project detail screen.
- Selecting a project restores its remembered shared template. A project/template selection writes both
  stores with rollback of the workspace if project persistence fails.
- Added common registry tests and an iOS UI path covering create, select and relaunch persistence.

## Deliberate boundary

This batch does not yet associate successful PhotoKit captures with the selected project. Project photo
counts therefore remain zero until the camera transaction batch connects confirmed capture IDs. The
disabled project-photo button makes that boundary explicit rather than opening an unrelated gallery.

## Capture association follow-up

- The camera session now appends a capture ID only after the existing PhotoKit transaction and workspace
  commit both succeed. A failed or cancelled capture never appears in a project.
- Latest-capture Undo removes the same capture ID from project metadata after Photos and workspace rollback.
  Any metadata write failure is reported as a saved/deleted-photo warning instead of falsely claiming that
  the PhotoKit transaction failed.
- Home reloads project metadata after returning from camera, and project detail can open a gallery filtered
  by its confirmed capture IDs. General gallery data and Android storage remain unchanged.

## Camera switching follow-up

- The camera now shows the current project directly below the filename bar. The picker changes between
  general capture and existing open projects without returning home.
- A project switch restores that project's remembered shared template, persists the workspace and registry,
  clears cross-project Undo state and reruns the existing automatic-counter synchronization.
- The UI test opens the camera from a persisted project and verifies that the camera picker exposes the same
  selection. Project creation and full metadata management intentionally remain on the home project screens.

## Quick results

- The camera opens the selected project's results or the current general capture session. Project detail
  opens the same results screen. Returning from its album or viewer restores the results and folder filter.
- Android and iOS share shutter grouping, newest-first ordering, folder sections and numbering. On iOS the
  persisted media role identifies companions even after a logical folder move. Only readable assets appear.
- The viewer pages through the visible captures, opens the result first and can show its original.
  General session results survive template and camera-setting edits that invalidate Undo.
- Continuing from project detail now enters the home camera flow, including first-capture configuration.

## Validation

`git diff --check`, shell syntax and source-level selector checks are local. This Linux environment cannot
download the Gradle wrapper and has no Apple SDK, so Kotlin/Native compilation and XCTest are delegated to
the existing macOS workflows. CI is requested as a checkpoint and is not awaited before the next batch.
