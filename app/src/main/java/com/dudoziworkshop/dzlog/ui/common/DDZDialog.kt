package com.dudoziworkshop.dzlog.ui.common

import com.dudoziworkshop.dzlog.feature.help.ui.LocalTutorialReadOnly
import com.dudoziworkshop.dzlog.feature.help.ui.tutorialTarget
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog as MaterialAlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.window.DialogProperties
import com.dudoziworkshop.dzlog.ui.theme.LocalDDZColor
import com.dudoziworkshop.dzlog.ui.theme.LocalDDZTypography
import com.dudoziworkshop.dzlog.ui.theme.LocalDDZSpacing
import com.dudoziworkshop.dzlog.ui.theme.DDZLayout

/** Shared surface and Material slots; callbacks and permission flows stay with callers. */
@Composable
fun DDZAlertDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: (@Composable () -> Unit)? = null,
    icon: (@Composable () -> Unit)? = null,
    title: (@Composable () -> Unit)? = null,
    text: (@Composable () -> Unit)? = null,
    shape: Shape = MaterialTheme.shapes.large,
    containerColor: Color = LocalDDZColor.current.Surface,
    iconContentColor: Color = LocalDDZColor.current.TextSecondary,
    titleContentColor: Color = LocalDDZColor.current.TextPrimary,
    textContentColor: Color = LocalDDZColor.current.TextSecondary,
    tonalElevation: Dp = 0.dp,
    properties: DialogProperties = DialogProperties(),
) {
    if (LocalTutorialReadOnly.current) {
        androidx.compose.foundation.layout.Box(Modifier.fillMaxSize().padding(top = com.dudoziworkshop.dzlog.feature.help.ui.LocalTutorialCardTopSpace.current).padding(horizontal = 24.dp),
            contentAlignment = androidx.compose.ui.Alignment.Center) {
            Surface(modifier.tutorialTarget("guide.dialog"), shape = shape, color = containerColor, tonalElevation = tonalElevation) {
                Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    icon?.invoke()
                    androidx.compose.runtime.CompositionLocalProvider(androidx.compose.material3.LocalContentColor provides titleContentColor) {
                        androidx.compose.material3.ProvideTextStyle(MaterialTheme.typography.headlineSmall) { title?.invoke() }
                    }
                    androidx.compose.runtime.CompositionLocalProvider(androidx.compose.material3.LocalContentColor provides textContentColor) {
                        androidx.compose.material3.ProvideTextStyle(MaterialTheme.typography.bodyMedium) { text?.invoke() }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        dismissButton?.invoke()
                        confirmButton()
                    }
                }
            }
        }
    } else MaterialAlertDialog(onDismissRequest = onDismissRequest, confirmButton = confirmButton,
        modifier = modifier, dismissButton = dismissButton, icon = icon, title = title, text = text,
        shape = shape, containerColor = containerColor, iconContentColor = iconContentColor,
        titleContentColor = titleContentColor, textContentColor = textContentColor,
        tonalElevation = tonalElevation, properties = properties)
}

@Composable
fun DDZDialogAction(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    style: DDZButtonStyle = DDZButtonStyle.Primary,
    content: @Composable RowScope.() -> Unit,
) {
    val spacing = LocalDDZSpacing.current
    DDZButtonContent(onClick = onClick, modifier = modifier, enabled = enabled, style = style,
        minHeight = DDZLayout.QuickInput.Touch, horizontalPadding = spacing.controlGap, content = content)
}

@Composable
fun DDZConfirmDialog(
    title: String,
    message: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    dismissText: String = "취소",
    destructive: Boolean = false,
) {
    val colors = LocalDDZColor.current
    val typography = LocalDDZTypography.current

    DDZAlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.Surface,
        shape = MaterialTheme.shapes.large,
        title = {
            Text(
                text = title,
                style = typography.SectionTitle,
                color = colors.TextPrimary,
            )
        },
        text = {
            Text(
                text = message,
                style = typography.Body,
                color = colors.TextSecondary,
            )
        },
        confirmButton = {
            DDZButton(
                text = confirmText,
                onClick = onConfirm,
                style = if (destructive) DDZButtonStyle.Destructive else DDZButtonStyle.Primary,
                minHeight = 40.dp,
            )
        },
        dismissButton = {
            DDZButton(
                text = dismissText,
                onClick = onDismiss,
                style = DDZButtonStyle.Secondary,
                minHeight = 40.dp,
            )
        },
    )
}

@Composable
fun DDZQuickChoiceDialog(
    onDismiss: () -> Unit,
    content: @Composable RowScope.() -> Unit,
) {
    val colors = LocalDDZColor.current

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = colors.Surface,
            shape = MaterialTheme.shapes.medium,
            shadowElevation = 4.dp,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                content()
            }
        }
    }
}

@Composable
fun DDZDialogContent(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        content()
    }
}


@Composable
fun DDZContentDialog(
    title: String,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit,
    actions: @Composable RowScope.() -> Unit,
    properties: DialogProperties = DialogProperties(),
    inlinePreview: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val colors = LocalDDZColor.current
    val typography = LocalDDZTypography.current

    val dialogContent: @Composable () -> Unit = {
        Surface(
            modifier = modifier.tutorialTarget("guide.dialog"),
            color = colors.Surface,
            shape = MaterialTheme.shapes.large,
            shadowElevation = 4.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = title,
                    style = typography.SectionTitle,
                    color = colors.TextPrimary,
                )
                content()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    actions()
                }
            }
        }
    }
    if (inlinePreview || LocalTutorialReadOnly.current) {
        androidx.compose.foundation.layout.Box(
            Modifier.fillMaxSize().padding(top = com.dudoziworkshop.dzlog.feature.help.ui.LocalTutorialCardTopSpace.current).padding(horizontal = 24.dp),
            contentAlignment = androidx.compose.ui.Alignment.Center,
        ) { dialogContent() }
    } else Dialog(onDismissRequest = onDismiss, properties = properties) { dialogContent() }

}

/**
 * Shared dirty-session decision: Cancel -> Discard -> Save in both layouts.
 * Save/discard behavior is provided by the caller, never handled by the view.
 */
@Composable
fun DDZUnsavedDecisionDialog(
    title: String,
    message: String,
    onCancel: () -> Unit,
    onDiscard: () -> Unit,
    onSave: () -> Unit,
    enabled: Boolean = true,
    saveText: String = "저장",
    discardText: String = "저장 안 함",
    verticalActions: Boolean = false,
) {
    val colors = LocalDDZColor.current
    val spacing = LocalDDZSpacing.current
    val typography = LocalDDZTypography.current
    DDZContentDialog(
        title = title,
        onDismiss = onCancel,
        content = {
            Text(message, style = typography.Body, color = colors.TextSecondary)
        },
        actions = {
            if (verticalActions) {
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(spacing.itemGap)) {
                    DDZButton(
                        text = "취소",
                        onClick = onCancel,
                        style = DDZButtonStyle.Secondary,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = enabled,
                    )
                    DDZButton(
                        text = discardText,
                        onClick = onDiscard,
                        style = DDZButtonStyle.Discard,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = enabled,
                    )
                    DDZButton(
                        text = saveText,
                        onClick = onSave,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = enabled,
                    )
                }
            } else {
                DDZButton(
                    text = "취소",
                    onClick = onCancel,
                    style = DDZButtonStyle.Secondary,
                    modifier = Modifier.weight(1f),
                    horizontalPadding = spacing.xs,
                    enabled = enabled,
                )
                DDZButton(
                    text = discardText,
                    onClick = onDiscard,
                    style = DDZButtonStyle.Discard,
                    modifier = Modifier.weight(1f),
                    horizontalPadding = spacing.xs,
                    enabled = enabled,
                )
                DDZButton(
                    text = saveText,
                    onClick = onSave,
                    modifier = Modifier.weight(1f),
                    horizontalPadding = spacing.xs,
                    enabled = enabled,
                )
            }
        },
    )
}
