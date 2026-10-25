package com.dudoziworkshop.dzlog.feature.capture.policy

import com.dudoziworkshop.dzlog.data.datastore.CaptureSoundChoice
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CaptureSoundOutputPolicyTest {
    @Test fun soundOffDoesNotRequestAppAudioWithEitherRegionalFlag() {
        CaptureSoundChoice.entries.forEach { choice ->
            listOf(false, true).forEach { flag ->
                assertEquals(
                    CaptureSoundOutput.OFF,
                    captureSoundOutput(false, choice, platformShutterRequired = flag),
                )
            }
        }
    }

    @Test fun soundOnSelectsOneBundledSampleWithEitherRegionalFlag() {
        CaptureSoundChoice.entries.forEach { choice ->
            listOf(false, true).forEach { flag ->
                assertEquals(
                    CaptureSoundOutput.CUSTOM,
                    captureSoundOutput(true, choice, platformShutterRequired = flag),
                )
            }
        }
    }

    @Test fun requiredFlagDoesNotForceTheSystemClickInTheApp() {
        CaptureSoundChoice.entries.forEach { choice ->
            assertEquals(
                CaptureSoundOutput.OFF,
                captureSoundOutput(false, choice, platformShutterRequired = true),
            )
            assertEquals(
                CaptureSoundOutput.CUSTOM,
                captureSoundOutput(true, choice, platformShutterRequired = true),
            )
        }
    }

    @Test fun requiredTrueIsNotConflatedWithQueryFailure() {
        val required = classifyShutterRequirement(Result.success(true))
        val failure = classifyShutterRequirement(Result.failure(IllegalStateException("unavailable")))
        assertEquals(ShutterRequirementStatus.REQUIRED, required.status)
        assertEquals(ShutterRequirementStatus.QUERY_FAILED, failure.status)
        assertEquals("IllegalStateException", failure.errorType)
        assertTrue(required.requiresSystemShutter)
        assertTrue(failure.requiresSystemShutter)
    }

    @Test fun nonRequiredPolicyCanUseCustomSelection() {
        val observation = classifyShutterRequirement(Result.success(false))
        assertEquals(ShutterRequirementStatus.NOT_REQUIRED, observation.status)
        assertEquals(false, observation.requiresSystemShutter)
        CaptureSoundChoice.entries.forEach { choice ->
            assertEquals(
                CaptureSoundOutput.CUSTOM,
                captureSoundOutput(true, choice, platformShutterRequired = observation.requiresSystemShutter),
            )
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
