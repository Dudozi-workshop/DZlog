# DZlog Design System v1 Usage Rules

- UI colors must come from `DDZColor`; avoid screen-level hex literals except explicit camera/viewer overlay cases.
- `Primary` is Light Brown for brand/primary actions.
- Sage tokens are for selected/active state, not general primary actions.
- Destructive actions use `Destructive` / `DestructiveSoft` only.
- Main UI is light-only. Dark surfaces are explicit camera/viewer overlay exceptions.
- Typography must use `DDZTypography`; avoid hardcoded font sizes/weights in screens.
- Main section labels use strong primary text contrast; secondary text must not look disabled.
- Spacing must use `DDZSpacing` and follow the 4dp grid.
- Prefer DDZ common components over raw Material controls when a DDZ equivalent exists.
