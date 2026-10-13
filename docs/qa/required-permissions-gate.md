# Required permission management QA

The app now checks actual camera and full photo grants before composing AppRoot.
A single-action centered dialog blocks entry until both grants are present.
Outside touches do not dismiss the dialog; Back finishes the activity.
Partial photo access is explicitly identified and cannot masquerade as a complete gallery.
Returning from Android settings and permission request callbacks re-read actual grants.
Permission changes invalidate in-memory gallery snapshots before entry is restored.
Folder SAF access remains optional and has its own existing guide and exact-root validation.
Settings > Permission management shows camera, photo and folder connection status.

Device checks:
- Fresh install: camera + full photo grant closes the dialog and enters home.
- Deny either permission: gate remains; button opens settings after the first request.
- Select some photos: status says partial access; grant all to enter.
- Revoke photos in Android settings, then return/relaunch: gate replaces gallery content.
- Revoke camera alone: camera is listed first as missing.
- Return from settings without changing grants: gate remains, no automatic request loop.
- Restore both grants: home recent photos and gallery reload; no stale folder counts.
- Outside tap has no effect; Back exits; relaunch rechecks grants.
- Settings > Permission management: status cards and app-settings link work.
- Folder connect: cancellation changes nothing; wrong folder retries; Pictures/DZlog works.
- Existing capture/save/table editing and folder delete flows remain functional.

Validation in this workspace:
- git diff --check passed.
- Android manifest declares CAMERA, READ_MEDIA_IMAGES, READ_MEDIA_VISUAL_USER_SELECTED,
  and legacy READ_EXTERNAL_STORAGE with maxSdkVersion=32.
- Added permission-state transition unit tests (not yet executed).
- Gradle compile could not start: Gradle 8.7 download blocked by workspace network.
- Remote CI not started: automatic approval review rejected source upload pending
  explicit confirmation of Dudozi-workshop/DZlog as the upload destination.
