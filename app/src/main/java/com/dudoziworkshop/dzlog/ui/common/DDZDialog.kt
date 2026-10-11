package com.dudoziworkshop.dzlog.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.unit.dp
import com.dudoziworkshop.dzlog.ui.theme.LocalDDZColor
import com.dudoziworkshop.dzlog.ui.theme.LocalDDZTypography
import com.dudoziworkshop.dzlog.ui.theme.LocalDDZSpacing

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

    AlertDialog(
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
) {
    val colors = LocalDDZColor.current
    val typography = LocalDDZTypography.current

    Dialog(onDismissRequest = onDismiss) {
        Surface(
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
