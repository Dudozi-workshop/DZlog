# Editor panels and shared UI

## Implemented scope

- All four detail editor panels keep the Structure-based height. A soft rounded top boundary distinguishes the work panel; a straight stronger divider distinguishes the four-tab navigation. Colors, typography, icon sizes and spacing use existing theme tokens.
- The Content panel footer displays selected cell position, file/path usage icons and previous/next arrows. Current save-rule drafts use the same ID-based link policy as quick edit. Icon taps explain usage. Every cell kind is reachable in row-major order; covered merged cells are excluded, roots are visited once, ends are disabled and never wrap. Moving selection does not modify values or history, and does not open the keyboard.
- The footer hides while the keyboard is visible. The accepted fixed-scale preview and explicit input expansion remain in place.
- Reuse the `feat/ui-consistency-v1` foundation (`0cdf1c3a`): Cancel → Discard → Save, visibly distinct Discard buttons, and complete warm Material3 color roles.
- App-owned Material alert dialogs now share surface, shape and tonal defaults. Confirm/cancel/delete actions use the same DDZ button rendering as other buttons. Gallery deletion/movement/retries, folder create/rename/delete, template rename/delete, rotating text CRUD, counter scope, format, permission explanations and legacy unsaved changes were reviewed. Existing callbacks, enabled conditions, staged confirmations and MediaStore/SAF permission requests are retained.

## Validation

Static review: inspect new imports/signatures/aliases, theme roles, all action slots and return labels, editor layout constraints, selected ID/link adapters and screen callbacks. Meaningful tests cover every cell kind, row ordering, merged roots, missing selection and non-wrapping boundaries. Run full unit tests, Kotlin compile and verified APK build for the final integration HEAD.

Device checks pending:

- Four-tab work-panel/nav boundaries, no height jump when switching tabs.
- Content footer position/icons, icon explanation, every cell kind, merged cell once, first/last arrows disabled; typing hides footer and closing restores it.
- Keyboard scale stability and manual input expansion after adding the footer.
- Dirty exit and template switch: Cancel/Discard/Save order, enabled state during save, failure retains draft.
- Gallery/template/rotating-text confirmations on a small screen and enlarged font; cancel, staged previous/next, retry and permission flows.
