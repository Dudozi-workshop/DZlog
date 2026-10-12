# Table editor keyboard preview

Scope: full table editor Content tab. Camera quick edit and the deferred panel/nav separators are unchanged.

- Preserve the last unobscured preview scale before focusing a value field, through IME opening/closing and explicit input expansion. Release after IME closing settles.
- Keep the grid mounted with real-time draft values. The viewport clips and scrolls instead of fitting the grid to each keyboard animation frame.
- Centre the selected root cell (including a merged cell) on selection or viewport changes. Typing does not reset manual scrolling. Show a scroll thumb when the grid overflows.
- Hide the guide and type settings while the keyboard is visible; restore the normal panel and full preview when it closes.
- Text input uses a fixed collapsed height and an explicit expand/collapse button with the same icons/dimensions as quick edit. Expansion stays in the current screen; cursor/composition are retained. Numeric Decimal/± and counter input remain available.

Static review: checked imports, internal signatures, the sole canvas/panel call sites, bounded scroll measurement, merged-cell selection, and unchanged session/save callbacks. Fit/selection policies have regression tests; full tests, Kotlin compile and APK verification run in Android CI for the published HEAD.

Device checks pending:

- Open/close Samsung text keyboard repeatedly on a 6×6 tall grid: no table scaling pulse; selected cell remains visible.
- Type/delete Korean text and numbers: draft preview updates immediately, with no per-character recentering.
- Expand/collapse a multiline text field: same field, cursor and composition retained; grid magnification unchanged.
- Scroll the preview while typing, select the first/last/merged cell, close keyboard with Back: correct visibility and full preview restored.
- Switch Content/Structure/Style/Save, resize boundaries, undo/redo, and save/reopen: original editing behavior retained.
