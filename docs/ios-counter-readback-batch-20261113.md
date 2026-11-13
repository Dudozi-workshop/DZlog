# iOS automatic counter readback batch

Status: implemented; remote native verification pending.

## Implemented contract

- The iOS table editor now initializes and explicitly synchronizes its next number from indexed DZlog photos that are still present in PhotoKit.
- The camera refreshes an automatic number on entry and verifies it again immediately before rendering and saving a capture.
- Readback follows the existing Android path and filename-prefix contract, including path-scope wildcarding, rotating-text prefix alternatives and padding-agnostic parsing.
- The saved maximum advances to `max + 1`; a delayed same-stream refresh never lowers the current automatic number.
- A number explicitly entered by the user remains manual and is not replaced by automatic readback.
- Missing/deleted PhotoKit assets do not contribute. Original/result duplicates collapse naturally because the policy uses a set of parsed numbers.
- Counter exhaustion fails closed instead of wrapping to a negative number.

## Transaction boundary

- Automatic synchronization is persisted before capture planning.
- The final table patch, filename, counter and media save plan are rebuilt from one timestamp and the synchronized document.
- The existing PhotoKit journal still starts only after permission, readback and rendering succeed.
- A settings change observed while PhotoKit readback is in flight rejects the stale update.

## Verification

- Added common policy tests for physical paths, wildcard scope, rotating prefixes, padding changes, rollback prevention and integer exhaustion.
- `git diff --check` passed.
- Local Gradle execution was unavailable because the wrapper distribution is not cached and this executor cannot reach the Gradle distribution host.
- Request shared/JVM policy tests, shared iOS tests, iPhone ARM64 compile and the existing iOS UI regression after publishing. Continue migration work without waiting for those runs.

## Device QA remaining

- Confirm first camera entry after reinstall and after Photos permission changes.
- Confirm automatic max+1 after deleting, moving and favoriting indexed photos.
- Confirm manual lower and higher numbers remain unchanged through camera entry.
- Confirm BOTH mode reads the result-path stream and does not double-advance from its original companion.
