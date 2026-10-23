package com.dudoziworkshop.dzlog.feature.capture.policy

import com.dudoziworkshop.dzlog.data.datastore.CaptureSoundChoice
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CaptureSoundOutputPolicyTest {
    @Test fun soundOffDoesNotRequestAppAudioWithoutRegionalRequirement() {
        CaptureSoundChoice.entries.forEach { choice ->
            assertEquals(
                CaptureSoundOutput.OFF,
                captureSoundOutput(false, choice, platformShutterRequired = false),
            )
        }
    }

    @Test fun soundOnPlaysOnlyTheSelectedBundledSampleWhenAllowed() {
        CaptureSoundChoice.entries.forEach { choice ->
            assertEquals(
                CaptureSoundOutput.CUSTOM,
                captureSoundOutput(true, choice, platformShutterRequired = false),
            )
        }
    }

    @Test fun requiredPlatformSoundIsTheOnlyPlaybackPath() {
        CaptureSoundChoice.entries.forEach { choice ->
            listOf(false, true).forEach { enabled ->
                assertEquals(
                    CaptureSoundOutput.SYSTEM,
                    captureSoundOutput(enabled, choice, platformShutterRequired = true),
                )
            }
        }
    }

    @Test fun soundGainConfigurationIsStableAndBounded() {
        CaptureSoundChoice.entries.forEach { choice ->
            assertTrue(choice.playbackGain > 0f)
            assertTrue(choice.playbackGain <= 1f)
        }
        assertTrue(CaptureSoundChoice.PPUPPYU.playbackGain < CaptureSoundChoice.DIGITAL_DSLR.playbackGain)
        assertEquals(0.50f, CaptureSoundChoice.CLASSIC_WERRA.playbackGain, 0.001f)
        assertEquals(1.00f, CaptureSoundChoice.SYSTEM.playbackGain, 0.001f)
    }
}
