package com.dudoziworkshop.dzlog.ui.common

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import com.dudoziworkshop.dzlog.ui.theme.LocalDDZColor
import com.dudoziworkshop.dzlog.ui.theme.LocalDDZTypography

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DDZTopBar(
    title: String,
    onBack: (() -> Unit)? = null,
    actions: @Composable () -> Unit = {},
) {
    val colors = LocalDDZColor.current
    val typography = LocalDDZTypography.current

    CenterAlignedTopAppBar(
        title = {
            Text(
                text = title,
                style = typography.ScreenTitle,
                color = colors.TextPrimary,
            )
        },
        navigationIcon = {
            if (onBack != null) {
                DDZTopBarIconButton(
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "뒤로가기",
                    onClick = onBack,
                )
            }
        },
        actions = { actions() },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = colors.Background,
            navigationIconContentColor = colors.TextPrimary,
            titleContentColor = colors.TextPrimary,
            actionIconContentColor = colors.TextPrimary,
        ),
    )
}

@Composable
fun DDZTopBarIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    val colors = LocalDDZColor.current

    IconButton(
        onClick = onClick,
        enabled = enabled,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (enabled) colors.TextPrimary else colors.TextDisabled,
        )
    }
}
