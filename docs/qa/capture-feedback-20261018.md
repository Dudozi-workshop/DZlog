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
