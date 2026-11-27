# iOS App Privacy checkpoint

## App Store privacy answers

- Tracking: No
- Tracking domains: None
- Data collected by the developer or a third party: None
- Camera and photo-library content stays on the device unless the user explicitly opens the
  system share sheet or exports a workspace backup.
- No analytics, advertising SDK, account identifier, or background upload is present.

## Required-reason APIs

- `NSUserDefaults` stores the workspace, counters, recovery journal, onboarding receipt, and
  gallery preferences inside DZlog: `CA92.1`.
- Compose gesture timing uses monotonic uptime to distinguish tap, double-tap, and long-press
  interactions: `35F9.1`.

`PrivacyInfo.xcprivacy` is included in both the Xcode resources phase and the manually assembled
simulator/device application bundles. The release-metadata validator checks the manifest,
production identity, permission descriptions, asset catalog, and opaque 1024 px App Store icon.
