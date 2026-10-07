# DZlog Update QA Gate

Date: 2026-10-08  
Status: Active

## Purpose

Every repository update must be reviewed twice before it is treated as complete.

## Gate 1 — Immediate static review

Run immediately after the patch is written, before moving to the next feature.

Check:
- changed-file diff and adjacent call sites
- missing imports / unresolved symbols
- renamed parameters and call-site compatibility
- navigation and callback wiring
- resource references and strings
- Screen / Composable responsibility boundaries
- duplicate defaults, direct literals, and avoidable hardcoding
- new files included in the correct package and references
- no accidental feature-semantic changes

Result:
- Fix all obvious compile/reference issues before stacking another patch.

## Gate 2 — Build / regression review

Run after Gate 1 passes.

Check:
- Android CI unit-test job
- Android CI debug-apk job
- inspect logs when either job fails
- verify the latest HEAD, not an older intermediate commit
- confirm APK artifact exists before sharing an install/download link
- run the feature-specific regression checklist on emulator/real device when applicable
- verify back navigation, state persistence, and data SSOT for touched flows

Result:
- Only mark the update complete after both CI jobs pass and the relevant interaction path is verified.

## Reporting rule

For future DZlog updates, report status using:
- Patch applied
- QA 1/2: static review PASS / issues found
- QA 2/2: CI + regression PASS / pending / failed
- Latest HEAD
- APK link only when generated from the verified HEAD

## Current rationale

This gate was added after the H3-3 Home refactor exposed several simple compile omissions across multiple files. The goal is to catch local reference/import mistakes before CI, then use CI and runtime regression as an independent second check.
