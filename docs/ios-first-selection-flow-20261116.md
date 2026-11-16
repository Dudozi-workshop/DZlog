# iOS first-selection flow — 2026-11-16

- When no template is active, the home capture action opens the existing template list. Selecting a template continues through the established save and camera-configuration flow.
- The isolated production-startup test now resets only with its explicit reset argument. Relaunches can verify actual saved state.
- Extend the startup XCTest to select the real basic starter, change photo quality, relaunch and verify both quality and direct editor entry.
- Prior commit `3fa9c590` passed iPhone compilation, Shared Core iOS and Unified Mainline. Its full UI workflow was cancelled; no full UI success is claimed.
- PR #87 remains Draft and unmerged.
