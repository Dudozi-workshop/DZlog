# iOS first-run onboarding — 2026-11-17

- Show the three-step DZlog introduction only when the production iOS store has no v2 workspace, recoverable v1 document or completed onboarding receipt.
- Reuse the shared Android/iOS starter catalog for basic, daily/hobby and work/field choices.
- Confirming a starter or skipping atomically writes an active iOS workspace before entering home. Existing workspaces and all legacy UI-test routes bypass onboarding.
- Reset the isolated production-startup test receipt only with the explicit reset launch argument, then verify onboarding completion, starter activation, settings persistence and relaunch bypass.
- PR #87 remains Draft and unmerged.
