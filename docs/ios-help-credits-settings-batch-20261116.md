# iOS help and credits settings batch — 2026-11-16

## Scope

- Replace the unavailable iOS `사용법 및 도움말` route with a read-only help index, introduction and module readers.
- Keep the same five approved functional modules as Android with iOS-accurate read-only guidance.
- Replace the unavailable iOS `도움 주신 분들` route with the same thank-you copy and names as Android.

## Safety

- Help and credits never request permissions or change workspace, capture, media or settings data.
- The introduction is explicitly read-only and does not choose or replace a template.
- Export and import remain unavailable in this batch.

## Verification

- XCTest opens both settings routes, advances a camera help page, returns through the read-only introduction and verifies the credits content.
- Request the existing full iOS UI, shared iOS and iPhone compile workflows after publishing, without waiting for completion.
- PR #87 remains Draft and unmerged.
