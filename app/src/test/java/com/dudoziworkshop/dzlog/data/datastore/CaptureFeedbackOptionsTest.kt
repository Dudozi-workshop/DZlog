package com.dudoziworkshop.dzlog.data.datastore

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CaptureFeedbackOptionsTest {
    @Test fun soundChoiceIdsRemainStable() {
        assertEquals(listOf(0, 1, 2, 3, 4), CaptureSoundChoice.entries.map { it.storedId })
        assertEquals(CaptureSoundChoice.SYSTEM, CaptureSoundChoice.fromStored(-1))
        assertEquals(CaptureSoundChoice.SYSTEM, CaptureSoundChoice.fromStored(999))
        assertEquals(CaptureSoundChoice.DIGITAL_DSLR, CaptureSoundChoice.fromStored(3))
    }

    @Test fun signatureIsNotSelectableBeforeOriginalIsRecovered() {
        assertFalse(CaptureSoundChoice.PPUPPYU.available)
        CaptureSoundChoice.entries.filter { it != CaptureSoundChoice.PPUPPYU }.forEach {
            assertTrue(it.available)
        }
    }

    @Test fun vibrationStrengthIdsAndFallbackRemainStable() {
        assertEquals(listOf(0, 1, 2, 3), CaptureVibrationStrength.entries.map { it.storedId })
        assertEquals(CaptureVibrationStrength.MEDIUM, CaptureVibrationStrength.fromStored(-1))
        assertEquals(CaptureVibrationStrength.MEDIUM, CaptureVibrationStrength.fromStored(999))
        assertEquals(CaptureVibrationStrength.OFF, CaptureVibrationStrength.fromStored(0))
    }

    @Test fun existingAppSettingsRetainOriginalDefaultFeedback() {
        assertTrue(AppSettings.Default.captureSoundEnabled)
        assertTrue(AppSettings.Default.captureHapticEnabled)
        assertEquals(CaptureSoundChoice.SYSTEM, AppSettings.Default.captureSoundChoice)
        assertEquals(CaptureVibrationStrength.MEDIUM, AppSettings.Default.captureVibrationStrength)
        assertEquals(70, AppSettings.Default.captureSoundVolume)
    }
}
