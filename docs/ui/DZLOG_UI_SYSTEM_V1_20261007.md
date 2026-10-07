# DZlog UI System v1

Date: 2026-10-07  
Status: Planning Approved / Production UI migration pending  
Brand: Dudozi Workshop

## 1. Direction

DZlog adopts a warm, clean, rounded utility style aligned with Dudozi Workshop.

Core keywords:
- Bright
- Warm
- Rounded
- Clean
- Emotional but not childish
- Functional at a glance
- Consistent across screens

Core rule:

> Soft color, strong text contrast.

## 2. Color System — A / Light Brown Main

| Role | Hex | Usage |
|---|---|---|
| Background | `#FBF8F3` | App background |
| Surface | `#FFFDF9` | Card / sheet |
| Soft Beige | `#EEE4D8` | Secondary surface |
| Primary Brown | `#A98F78` | Primary action |
| Primary Dark | `#6D5645` | Strong brown accent |
| Sage Accent | `#7E9670` | Selected / active state |
| Text Primary | `#332A25` | Main text |
| Text Secondary | `#6F655D` | Supporting text |
| Border | `#D8CEC4` | Border / divider |
| Delete | `#D96862` | Destructive action |

Role policy:
- Brown / Beige = brand + primary action
- Sage = selected / active state
- Red = destructive only

## 3. Typography Hierarchy

| Role | Spec |
|---|---|
| Screen Title | 20sp / SemiBold |
| Section Title | 17sp / SemiBold |
| Setting Label | 15sp / Medium |
| Body | 14sp / Regular |
| Secondary | 13sp / Regular |
| Caption | 12sp / Regular |

Rules:
- Supporting text must remain readable and must not look disabled.
- Disabled color is reserved for truly disabled states.
- Main title, values, actions and selected states need clear luminance contrast.

## 4. Button Families

### Primary
Use for Save / Complete / Apply.
- Primary Brown fill
- White text
- Rounded

### Secondary
Use for Advanced Settings / Select / Add.
- Light surface
- Border
- Dark text

### Text
Use for Cancel / Close.
- No fill

### Destructive
Use only for destructive actions.
- Soft Red
- Avoid red outside delete / destructive flows

## 5. Bottom Navigation

Fixed four categories:
- 내용
- 구조
- 스타일
- 저장설정

Rules:
- Icon + text
- Selected item: Sage Soft background + deep Sage
- Unselected item: dark brown-gray
- Must read as navigation, not four heavy cards

## 6. Structure Tab

First-level actions are fixed to:
- 행 추가
- 열 추가
- 병합 / 병합 해제
- 삭제

Rules:
- Row / column add icons must communicate orientation immediately.
- Merge button stays in the same position; label changes to 병합 해제 for merged selection.
- Delete opens Quick Choice.

## 7. Delete Quick Choice

Only two buttons:
- 행 삭제
- 열 삭제

Rules:
- No title
- No description
- No cancel button
- Outside tap closes
- Light surface
- Both buttons use destructive color
- Horizontal left/right layout

## 8. Bottom Sheet / Dialog Consistency

Bottom sheets:
- Light surface by default
- Shared radius / handle / padding / title hierarchy / button height
- Dark sheets are not allowed except explicit camera-overlay exceptions

Dialogs:
- Quick Choice
- Confirm

Avoid introducing additional dialog families without a clear need.

## 9. Settings Row

Standard pattern:

**Label / Value / Chevron**

Examples:
- 파일명 / 셀_셀_DZlog_0012.jpg / >
- 저장 위치 / Pictures/DZlog/ / >
- 자동번호 / 다음 번호 0012 / >
- 고급 설정 / 상세 옵션 / >

## 10. Table Editor Top Bar

Keep:
- Back
- 표 편집
- Undo
- Redo
- Save

Rules:
- Keep Undo / Redo visible.
- Save uses Primary Brown.
- Maintain strong text/icon contrast.
- Avoid changing the screen title by tab; use bottom navigation to communicate editing mode.

## 11. Rollout Order

1. Color + text contrast
2. Bottom Sheet / Dialog consistency
3. Button families
4. Bottom navigation + Structure tab
5. Save Settings
6. Content / Style
7. Top bar polish + full UI regression QA

## 12. Implementation Rule

This UI System must be applied without changing the current production feature semantics.

Refactor visual components first; preserve domain logic and editor behavior.

The approved visual board is stored with the planning record in Notion.


## 13. Home H3-3 — Final Visual Direction

Approved on 2026-10-08.

Baseline:
- H3-3 textured / film-emotional home concept.
- Warm cream background, natural light, leaf shadow and subtle paper/film texture.
- The home remains a shooting tool first; atmosphere must not reduce legibility.

Home hierarchy:
1. DZlog + Settings
2. Current capture state
   - `현재 촬영`
   - active template name
   - next capture number
3. Primary brown `촬영 시작`
4. Recent capture
   - section title `최근 촬영`
   - right action `앨범 보기 >` opens full album
   - tapping the recent image opens the album/grid focused on that capture context
   - thumbnail + filename + time
5. Bottom utility actions
   - `표 상세설정`
   - `저장 설정`

Visibility policy:
- Do not show table dimensions such as 3×4 on Home.
- Do not show full save path or long naming-rule details on Home.
- Do not show an extra chevron on the current-capture information block.
- Keep the recent image slightly smaller than the earlier mock so the layout reads cleaner.

Implementation targets:
- Active template name: about 19–20sp SemiBold.
- Primary capture button label: about 15–16sp SemiBold.
- Bottom utility actions: about 42–46dp height, 13–14sp text.
- Validate proportions on tall Android phone ratios around 9:19.5–9:20.
- Existing Step 5B Home implementation is considered a prototype and must be revised to this H3-3 baseline.


## 14. Home H3-3 — Implementation Cleanup

Updated on 2026-10-08.

Hardcoding cleanup:
- Home layout measurements moved to `HomeUiSpec`.
- Ambient leaf/light geometry moved to `HomeAmbientSpec`.
- Warm ambient color moved to semantic token `DDZColor.AmbientWarm`.
- Home user-facing copy moved to `strings.xml`.
- Shared app defaults centralized as `AppSettings.Default`.
- Home counter / settings / latest MediaStore item loading moved from the Composable into `HomeViewModel`.
- Recent capture UI extracted to `HomeRecentCaptureSection`.
- `HomeScreen` is now focused on rendering + event wiring and is below the 300-line screen threshold.

Save-settings entry:
- Home `저장 설정` now opens the active template editor directly with the save-settings tab selected.
- The editor still owns the same save-rule draft/session and save pipeline; no duplicate save-settings store was introduced.
- If there is no active template, the user is routed to the template list instead of creating an invalid editor state.

Remaining gate:
- Android CI must pass on the latest HEAD.
- Then validate the H3-3 proportions on a tall Android device and perform final typography/spacing polish.
