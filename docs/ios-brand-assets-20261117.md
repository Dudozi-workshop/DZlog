# iOS brand asset checkpoint

- The current Android Play Store camera artwork is the single visual source for iOS.
- `AppIcon` contains the required opaque 1024 x 1024 iOS marketing icon.
- `LaunchMark` provides 1x, 2x, and 3x centered launch images.
- `LaunchBackground` uses the app background color `#FBF8F3`.
- The launch configuration references asset names instead of device-specific files.

The Xcode target compiles the asset catalog through a standard resources build phase. Internal
PoC target names remain unchanged for CI compatibility; installed branding remains `DZlog`.
