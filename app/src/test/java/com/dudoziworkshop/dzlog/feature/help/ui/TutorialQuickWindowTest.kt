package com.dudoziworkshop.dzlog.feature.help.ui

import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import com.dudoziworkshop.dzlog.domain.model.*
import com.dudoziworkshop.dzlog.feature.help.model.TutorialLocation
import com.dudoziworkshop.dzlog.feature.table.policy.QuickEditSelectionMemory
import com.dudoziworkshop.dzlog.feature.table.render.TableRenderStyle
import com.dudoziworkshop.dzlog.ui.camera.controls.CameraQuickValueSheet
import com.dudoziworkshop.dzlog.ui.theme.DDZTheme
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.util.Date
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TutorialQuickWindowTest {
    @Test fun guideCanMountTheRealQuickEditorAndCloseWithoutCommitting() {
        val controller = Robolectric.buildActivity(ComponentActivity::class.java).setup().visible()
        var composed = false
        var committed = false
        controller.get().setContent {
            DDZTheme {
                CompositionLocalProvider(LocalTutorialReadOnly provides true,
                    LocalTutorialLocation provides TutorialLocation("camera", "table", "quick")) {
                    CameraQuickValueSheet(template = guideQuickTemplate(), templateName = "기능 안내 예시",
                        counterDigits = 4, counterSeed = 1, counterRequest = null,
                        includePathInCounterScope = false, includeFilenameInCounterScope = false,
                        phraseCursor = 0, now = Date(0), captureAspect = CaptureAspect.R3_4,
                        style = TableRenderStyle(), tableWidthRatio = 40, tableHeightRatio = 20,
                        saveMode = SaveMode.BOTH, templates = emptyList(), activeTemplateId = null,
                        selectionMemory = QuickEditSelectionMemory(), onTemplateSelect = { _, _ -> false },
                        onDismiss = {}, onCommit = { committed = true })
                    SideEffect { composed = true }
                }
            }
        }
        shadowOf(Looper.getMainLooper()).idleFor(2, TimeUnit.SECONDS)
        assertTrue(composed)
        controller.pause().stop().destroy()
        assertFalse(committed)
    }
}
