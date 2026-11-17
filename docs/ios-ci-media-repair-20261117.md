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
