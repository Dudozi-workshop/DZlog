package com.dudoziworkshop.dzlog.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsIgnoringVisibility
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.dudoziworkshop.dzlog.ui.theme.DDZColor

@Composable
fun Modifier.dzScreen(): Modifier =
    this
        .fillMaxSize()
        .background(DDZColor.Background)
        .windowInsetsPadding(
            WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
        )
        .windowInsetsPadding(
            WindowInsets.navigationBarsIgnoringVisibility.only(WindowInsetsSides.Bottom)
        )


@Composable
fun Modifier.dzScaffoldContent(): Modifier =
    this
        .fillMaxSize()
        .background(DDZColor.Background)
        .windowInsetsPadding(
            WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)
        )
