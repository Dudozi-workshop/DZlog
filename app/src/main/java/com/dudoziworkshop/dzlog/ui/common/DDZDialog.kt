package com.dudoziworkshop.dzlog.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.unit.dp
import com.dudoziworkshop.dzlog.ui.theme.LocalDDZColor
import com.dudoziworkshop.dzlog.ui.theme.LocalDDZTypography

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
        shape = RoundedCornerShape(20.dp),
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
                style = DDZButtonStyle.Text,
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
            shape = RoundedCornerShape(16.dp),
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
            shape = RoundedCornerShape(20.dp),
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
