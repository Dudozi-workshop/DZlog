# iOS gallery viewer batch · 2026-11-16

## Implemented
- Saved original/result photos now support 1–4× pinch zoom, bounded pan, double-tap 2×/fit, and accessible zoom out/in/fit controls with a percentage indicator. Zoom and pan reset for a different asset or viewport size. Fitted aspect ratio is retained and gestures cannot pan the image entirely offscreen.
- Viewer actions and list filters scroll horizontally rather than squeezing Korean labels off a narrow iPhone screen.
- Opening an original/result companion outside the current favorites/folder filter no longer expands previous/next navigation to the entire library. Companion switching remains available; returning to the list reconciles the filter selection as before.
- The viewer can reload Photos metadata and retry image decoding without leaving the screen. A revision explicitly retriggers the load even when metadata is unchanged, transient image-read failures show a loading state on retry, and cancelled prior loads do not emit a false failure.

## Regression coverage
Extended existing `testGalleryFolderGridViewerAndOriginalResultSwitch` for zoom controls, fit reset, reload and companion zoom reset. Extended `testGalleryFavoriteAndEmptyFolderManagementPersist` for bounded navigation while showing a non-favorite companion. Reload waits for a zoom-state reset instead of accepting the pre-existing success label; optional companion queries no longer invoke a helper that asserts the optional button exists. Previous/companion/next navigation uses equal widths to remain on-screen. Both tests remain in the existing media scope; no extra CI run requested.

## Verification and limits
Local: whitespace diff check, all iOS shell script syntax checks, 15 CI-reuse Node tests, selected XCTest method-name consistency. These checks do NOT execute Kotlin/Native, XCTest or iPhone gestures. Pinch/double tap, long-image pan boundaries, narrow-screen scrolling and iCloud failure/retry still require simulator/device checks. No physical device available.

Previous head `efee0dc` snapshot at start: shared core and Android passed; iPhone device compile and UI workflow running. Did not wait or poll for completion. This batch is published once after local review; no merge/deployment. Android rotating phrases untouched.
