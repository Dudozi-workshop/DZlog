# DZlog iOS final parity audit · 2026-11-26

## Scope and evidence boundary

- Android reference: `origin/android-integration` at `4e4fecb7101d87f607dfab2591e900be277b6527`.
- iOS implementation: Draft PR #87, head before this document `452cee8cbe9e91fd1cb28bc19f0c62dbbc03ad44`.
- This is a code-path audit. It does not replace current-head native CI, matched image comparison, or a physical iPhone review.
- Do not infer completion from old November 2 gap lists. Many of those items have since been implemented.

## Current user-flow matrix

| Area | Current iOS state | Android comparison | Remaining gate |
| --- | --- | --- | --- |
| Startup, onboarding, permissions | Production starter, onboarding, camera/Photos states and Settings return are connected | Equivalent intent with iOS Limited Photos handling | Physical permission and revocation review |
| Home and projects | Shared home content, project create/select/complete/reopen/delete and persisted selection | Main project flows aligned | Physical narrow-screen review |
| Templates and table editor | Shared template catalog/editor, structure/style/save settings and backup import/export | Shared policies and presentation reused | Matched rendered-output review |
| Camera controls | Focus/exposure lock, zoom, flash, grid/table, assist shutter, quick values, settings, project switching and Undo are connected | Core controls present; platform shutter-sound behavior intentionally differs | Continuous capture queue gap below |
| Capture storage | Three save modes, placement, crop/orientation, PhotoKit transaction, recovery and exactly-once workspace commit | Save contract aligned in tested single-capture flow | Continuous queued saving and matched pixels |
| Quick results | Folder sections, 2–5 columns, pinch persistence, scroll anchor, badges, original-only state and scoped viewer | Current Android ordering and original/result behavior aligned | Current-head native UI execution |
| Gallery | Folder/grid, recent/default storage, favorites, ordering/filtering, selection, batch move/delete, share, viewer paging/zoom/info | Major Android gallery actions present | Native sheets, Limited Photos and long-list review |
| Settings/help/about | Real permissions, capture feedback, guide, credits and backup routes | Shared settings root reused | Physical presentation review |

## Blocking software mismatch: continuous capture saving

Android does not keep the shutter blocked for the entire media write. It stages an exact capture snapshot and submits a durable ID to a serial worker with a maximum of three outstanding captures. The worker quarantines uncertain writes instead of retrying them blindly.

iOS currently sets `busy = true` before `captureJpeg` and clears it only after `saveConfiguredJpeg` completes. `saveConfiguredJpeg` resolves the active workspace at save time and uses a single pending journal. Merely allowing another shutter press would therefore risk:

- assigning the same automatic counter to multiple photos;
- advancing rotating phrases in the wrong order;
- rendering with a template, project, placement or timestamp selected after the shutter press;
- making Undo target a photo that has not committed yet;
- retrying an uncertain PhotoKit write and producing duplicates.

### Required implementation contract

1. Reserve a capture ID and counter before invoking the sensor.
2. Freeze the exact document/template ID, project ID, resolved preview, phrase progress, save mode, placement, crop/aspect and quality used by that shutter press.
3. Persist the JPEG and frozen snapshot before acknowledging the capture as queued.
4. Process a serial queue with at most three outstanding captures, matching Android's bounded behavior.
5. Apply workspace progress strictly in reservation order and only once.
6. Treat `READY` work as resumable; quarantine processing or media-saved uncertainty for explicit recovery.
7. Keep project switching, template editing, settings changes, Undo and app exit guarded while their result would be ambiguous.
8. Show a pending-count/error indicator without using in-memory UI state as the source of truth.

### Tests required before enabling repeated shutter input

- Three rapid BOTH captures receive distinct consecutive numbers and preserve shutter order.
- A fourth capture is rejected while three jobs are outstanding without changing progress.
- Template/project/settings changes after the shutter do not alter queued output.
- Restart resumes verified-ready work and does not retry an uncertain PhotoKit write.
- One failed middle job does not apply later phrase/counter progress out of order.
- Undo is unavailable until the latest capture is durably committed, then removes exactly that capture.

## Intentional platform differences

- iPhone shutter sound follows iOS device and regional policy. Android's app sound/volume-key controls must not be imitated with a fake or policy-bypassing implementation.
- iOS uses PhotoKit and Limited Photos access; Android uses MediaStore/SAF. User-visible outcomes should align while permission UI remains native to each platform.

## Remaining release gates

1. Implement and verify the durable iOS continuous-capture queue above.
2. Compare Android/iOS rendered images using identical Korean text, merged cells, styles, orientation and placement fixtures.
3. Obtain latest iPhone ARM64, shared-core, unified and full 55-case native UI results for one exact head.
4. Prepare a signed/device-installable review build and checklist.
5. Perform physical iPhone camera, Photos, gesture, vibration, performance and interruption review last.
6. Keep PR #87 Draft and unmerged until explicit approval.
