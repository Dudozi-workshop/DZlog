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

## Style order and fixed cell value

- Style rows: background, background opacity (only for light/dark backgrounds), text size, text color, alignment, border. Hiding opacity preserves its saved value.
- Content keeps the selected-cell header and value above the scrolling type-specific settings. Padding is 16dp horizontal / 8dp vertical, with a 40dp close button and 18dp icon. Keyboard mode keeps only the fixed input area.
- Structure add buttons retain the existing row/column icons with +; delete buttons use − and destructive red, faded when unavailable.
- Shared panel height and preview fit policy are unchanged. Static review passed; final-HEAD CI and device checks are tracked in PR #32.

## Consistent compact tabs

- Equalization uses row height on the left / column width on the right, matching add/delete.
- Content and Save retain two-line settings: title above value, 16dp icons with 4dp icon/text gaps, and single-line ellipsis for values. Style retains single-row controls. The panel inset is 16dp horizontal / 8dp vertical, with 4dp gaps.
- Cell type values and filename/path rule type values include their existing icons. Rule type badges have a bounded width and long values use ellipsis.
- Text color control is removed. Editor draft uses automatic text contrast, including legacy manual colors; persistence changes only when explicitly saved. A regression verifies draft normalization, undo, and saved baseline.
- Final-HEAD CI/APK status and device checklist are tracked in PR #32.

## Cell icons and numeric keyboard

- Shared DDZCellTypeIcons uses Material Filled Pin (123) for Number and ExposurePlus1 (+1) for AutoNumber; current/legacy editors and save settings reuse the same definitions.
- Number cell values and auto-number start inputs request KeyboardType.Number. Text cells keep KeyboardType.Text, and unrelated DDZTextField callers retain the default keyboard.
- Static review passed. Final-HEAD CI/APK results are tracked in PR #32. No additional device review requested for the icon change.
