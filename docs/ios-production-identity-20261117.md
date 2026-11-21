# iOS production identity checkpoint

- User-facing app name: `DZlog`
- Production bundle identifier: `com.dudoziworkshop.dzlog`
- UI test bundle identifier: `com.dudoziworkshop.dzlog.uitests`
- Marketing version / build: `1.0.0 (3)`, aligned with the current Android release
- Camera and photo-library permission descriptions are presented in Korean.

The existing Xcode project, target, scheme, executable, and build-script names retain the
`DZlogUiPoc` identifier for CI compatibility. They are internal build names and do not change
the installed app name. Apple development-team and provisioning settings remain intentionally
unset until the signing account is selected.
