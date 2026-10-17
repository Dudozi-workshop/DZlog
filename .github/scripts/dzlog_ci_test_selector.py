#!/usr/bin/env python3
"""Conservative DZlog CI test selection.

Fast checks are advisory only. The protected milestone and release pipelines
still run the entire suite; unknown or shared changes fall back to full tests.
"""
from __future__ import annotations

import argparse
from dataclasses import dataclass
from pathlib import Path
import subprocess

MAIN = "app/src/main/java/com/dudoziworkshop/dzlog/"
TEST = "app/src/test/java/"
SAFETY = {
    "com.dudoziworkshop.dzlog.data.backup.LocalTemplateBackupRepositorySafetyTest",
    "com.dudoziworkshop.dzlog.data.mediastore.GalleryPhotoDeletionTest",
    "com.dudoziworkshop.dzlog.data.repository.CapturePendingStoreTest",
    "com.dudoziworkshop.dzlog.domain.counter.CounterScopeResolverTest",
    "com.dudoziworkshop.dzlog.feature.capture.policy.UndoCapturePolicyTest",
    "com.dudoziworkshop.dzlog.feature.table.policy.QuickCellValuePolicyTest",
}
SHARED_PREFIXES = (
    MAIN + "MainActivity.kt",
    MAIN + "domain/",
    MAIN + "data/",
    MAIN + "feature/capture/",
    MAIN + "feature/counter/",
    MAIN + "ui/navigation/",
)
FULL_PREFIXES = (
    "app/build.gradle",
    "app/src/main/AndroidManifest.xml",
    "app/src/main/res/",
    "app/schemas/",
    "gradle/",
    "build.gradle",
    "settings.gradle",
    "gradle.properties",
    "gradlew",
    "billing-server/",
)


@dataclass(frozen=True)
class Selection:
    mode: str
    classes: tuple[str, ...] = ()
    reason: str = ""


def class_name(file_path: str) -> str:
    """A conventional Kotlin/Java JUnit class path; never execute this value."""
    stem = file_path.removeprefix(TEST).rsplit(".", 1)[0]
    return stem.replace("/", ".")


def select(changed: list[str], test_paths: list[str]) -> Selection:
    paths = sorted({p.replace("\\", "/") for p in changed if p.strip()})
    available = {class_name(p) for p in test_paths if p.startswith(TEST)
                 and p.endswith(("Test.kt", "Test.java"))}
    if not paths:
        return Selection("full", reason="Missing change information: fail closed")

    if any(p.startswith(FULL_PREFIXES) or p.startswith(SHARED_PREFIXES) for p in paths):
        return Selection("full", reason="Shared implementation, build, or data change")

    affected = set()
    changed_app = False
    ci_only = False

    for p in paths:
        if p.startswith(TEST):
            changed_app = True
            ci_only = False
            target = class_name(p)
            if target not in available:
                return Selection("full", reason="Unknown test path")
            affected.add(target)
        elif p.startswith(MAIN):
            changed_app = True
            ci_only = False
            relative = p.removeprefix(MAIN)
            parts = relative.split("/")
            if len(parts) < 3:
                return Selection("full", reason="Broad implementation change")
            # Start with the nearest directory and widen at most once.
            for depth in (len(parts) - 1, len(parts) - 2):
                if depth < 2:
                    continue
                prefix = TEST + "com/dudoziworkshop/dzlog/" + "/".join(parts[:depth]) + "/"
                matches = {class_name(t) for t in test_paths if t.startswith(prefix)
                           and t.endswith(("Test.kt", "Test.java"))}
                if matches:
                    affected.update(matches)
                    break
            else:
                return Selection("full", reason="No directly related unit tests")
        elif p.startswith("app/"):
            return Selection("full", reason="Other Android source or resources changed")
        elif p.startswith(".github/"):
            # CI config updates verify the policy and run the safety canaries.
            ci_only = True
        elif p.startswith(("docs/", "README", ".gitignore")):
            continue
        else:
            return Selection("full", reason="Unmapped repository change")

    if not changed_app and not ci_only:
        return Selection("none", reason="Documentation-only changes")
    affected.update(SAFETY)
    if not affected.issubset(available):
        return Selection("full", reason="Required safety test missing")
    if len(affected) > 35:
        return Selection("full", reason="Wide change: avoid fragmented test selection")
    return Selection("focused", tuple(sorted(affected)), reason="Localized change and safety canaries")


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--base", required=True, help="git revision used for change detection")
    parser.add_argument("--head", default="HEAD")
    parser.add_argument("--test-root", default="app/src/test/java")
    args = parser.parse_args()

    changed = subprocess.check_output(
        ["git", "diff", "--name-only", args.base, args.head, "--"], text=True
    ).splitlines()
    test_paths = [str(f) for f in Path(args.test_root).rglob("*Test.*")
                  if f.suffix in {".kt", ".java"}]
    selection = select(changed, test_paths)
    print("MODE=" + selection.mode)
    print("REASON=" + selection.reason)
    for test in selection.classes:
        print("TEST=" + test)


if __name__ == "__main__":
    main()
