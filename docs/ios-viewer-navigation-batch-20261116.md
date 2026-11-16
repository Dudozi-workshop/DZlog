# iOS viewer gestures and chrome · 2026-11-16

## Implemented
- A single gesture recognizer arbitrates single tap (controls), double tap (zoom), pinch/pan and fitted-image swipes. A gesture involving multiple fingers or starting zoomed never navigates to another photo.
- Fitted horizontal swipes invoke previous/next in the active folder/favorites order, without wraparound or an implicit jump to the whole library. Out-of-filter original/result companions still require the explicit companion button.
- Fitted upward swipe opens the existing photo-information dialog, following Android's 64dp / vertical-dominance policy. Horizontal navigation is a threshold action, not Android's animated pager implementation.
- Controls can be hidden by image tap or a button. A visible restore button remains available even for an inaccessible/loading image and for non-gesture use. Zoom persists when chrome changes the viewport; offsets are clamped to its bounds. New assets still start fitted.
- Processing/refresh blocks gesture actions. No storage writes are performed by gestures.

## Tests / evidence
- Four common policy tests cover directional thresholds, multi-pointer/zoom blocking and invalid values.
- Existing XCTest extends the real gallery path with fitted swipe to companion, control hiding/restoring, single-tap toggle and zoom retention. Existing zoom/load/selection checks remain.
- Local: whitespace and iOS shell checks; 15 Node CI-reuse tests passed; all 29 selected XCTest names exist.
- Native tests, Kotlin/Native compile and live gestures are not executed in this Linux workspace. Fast tap arbitration, pinch transitions, inaccessible-image recovery, VoiceOver and narrow-screen layout remain device/simulator checks.

## Status and PR metadata
Before work, head `60260d0` had all four CI workflows running. No wait/poll/retry. PR #87 description was still presenting Nov 7 manual quick inputs and read-only gallery as current; update its current overview while retaining older evidence under a clearly historical section. Previous successful suites are not evidence for this head.

No merge, deployment, Android production changes or rotating-phrase changes. Remaining scope includes dedicated recent/default-storage gallery routes, exact animated-pager/visual parity, final rotating-phrase parity and full native/physical-device release validation.
