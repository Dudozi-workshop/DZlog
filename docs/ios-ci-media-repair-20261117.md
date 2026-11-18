# iOS media CI repair checkpoint

Baseline: `485567c1f971723c67e8cefe2660490897bd324a`.
Evidence: [UI run 44100590970](https://github.com/Dudozi-workshop/DZlog/actions/runs/44100590970), media evidence artifact `14339057763`.

The PhotoKit selection ran 25 tests: 14 passed and 11 failed. The later full suite did not run. Shared core tests, iPhone compile and unified CI passed for the baseline; these are not a pass for this repair.

## Separate failure classes

- Seven failures: capture settings' BOTH mode was below the viewport. Tests now scroll the actual settings surface and return to its top to save, retaining visibility and persistence assertions.
- Two failures: home entry crashed, not merely an automation disconnect. The xcresult console reports `fileNameSlotDrafts must have exactly 5 entries`; both app crash records show Kotlin unhandled-exception termination. Starter creation supplied only the occupied three filename positions. Pad unused positions with null without changing occupied entries or naming rules. Cover every starter through the real preview pipeline and retain Android's three occupied-slot expectation.
- One focus failure: the observed brightness label was an accessibility `Other`, not `StaticText`. Query any matching element and retain changed-value/lock assertions.
- One live-placement failure: a fixed 2:3 test preview disagreed with the persisted default 3:4 configured crop. Match fixture geometry to the selected ratio; retain the mismatched-raster rejection at the raw placed-image boundary, including counter/pending/asset invariants. Production aspect validation is unchanged.

Home/media tests also handle first-launch onboarding through its actual skip action; the dedicated onboarding tests remain intact.

## Verification boundary

Local diff and scoped static/numeric checks passed. The attempted shared JVM regression could not download Gradle because network access was unavailable. No local Kotlin, Swift, simulator or device execution is claimed. The next CI run must confirm this repair; fixing setup failures only makes downstream gallery assertions reachable and does not prove them successful.

No tests were skipped, capture safety guard relaxed, user data deleted, branch merged or app deployed.

## Follow-up: `22f271a9`

[UI run 44169119375](https://github.com/Dudozi-workshop/DZlog/actions/runs/44169119375) passed 17/25 selected tests. Home startup and live-placement failures cleared. Eight remain: seven capture-settings interactions and one deliberate exposure-control absence assertion. Shared core and iPhone compile passed. [Android run 44169119410](https://github.com/Dudozi-workshop/DZlog/actions/runs/44169119410) ran 607 tests with two failures.

- The settings helper's one-second predicate deadline can discard its first synchronous accessibility evaluation. Scrolled Compose label frames also disagree with neighboring control frames. Use a fresh screenshot's unique exact rendered label (Vision, Korean/English) plus an enabled semantic button for the actual touch. Preserve bounded scrolling and all resulting state/persistence assertions. Missing or ambiguous rendered labels fail rather than guessing coordinates.
- A negative visibility assertion called the positive `button()` helper, which itself requires existence. Query absence directly while also asserting that the interaction-unlock control is present. Preserve subsequent exposure-lock and brightness restoration assertions.
- Android policy fixtures appended occupied slots after the new null padding, making six/seven-position test arrays. Fill the five-position fixture instead; preserve authorization and no-mutation assertions. Add a default-rollout regression proving five occupied slots, unlimited template actions and no purchase UI for absent/Free/Pro entitlement. Production `ALL_UNLOCKED` and entitlement policies are unchanged.

Local focused Android execution again stopped at the Gradle download (`Network is unreachable`). Static checks do not replace the next native/JVM CI execution.

## Follow-up: `33a1d0c6`

[UI run 44179890177](https://github.com/Dudozi-workshop/DZlog/actions/runs/44179890177) passed 23/25 selected tests. Android, shared core and iPhone compile passed. The remaining two failures are separate:

- Folder creation opened its dialog, but the empty text field had no accessible name. Add the existing visible label as the field's content description; keep the test's label-based input and add empty-name disabled / valid-name enabled save assertions.
- Recent-gallery expected two photos but showed fifteen, because earlier tests shared the same app-private PhotoKit index. Give each XCTest invocation a UUID environment namespace, consumed only by the explicit configured-capture fixture. Inject its defaults suite into the real repository, capture/undo journals, pending checks and recovery; reuse the same suite through relaunch and camera return. Production defaults remain standard. Do not filter gallery results, weaken exact counts, erase the production index or delete Photos assets during setup.

Native suite-isolation regression coverage accompanies the dependency injection. Simulator execution must still validate the complete UI path; local static checks alone cannot establish a pass.

## Follow-up: `9c7bb8bc`

[UI run 44199737306](https://github.com/Dudozi-workshop/DZlog/actions/runs/44199737306) was cancelled, not successful. Its log contains 15 passed tests, one failed test and a seventeenth interrupted test; eight selected tests were not reached. Android unified CI, shared core and iPhone device compilation passed at this head.

- The exact two-photo gallery/grid/viewer test now passes. The folder-management test passed its named-field lookup and reached native keyboard input, then failed while waiting for Apple's first-use typing introduction to disappear. The teardown accessibility tree already shows the ordinary letter keyboard and no introduction. This is a different failure from the previous missing folder label.
- The job started at 23:29:03 UTC, reached test execution at 23:51, and was cancelled at 00:09:15, consistent with its 40-minute job limit. No newer PR head or PR-triggered retry was present at inspection. Allow 60 minutes for cold native compilation plus the unchanged selected tests; do not skip tests or reinterpret a cancellation as a pass.

The native keyboard helper repair and larger finite job budget require a new CI result. Linux static inspection cannot validate native keyboard interaction. No merge, release, production data reset, or new UX is included.

Keyboard setup now observes a fresh zero match count for the introduction together with a visible, hittable first input key, within a bounded transition check. It retains the completed final accessibility observation rather than discarding it if a synchronous query outlasts the waiter's deadline. Actual key touches, valid-name enabled-state and saved folder assertions are unchanged. Local verification: diff check, workflow YAML parse and all 15 CI-reuse Node tests passed; native Swift/XCTest was not run locally.

## Follow-up: `a47da878`

The preceding `6d95002c` media run `44206861073` passed all 25 selected tests, including both gallery tests. After camera/gallery parity changes, run `44214470955` at `a47da878` passed 21/25; Android, shared core and device compilation passed.

The four failures are stale test targets, verified against source and failure accessibility trees. The gallery filter is now an actual button named `좋아요만 보기`, not `즐겨찾기`. Camera chrome intentionally removed the redundant `카메라` title; the denial screen still exposes `촬영 도구`, the denial message, zero capture count and disabled shutter. Update the exact filter target and use the camera tool control as the arrival marker. Retain all filter count, favorites persistence, selection clearing, disabled capture and home-return assertions. Do not revert the newer UI or loosen these behavioral checks.

Local diff checks and 15 CI-reuse Node tests passed. Native XCTest remains unexecuted locally; the next CI run must validate this test-only alignment. No merge or release.
