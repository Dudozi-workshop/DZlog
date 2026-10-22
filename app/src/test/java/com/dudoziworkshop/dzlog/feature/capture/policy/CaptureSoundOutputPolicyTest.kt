package com.dudoziworkshop.dzlog.feature.capture.policy

import com.dudoziworkshop.dzlog.data.datastore.CaptureSoundChoice
import org.junit.Assert.assertEquals
import org.junit.Test

class CaptureSoundOutputPolicyTest {
    @Test fun disabledSoundsNeverProduceAppShutter() {
        CaptureSoundChoice.entries.forEach {
            assertEquals(CaptureSoundOutput.OFF, captureSoundOutput(false, it))
        }
    }

    @Test fun onlyExplicitSystemSelectionUsesPlatformClick() {
        assertEquals(CaptureSoundOutput.SYSTEM, captureSoundOutput(true, CaptureSoundChoice.SYSTEM))
        CaptureSoundChoice.entries.filter { it != CaptureSoundChoice.SYSTEM }.forEach {
            assertEquals(CaptureSoundOutput.CUSTOM, captureSoundOutput(true, it))
        }
    }
}
