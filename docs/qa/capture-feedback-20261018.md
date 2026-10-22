# DZlog capture feedback — minimal device QA (2026-10-18)

**Latest QA build:** `cde4f14c` · [Actions #39107717419](https://github.com/Dudozi-workshop/DZlog/actions/runs/39107717419) · package `com.dudoziworkshop.dzlog.qa.sfx2` (isolated from production and previous sfx QA). Does not overwrite or share app data with the production `com.dudoziworkshop.dzlog` install. Only use this artifact from the isolated build workflow.

## Required checks (5 groups)

1. **Existing design/navigation** — from root Settings > 촬영 설정 > 촬영 효과음 및 진동; camera settings keep sound/vibration quick toggles but the extra link to detailed settings is removed. Existing camera settings rows, back button and panel appearance unchanged.
2. **Five sounds and preview** — play ① Android 기본 ② Contarex ③ Werra ④ DSLR ⑤ 뿌쀼!. Preview must not take a photo, change counter, create a file or vibrate. No duplicate/overlap when switching rapidly. Note actual perceived loudness and whether 뿌쀼! is balanced.
3. **Custom volume and persistence** — verify 0%, 70%, 100% behavior for at least one custom sound and 뿌쀼!; Android system slider disabled. Close/restart QA app and confirm sound choice/volume survive. Sound toggle OFF must mute custom playback during capture; ON restores chosen sound (subject to mandatory OS shutter policy).
4. **Vibration 4 states** — OFF, 약함, 보통, 강함 during an actual capture. Toggle vibration OFF/ON in existing camera panel; last chosen intensity should resume. Some devices lack amplitude control, so only duration can differ.
5. **One-capture regression guard** — take one photo with existing template and rotating phrase, verify original/result saved to expected paths; perform Undo and confirm photo+counter/phrase restore. Do **not** run full QA.

**Build verified:** scoped Kotlin compile and 5 unit tests passed on `a665e433`; isolated QA APK assembly and upload passed on `cde4f14c`. **Not yet validated:** preview vibration change across all strengths; actual-capture vibration strength; perceived loudness, mandatory camera shutter restrictions; save/Undo/phrase regression and remaining checklist items. Do not merge feature branch until reviewed.

Source reference: `design-assets/capture-feedback/PROVENANCE.md`; original approved 뿌쀼! WAV SHA-256 `615ee6675c8d5821fea1584b15e27b0a10050001fa1aada69f2980be12734db1`.

## User device QA result / scoped closeout — 2026-10-18

**User accepted ending this iteration with one explicitly unverified check.** This is an implementation/feedback-loop closeout, **not full feature QA PASS, integration sign-off or release approval**.

| Focused check | Result | Evidence |
|---|---|---|
| Removed redundant camera-settings link | **PASS** | User: "통과 - 카메라 촬영 설정에서 불필요한 설정 이동 링크가 제거됨" |
| Five audio previews work | **PASS** | User: "통과 - 효과음 5종 모두 미리 듣기가 정상 작동함" |
| Strength selection immediately demonstrates OFF/weak/normal/strong | **UNVERIFIED** | User: "미확인 - 진동 OFF·약함·보통·강함 선택 시 즉시 예시 피드백이 작동함" |

- User closing comment: "마감해도 될 듯". Do not convert the third item into a pass.
- No new implementation changes requested here. Existing photos, save, Undo, rotating text and user settings must remain protected; no merge, full QA or release was performed.
- Feature remains on `feat/capture-feedback-options-20261018`; runtime implementation is in commit `a665e433` and latest QA-workflow commit is `cde4f14c`. Dedicated CC0 source intake branch remains separate.
- When resuming: verify the vibration sample triggered on changing strength (including no vibration when OFF) on an actual device, then capture feedback and one-shot regression only as needed. Independently approve any merge or Play release.

## Final user device acceptance — 2026-10-18

User subsequently confirmed **all remaining real-device checks work correctly** and explicitly requested final completion.

- PASS — Removed unnecessary camera settings link.
- PASS — Preview playback of all five selected sounds.
- PASS — OFF/weak/normal/strong vibration instant example response.
- PASS — Selected sound and vibration respond on actual camera capture.
- PASS — Sound selection, app volume and vibration settings survive app restart.
- PASS — Single-photo original/result save, Undo, counter and rotating phrase behavior remain normal.

This supersedes the earlier **UNVERIFIED** note for vibration preview: it was pending at the prior checkpoint, but the user has now confirmed it and all follow-up device items pass.

Review gate: feature-focused Android Kotlin compilation and five related unit tests succeeded on commit `a665e433`, and the isolated sfx2 APK build succeeded on `cde4f14c` (run [#39107717419](https://github.com/Dudozi-workshop/DZlog/actions/runs/39107717419)). Sound asset provenance and the approved signature WAV are unchanged.

Scope: feature work only; no changes to media persistence, Undo, rotating phrase source or onboarding. An optional integration checkpoint may include feature code/tests/assets and this record, while omitting branch-specific QA-only workflows. Play Store publishing is **not** authorized by this acceptance.


## DZQ-018 · 2026-10-22 quieter and balanced capture sound (Draft PR #89)

The 2026-10-18 acceptance above is a historical result. A new loudness complaint
prompted this *unmerged* revision, which must pass new device QA.

- **Default sound:** res/raw/dzlog_default_soft.wav, independently synthesized
  160 ms, 22,050 Hz, mono PCM16, 7,100 bytes, approximately -36.3 dBFS RMS.
  Git blob 7e390883740360579a32609c2c893c8f2e3cfca0.
  This is a new QA candidate, not a modification of any previously approved file.
  The persisted default choice ID remains 0; the label now says "기본 셔터 (부드럽게)".
- **All five source sounds stay intact:** existing Contarex, Werra, DSLR and
  "뿌쀼!" sample bytes remain unchanged. Playback gains are default 1.00,
  Contarex/Werra/DSLR 0.50 and signature 0.06. These gains are an approximate
  starting point from historical WAV RMS analysis, *not* calibrated perceived
  loudness across devices. Both actual exposure and preview multiply the same
  per-sound gain by the saved 0-100% slider.
- **Audio playback:** All optional sounds use bundled samples on the media
  stream. Selected sounds that fail to load no longer switch to a louder
  MediaActionSound fallback. The system sound remains reserved for a device
  whose platform requires shutter playback.
- **Regional device exception:** On API 33+, when
  MediaActionSound.mustPlayShutterSound() reports required, actual capture
  must play the platform shutter regardless of the app selection or mute toggle.
  Such a mandated shutter cannot be attenuated by the app; the settings screen
  discloses this. The preview still demonstrates the app's soft sounds.
  No system volume changes or mandatory-audio bypass.
- **Build/test gate:** Focused policy test and Kotlin compilation, then
  isolated QA APK dzlog-sfx3-volume-qa-apk
  (package com.dudoziworkshop.dzlog.qa.sfx3). Until latest Actions run
  succeeds, neither code correctness nor physical device audibility is verified.
- **Physical device QA:** Compare 5 choices at 0/30/70/100%, preview vs first
  and repeated actual captures, different system media-volume positions,
  app restart/persisted choice, capture sound OFF/ON and mandatory shutter
  behavior. Confirm no duplicate click, a clear default soft click on
  non-required devices and no camera preview/save/Undo/rotation regressions.
  Capture media persistence and counter policies are unchanged.
