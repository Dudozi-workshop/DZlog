# Table editor compact controls — 2026-10-11

## Behavior

- Structure limit is 6 rows × 6 columns. Add controls show their limit and disable at capacity.
- Template selector and undo/redo share the preview card header; the single tab hint sits below the card.
- Normal tabs retain one fixed tool band; only its contents scroll. Style controls use six inline label/control rows.
- Structure controls pair row/column addition and deletion, followed by merge/unmerge and equalization. Blank preview space clears structure selection.
- Terminal right/bottom handles adjust the final column/row weight independently; internal handles redistribute adjacent weights. All handles contain one bidirectional arrow. Extent uses weight sums; equalization retains those sums and keeps cell data unchanged.
- File/path rule lists explain the existing swipe-to-reveal deletion action; no persistent trash controls are added.
- Template management uses the shared top bar, top create action, mini previews and modified/used/name sorting.
- Successful media save records the trigger's template ID and last-use time. Catalog serialization is backward compatible. Atomic persistence retains newer usage metadata across stale snapshot saves. Opening or selecting a template does not record usage.

## Verification

Static review: changed callbacks, API visibility, catalog persistence, imports and diff checked.
Regression additions: reach 6×6 without losing existing cells; terminal resize bounds and equalization extent; legacy usage default, sorting and serialization; stale snapshot persistence.
CI unit tests and APK: pending final HEAD build.
Device verification: pending; check tab preview stability, keyboard spacing, six inline style rows on narrow screens, terminal dragging and undo, direct row/column deletion, and use-order after capture/restart.

Full application UI/system-bar consistency audit remains separate follow-up work.

## Device-feedback refinement

- Normal tab height now derives from four structure action rows, their gaps/divider/padding (209dp at the default font scale). Large fonts expand that reference; short viewports cap it and scroll contents. Removed the structure status/help footer.
- Preview history icons are 18dp inside compact 40dp buttons. The style border toggle is a 32×18dp track with a 48dp touch area and switch semantics.
- Save settings labels have small existing file/folder/number icons. File/path add/type menus share a 2×2 icon+text layout for cell/manual/date/time. Legacy rotating-text records remain readable but cannot be newly selected; rotating cells remain usable through cell references.
- Editor-only fitting permits upscaling to maximize the preview at the saved aspect. A drag holds its starting zoom and never zooms in automatically; growing geometry can zoom out only to stay within the viewport. Release smoothly refits in 180ms. Fit does not change template data or edit history.
- Added fit regression checks for shrink/release aspect retention and viewport containment on growth/tall shapes. Final CI/APK status is recorded in PR #32; device interaction checks remain pending.
