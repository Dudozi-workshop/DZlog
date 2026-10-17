import importlib.util
from pathlib import Path
import unittest

spec = importlib.util.spec_from_file_location(
    "selector", Path(__file__).with_name("dzlog_ci_test_selector.py")
)
module = importlib.util.module_from_spec(spec)
import sys
sys.modules[spec.name] = module
spec.loader.exec_module(module)

ROOT = "app/src/test/java/"
QUICK = ROOT + "com/dudoziworkshop/dzlog/feature/table/policy/QuickCellValuePolicyTest.kt"
BACKUP = ROOT + "com/dudoziworkshop/dzlog/data/backup/LocalTemplateBackupRepositorySafetyTest.kt"
DELETE = ROOT + "com/dudoziworkshop/dzlog/data/mediastore/GalleryPhotoDeletionTest.kt"
PENDING = ROOT + "com/dudoziworkshop/dzlog/data/repository/CapturePendingStoreTest.kt"
COUNTER = ROOT + "com/dudoziworkshop/dzlog/domain/counter/CounterScopeResolverTest.kt"
UNDO = ROOT + "com/dudoziworkshop/dzlog/feature/capture/policy/UndoCapturePolicyTest.kt"
EXTRA = ROOT + "com/dudoziworkshop/dzlog/ui/camera/controls/CameraTableResizePolicyTest.kt"
TESTS = [QUICK, BACKUP, DELETE, PENDING, COUNTER, UNDO, EXTRA]


class SelectorSafetyTest(unittest.TestCase):
    def test_missing_diff_runs_full(self):
        self.assertEqual("full", module.select([], TESTS).mode)

    def test_shared_capture_changes_run_full(self):
        self.assertEqual("full", module.select([
            "app/src/main/java/com/dudoziworkshop/dzlog/feature/capture/Save.kt"
        ], TESTS).mode)

    def test_build_changes_run_full(self):
        self.assertEqual("full", module.select(["app/build.gradle.kts"], TESTS).mode)

    def test_unknown_changes_run_full(self):
        self.assertEqual("full", module.select(["another-tool/something.py"], TESTS).mode)

    def test_narrow_camera_change_includes_safety_tests(self):
        result = module.select([
            "app/src/main/java/com/dudoziworkshop/dzlog/ui/camera/controls/CameraTableResizePolicy.kt"
        ], TESTS)
        self.assertEqual("focused", result.mode)
        self.assertIn("com.dudoziworkshop.dzlog.ui.camera.controls.CameraTableResizePolicyTest", result.classes)
        self.assertEqual(7, len(result.classes))

    def test_unknown_test_file_falls_back(self):
        self.assertEqual("full", module.select([ROOT+"unknown/NonExistentTest.kt"], TESTS).mode)

    def test_documentation_is_noop(self):
        self.assertEqual("none", module.select(["docs/readme.md"], TESTS).mode)

    def test_ci_changes_run_safety_canaries(self):
        self.assertEqual("focused", module.select([".github/workflows/ci.yml"], TESTS).mode)


if __name__ == "__main__":
    unittest.main()
