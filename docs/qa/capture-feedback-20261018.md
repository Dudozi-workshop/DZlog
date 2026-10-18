# DZlog capture feedback — minimal device QA (2026-10-18)

**Build scope:** installed as `com.dudoziworkshop.dzlog.qa.sfx`. Does not overwrite or share app data with the production `com.dudoziworkshop.dzlog` install. Only use this artifact from the isolated build workflow.

## Required checks (5 groups)

1. **Existing design/navigation** — from root Settings > 촬영 설정 > 촬영 효과음 및 진동; also camera settings > feedback toggles > detailed settings. Existing camera settings rows, back button and panel appearance unchanged.
2. **Five sounds and preview** — play ① Android 기본 ② Contarex ③ Werra ④ DSLR ⑤ 뿌쀼!. Preview must not take a photo, change counter, create a file or vibrate. No duplicate/overlap when switching rapidly. Note actual perceived loudness and whether 뿌쀼! is balanced.
3. **Custom volume and persistence** — verify 0%, 70%, 100% behavior for at least one custom sound and 뿌쀼!; Android system slider disabled. Close/restart QA app and confirm sound choice/volume survive. Sound toggle OFF must mute custom playback during capture; ON restores chosen sound (subject to mandatory OS shutter policy).
4. **Vibration 4 states** — OFF, 약함, 보통, 강함 during an actual capture. Toggle vibration OFF/ON in existing camera panel; last chosen intensity should resume. Some devices lack amplitude control, so only duration can differ.
5. **One-capture regression guard** — take one photo with existing template and rotating phrase, verify original/result saved to expected paths; perform Undo and confirm photo+counter/phrase restore. Do **not** run full QA.

**Not yet validated:** build result, real-device audio/haptics, perceived loudness, any mandatory camera shutter restrictions. Do not merge feature branch until reviewed.

Source reference: `design-assets/capture-feedback/PROVENANCE.md`; original approved 뿌쀼! WAV SHA-256 `615ee6675c8d5821fea1584b15e27b0a10050001fa1aada69f2980be12734db1`.
