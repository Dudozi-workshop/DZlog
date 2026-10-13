# Required permission management QA

The app now checks actual camera and full photo grants plus the persisted
Pictures/DZlog folder connection before composing AppRoot.
A single-action centered dialog leads the user through permissions first and
folder connection second, blocking entry until all three settings are complete.
Outside touches do not dismiss the dialog; Back finishes the activity.
Partial photo access is explicitly identified and cannot masquerade as a complete gallery.
Returning from Android settings and permission request callbacks re-read actual grants.
Permission changes invalidate in-memory gallery snapshots before entry is restored.
Folder SAF access only accepts the exact internal-storage Pictures/DZlog tree.
The picker starts at that location when the device supports it; a wrong folder is
never saved and returns to the guide with the selected path visible.
Settings > Permission management shows camera, photo and folder connection status.

Device checks:
- Fresh install: camera + full photo grant advances to required folder connection.
- Connect exact Pictures/DZlog: the dialog closes and home opens.
- If Pictures/DZlog does not exist yet, create a folder named DZlog inside Pictures
  from the system picker, then select it.
- Deny either permission: gate remains; button opens settings after the first request.
- Select some photos: status says partial access; grant all to enter.
- Revoke photos in Android settings, then return/relaunch: gate replaces gallery content.
- Revoke camera alone: camera is listed first as missing.
- Return from settings without changing grants: gate remains, no automatic request loop.
- Restore both grants and the folder connection: home recent photos and gallery
  reload; no stale folder counts.
- Outside tap has no effect; Back exits; relaunch rechecks grants.
- Settings > Permission management: status cards and app-settings link work.
- Folder connect: cancellation returns to the gate; a wrong folder shows its path
  and retries; only internal-storage Pictures/DZlog works.
- Existing capture/save/table editing and folder delete flows remain functional.

Validation in this workspace:
- git diff --check passed.
- Android manifest declares CAMERA, READ_MEDIA_IMAGES, READ_MEDIA_VISUAL_USER_SELECTED,
  and legacy READ_EXTERNAL_STORAGE with maxSdkVersion=32.
- Permission-state transition unit tests cover full, partial, denied and revoked grants.
- Previous permission-gate revision passed remote unit tests, Kotlin compilation,
  update-compatible APK verification and APK assembly.
