package com.dudoziworkshop.dzlog.feature.help.ui

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.dudoziworkshop.dzlog.feature.help.model.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TutorialProgressStoreTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    @Before fun resetGuideOnly() {
        context.getSharedPreferences("dzlog_tutorial_v2", Context.MODE_PRIVATE).edit().clear().commit()
    }
    @Test fun closingMidSessionResumesWithoutCompletingOrChangingOtherSessions() {
        val store = TutorialProgressStore(context)
        assertEquals("미시작", store.status("camera", "tools"))
        store.save(TutorialLocation("camera", "tools", "grid"))
        val reopened = TutorialProgressStore(context)
        assertEquals("grid", reopened.page("camera", "tools"))
        assertEquals("보는 중", reopened.status("camera", "tools"))
        assertEquals("미시작", reopened.status("camera", "capture"))
        reopened.complete(TutorialLocation("camera", "tools", "assist"))
        reopened.save(TutorialLocation("camera", "tools", "menu"))
        assertEquals("완료", store.status("camera", "tools"))
    }
}
