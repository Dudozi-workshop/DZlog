package com.dudoziworkshop.dzlog.feature.help.model

import com.dudoziworkshop.dzlog.data.template.newBlankTableTemplateState
import com.dudoziworkshop.dzlog.feature.capture.policy.CapturePendingProjection
import org.junit.Assert.*
import org.junit.Test

class TutorialCaptureIsolationTest {
    @Test fun openingAndClosingGuideCannotClearTheActiveCapturesCommittedProgress() {
        val base = newBlankTableTemplateState(rows = 1, cols = 1)
        val committed = base.copy(cells = base.cells.map { it.copy(rawText = "committed value") })
        val live = CapturePendingProjection()
        live.bindTemplate("active-template")
        live.recordCommittedTemplate(committed)
        val guide = tutorialCaptureProjection(live, readOnly = true)
        guide.bindTemplate(null)
        guide.recordCommittedTemplate(base)
        assertEquals(committed, live.committedBase(base))
        assertSame(live, tutorialCaptureProjection(live, readOnly = false))
    }
}
