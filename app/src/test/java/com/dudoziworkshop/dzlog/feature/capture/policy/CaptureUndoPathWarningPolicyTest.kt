package com.dudoziworkshop.dzlog.feature.capture.policy

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CaptureUndoPathWarningPolicyTest {
    @Test fun sameFolderDoesNotWarn() {
        assertFalse(CaptureUndoPathWarningPolicy.requiresConfirmation(
            "Pictures/DZlog/Experiment A/", "Pictures/DZlog/Experiment A/"
        ))
    }

    @Test fun differentFolderWarns() {
        assertTrue(CaptureUndoPathWarningPolicy.requiresConfirmation(
            "Pictures/DZlog/Experiment A/", "Pictures/DZlog/Experiment B/"
        ))
    }

    @Test fun originalFolderIsSameLogicalFolder() {
        assertFalse(CaptureUndoPathWarningPolicy.requiresConfirmation(
            "Pictures/DZlog/Experiment A/original/", "Pictures/DZlog/Experiment A/"
        ))
    }

    @Test fun unknownCapturePathKeepsExistingUndoBehavior() {
        assertFalse(CaptureUndoPathWarningPolicy.requiresConfirmation(null, "Pictures/DZlog/Experiment B/"))
    }

    @Test fun rotatingPhraseInFilenameDoesNotWarn() {
        assertFalse(CaptureUndoPathWarningPolicy.requiresConfirmation(
            "Pictures/DZlog/Experiment A/", "Pictures/DZlog/Experiment A/"
        ))
    }

    @Test fun rotatingPhraseInPathTriggersSameWarning() {
        assertTrue(CaptureUndoPathWarningPolicy.requiresConfirmation(
            "Pictures/DZlog/Treatment A/", "Pictures/DZlog/Treatment B/"
        ))
    }

    @Test fun dateBasedFolderChangeWarns() {
        assertTrue(CaptureUndoPathWarningPolicy.requiresConfirmation(
            "Pictures/DZlog/20261016/", "Pictures/DZlog/20261017/"
        ))
    }
}
