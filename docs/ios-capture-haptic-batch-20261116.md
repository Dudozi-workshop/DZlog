# iOS capture haptic feedback

Implemented; native compile and accumulated UI verification pending publication.

- Capture settings persist an independent iOS capture-haptic toggle, defaulting on for older workspaces.
- The impact generator prepares when the camera is ready and fires only after AVFoundation accepts a shutter request.
- Disabled haptics do not prepare or fire. Failed preflight capture attempts do not produce app haptics.
- The settings screen explains that iPhone shutter audio follows system and regional policy; it does not expose a misleading mute control.
- Workspace codec and the configured settings/relaunch route cover persistence. Simulator tests cannot prove physical vibration strength.
