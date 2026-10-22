package com.dudoziworkshop.dzlog.feature.capture.policy

import com.dudoziworkshop.dzlog.data.datastore.CaptureSoundChoice
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CaptureSoundOutputPolicyTest {
    @Test fun mutedOptionalShutterNeverPlays() {
        CaptureSoundChoice.entries.forEach {
            assertEquals(CaptureSoundOutput.OFF, captureSoundOutput(false, it))
        }
    }

    @Test fun everyOptionalShutterIncludingDefaultUsesBundledSample() {
        CaptureSoundChoice.entries.forEach {
            assertEquals(CaptureSoundOutput.CUSTOM, captureSoundOutput(true, it))
        }
    }

    @Test fun mandatoryPlatformShutterCannotBeReplacedOrMuted() {
        CaptureSoundChoice.entries.forEach { choice ->
            listOf(true, false).forEach { enabled ->
                assertEquals(
                    CaptureSoundOutput.SYSTEM,
                    captureSoundOutput(enabled, choice, platformShutterRequired = true),
                )
            }
        }
    }

    @Test fun playbackLevelsAreBoundedAndSignatureIsTrimmed() {
        CaptureSoundChoice.entries.forEach { choice ->
            assertTrue(choice.playbackGain > 0f)
            assertTrue(choice.playbackGain <= 1f)
        }
        assertTrue(CaptureSoundChoice.PPUPPYU.playbackGain < CaptureSoundChoice.DIGITAL_DSLR.playbackGain)
        assertEquals(0.50f, CaptureSoundChoice.CLASSIC_WERRA.playbackGain, 0.001f)
        assertEquals(1.00f, CaptureSoundChoice.SYSTEM.playbackGain, 0.001f)
    }
}
