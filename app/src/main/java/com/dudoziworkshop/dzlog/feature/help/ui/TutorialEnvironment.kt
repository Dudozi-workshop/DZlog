package com.dudoziworkshop.dzlog.feature.help.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.unit.dp
import com.dudoziworkshop.dzlog.feature.help.model.TutorialLocation
import com.dudoziworkshop.dzlog.ui.theme.DDZColor

internal val LocalTutorialLocation = staticCompositionLocalOf<TutorialLocation?> { null }
internal val LocalTutorialTarget = staticCompositionLocalOf<String?> { null }
internal val LocalTutorialReadOnly = staticCompositionLocalOf { false }

/** Inactive during ordinary app use. Target selection only scrolls/paints the real UI. */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
internal fun Modifier.tutorialTarget(id: String): Modifier = composed {
    val selected = LocalTutorialTarget.current == id
    val requester = remember { BringIntoViewRequester() }
    LaunchedEffect(selected) { if (selected) requester.bringIntoView() }
    if (selected) this.bringIntoViewRequester(requester)
        .border(3.dp, DDZColor.PrimaryDark, RoundedCornerShape(12.dp))
    else this
}
