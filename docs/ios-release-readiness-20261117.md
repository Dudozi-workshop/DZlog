# DZlog iOS release readiness — 2026-11-17

PR #87 is Draft and unmerged. Implementation and store preparation are not release approval.

## Validation snapshot

Source checkpoint: `5dd255dd9170010b50dfc8a446b3c6e95b5cddf0`.

| Check | Observed result |
| --- | --- |
| Unified CI | Success: https://github.com/Dudozi-workshop/DZlog/actions/runs/44078693377 |
| Shared iOS core | Success via identical-input evidence reuse; native compile/test steps skipped in this run: https://github.com/Dudozi-workshop/DZlog/actions/runs/44078693391 |
| iPhone unsigned build | Success: https://github.com/Dudozi-workshop/DZlog/actions/runs/44078693347 |
| Complete iOS UI | Cancelled after a subsequent documentation commit: https://github.com/Dudozi-workshop/DZlog/actions/runs/44078693424 . Not a passing UI result. |

The previous gallery compilation failure was an implicit `maxHeight` receiver inside a nested layout. The checkpoint captures the height in `BoxWithConstraints` before entering `Column`. The device build now succeeds; complete UI results are still required.

PR path filters evaluate the accumulated PR diff, so even a docs-only follow-up can trigger these workflows and cancel the prior run. Also, the full-UI marker is read only from the current head commit message. Publish this checkpoint with `[ios-full-ui]`, then keep additional preparation local until the next result snapshot. Do not poll or wait, and do not repeatedly publish documentation-only updates while full UI validation is running.

## Source preparation completed

- Production identity: DZlog, `com.dudoziworkshop.dzlog`, iOS version `1.0.0 (3)`.
- First-run onboarding, icon/launch assets, Korean permission descriptions and privacy manifest.
- Common Android/iOS support and privacy pages; links in shared settings.
- Korean App Store text draft: `app-store-connect-ko-draft-20261117.md`.
- Capture coordinator tests extended for successful/failed save, journal recreation, repeat recovery, all save modes and rollback/retry. Two phrase cells exercise item rollover and individual-interval accumulation alongside the counter.

## Remaining work

| Item | Concrete completion evidence |
| --- | --- |
| Native/UI validation | Successful latest iPhone build and complete UI run; inspect failures before new changes. Confirm the extended coordinator tests have executed or have matching successful evidence. |
| Physical-device QA | Actual camera/Photos permissions, sensor capture, focus/exposure/flash, continuous save, recovery, Korean input and external/iCloud Photos behavior. Simulator success does not cover these. |
| Signing | Select the Apple Developer team and provisioning for the production bundle ID. No team/profile is set in the project. |
| Distribution build | Prepare and validate a Release archive and App Store export using the chosen team. Current device workflow links a debug framework and does not produce an App Store archive. |
| Store media | Capture current iOS screens; check layout on supported device families. Current target declares iPhone and iPad (`1,2`), so verify iPad scope before submission. |
| Store submission | Enter confirmed metadata, screenshots, privacy/age-rating answers, select a processed signed build and submit. Current draft is not an App Store Connect submission. |

## Existing artifacts and their limits

The device workflow artifact is `dzlog-m3a-iphone-unsigned-build` (7-day retention).

- `dzlog-ios-device-UNSIGNED.zip`: diagnostic device app; not directly installable.
- `dzlog-ios-device-CLOUD-RESIGN.ipa`: ad-hoc signed package for a service that replaces signing; not a TestFlight/App Store build or a directly installable user build.

Public URLs:

- Support: https://dudozi-workshop.github.io/dzlog-privacy/support.html
- Privacy: https://dudozi-workshop.github.io/dzlog-privacy/

No release percentage is assigned until the remaining validation and signing gates are resolved. Older dated batch documents describe their own checkpoints, not the current remaining-work list.
