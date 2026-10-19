# Capture feedback source provenance · 2026-10-18

This branch adds all five approved choices without merging unrelated branches.

- Android system shutter: platform MediaActionSound (no bundled file).
- Soft Contarex: `dzlog_contarex_soft.mp3`; Git blob `6a512d22a5c69aa5b024695288db74022b9feeb1`. Source CC0 camera sound, derived upstream from pingdotgg/t3code `apps/web/src/assets/snap-shot-click.mp3`; see https://github.com/pingdotgg/t3code .
- Classic Werra: `dzlog_werra_classic.wav`; Git blob `59e87ec4c1bad33596cca2c0612553a8d8b05dae`; source hmilleo (Freesound 409093), distributed by Remotion under CC0.
- Digital DSLR: `dzlog_dslr_digital.wav`; Git blob `7b20a24cef779e9b20b64f3e0f72caef2b66bf9c`; source ristooooo1 (Freesound 539136), Remotion CC0.
- DZlog signature 뿌쀼! A: `dzlog_signature_ppuppyu.wav` (40/100 second, 48 kHz, mono, PCM 16-bit, 38,444 bytes), SHA-256 `615ee6675c8d5821fea1584b15e27b0a10050001fa1aada69f2980be12734db1`; **exact original** recovered from embedded WAV in previously approved `DZlog_capture_feedback_UI_preview.html` in the user's DZlog file library; SHA-256 matched the 2026-10-18 approved signature handoff. Self-created original, not replaced or remixed.

**Outstanding:** Android Kotlin compilation, scoped unit tests, offline audible comparison, device-specific actual capture and vibrator QA. No PR merge or install was performed here; preserve existing user photos, Undo, rotating phrases and onboarding.
