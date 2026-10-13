# DZlog Capture Pipeline — QA #182 and pre-merge handoff

Date: 2026-10-13 (Asia/Seoul)

## Scope and evidence

- Repository: `Dudozi-workshop/DZlog`
- Pull request: https://github.com/Dudozi-workshop/DZlog/pull/33
- Feature branch: `feat/capture-async-pipeline-v1`
- Functionally tested commit: `a934bdc85036ee48418543e86b4442a1a2e00894`
- CI: https://github.com/Dudozi-workshop/DZlog/actions/runs/38412881052
- CI result: full Android unit tests, isolated QA APK build, artifact publication succeeded
- Artifact tested: `DZlog-Capture-QA-182` (`.captureqa` app ID suffix)
- Manual QA evidence: user's report in DZlog capture-improvement conversation; all eight items reported **PASS**. This is user-reported device QA, not an automated device-farm run.

## User-reported manual QA — PASS (8/8)

1. OFF rapid captures
2. SHORT post-save preview
3. HOLD post-save preview
4. Shutter sound and haptics
5. Shutter gating and re-enablement
6. Rotating phrases and scoped counters
7. Original-only / watermark-only / both save modes
8. Normal app restart and data persistence

## Code audit

- OFF, SHORT, HOLD use the same durable CameraX JPEG staging and bounded serial save queue. The modes vary only in post-save review.
- SHORT and HOLD open their review overlay after the matching committed receipt; the next shot is gated while the review is pending or visible.
- The max outstanding count is **3 in-flight jobs**, not three saved photos total.
- Capture start delivers sound and a short single-pulse vibration; JPEG save completion does not trigger another vibration.
- A reserved number and table snapshot are persisted before staging. Pending projections advance rotating phrases ahead of final save.
- Same-directory CameraX JPEGs are fsynced/hashed and promoted in place; other sources use the copy-and-sync fallback.
- Recovery automatically resumes only validated READY entries. Ambiguous PROCESSING/NEEDS_REVIEW and damaged journals are not blindly retried. Committed receipts are acknowledged only after durable template application.
- Audit did not identify a new defect reproducible from the reported 8/8 manual QA; no additional functional patch is required on that evidence.

## Outstanding release/integration gates

1. **Save-in-progress force-stop / restart** was not separately established by the user's normal-restart QA. Test this on the isolated QA app; compare MediaStore outputs, progress, and recovery indication.
2. **Injected storage/MediaStore failure** was not covered by the eight manual QA items. Test no silent loss, no duplicated counters, and visible recovery/error state.
3. Merge-base drift: integration has **nine commits** not yet in the feature branch (observed 2026-10-13). Only `AppNavigation.kt` overlaps by path. Integration removes a `GalleryPermissionOnboarding()` invocation; ensure its intended deletion survives the merge.
4. After integration, run `:app:testDebugUnitTest` and `:app:assembleDebug` again and check real gallery/camera onboarding. Mergeability reported by GitHub does not replace this regression check.

## Handoff status

- **Code implementation / happy-path QA:** done for this scope.
- **Branch:** keep open as draft until the two exceptional-failure QA cases and integration checks pass.
- **Data policy:** preserve existing installations and gallery files; avoid force merge and avoid clearing app storage to solve conflicts.
- **Next owner:** integration/release stabilization workflow. Do not expand scope to unrelated camera UI or gallery changes in this branch.
