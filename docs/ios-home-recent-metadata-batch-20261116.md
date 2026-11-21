# iOS home recent capture metadata batch — 2026-11-16

## Scope

- Replace the fixed iOS home subtitle `DZlog 저장 사진` with the actual PhotoKit creation time.
- Show `오늘 HH:mm` for today's capture and `yyyy.MM.dd HH:mm` for older assets, matching the Android home convention.
- Show the selected asset's current DZlog logical folder below the time while retaining its real saved filename.

## Safety

- PhotoKit remains the source of truth for creation time; no guessed timestamp is added to the private gallery index.
- Missing or limited-access assets remain excluded from the recent card. The latest present capture prefers its result role and falls back to any present role.
- Gallery moves immediately affect the logical location shown after the next home refresh; physical Photos metadata is not rewritten.

## Verification

- The existing real PhotoKit save/relaunch XCTest now opens home and verifies a current `오늘` timestamp, `DZlog/M3A-PhotoKit` path and removal of the fixed placeholder.
- Request the full iOS UI, shared iOS and iPhone compile workflows after publishing without waiting for completion.
- PR #87 remains Draft and unmerged.
