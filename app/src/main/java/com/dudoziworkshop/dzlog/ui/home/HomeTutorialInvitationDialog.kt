package com.dudoziworkshop.dzlog.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.dudoziworkshop.dzlog.ui.common.DDZButton
import com.dudoziworkshop.dzlog.ui.common.DDZButtonStyle
import com.dudoziworkshop.dzlog.ui.theme.DDZColor

/**
 * First-home invitation only. Persistence and tutorial routing remain owned by
 * AppNavigation; this composable does not request or modify permissions/data.
 */
@Composable
internal fun HomeTutorialInvitationDialog(
    onStart: () -> Unit,
    onLater: () -> Unit,
) {
    Dialog(
        onDismissRequest = onLater,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
        ),
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = DDZColor.Surface,
            shadowElevation = 12.dp,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(
                    horizontal = 24.dp, vertical = 26.dp,
                ),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Box(
                    modifier = Modifier.size(58.dp)
                        .background(DDZColor.SelectedSoft, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Default.MenuBook,
                        contentDescription = null,
                        tint = DDZColor.SelectedDark,
                        modifier = Modifier.size(28.dp),
                    )
                }
                Text(
                    "DZlog 사용법을\n알아볼까요?",
                    fontSize = 18.sp,
                    lineHeight = 25.sp,
                    fontWeight = FontWeight.Bold,
                    color = DDZColor.TextPrimary,
                    textAlign = TextAlign.Center,
                )
                Text(
                    "카메라·앨범·기록표·저장 설정까지\n" +
                        "실제 화면에서 하나씩 살펴보세요.",
                    fontSize = 13.sp,
                    lineHeight = 20.sp,
                    color = DDZColor.TextSecondary,
                    textAlign = TextAlign.Center,
                )
                DDZButton(
                    text = "사용법 살펴보기",
                    onClick = onStart,
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                    minHeight = 52.dp,
                    shape = RoundedCornerShape(15.dp),
                )
                DDZButton(
                    text = "나중에",
                    onClick = onLater,
                    modifier = Modifier.fillMaxWidth(),
                    style = DDZButtonStyle.Text,
                    minHeight = 40.dp,
                )
            }
        }
    }
}
