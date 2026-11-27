# iOS continuous capture preview

Implemented; accumulated native validation requested after publication.

- Capture settings now persist the Android-compatible `off`, `short` and `hold` values in the iOS workspace.
- Short review closes after 1.5 seconds, matching Android. Hold review closes when the photo area is tapped.
- The review uses the committed session thumbnail. Capture, settings, navigation, quick input and Undo stay disabled while it is visible.
- Legacy workspaces default to review off. The workspace codec round trip and native settings/relaunch route cover persistence.
- Two unrelated full-suite assertions were stabilized: camera return waits on the disabled shutter, and favorite filtering waits on the persisted thumbnail rather than the optional large preview.
- Simulator coverage verifies state and UI routing. Photo timing, display color and touch feel still require physical-device QA.
