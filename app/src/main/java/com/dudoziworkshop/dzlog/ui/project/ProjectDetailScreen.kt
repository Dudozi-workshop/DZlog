package com.dudoziworkshop.dzlog.ui.project

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.dudoziworkshop.dzlog.domain.project.CaptureProject
import com.dudoziworkshop.dzlog.ui.common.DDZButton
import com.dudoziworkshop.dzlog.ui.common.DDZButtonStyle
import com.dudoziworkshop.dzlog.ui.theme.DDZColor
import com.dudoziworkshop.dzlog.ui.theme.DDZTypography

/** Browsing never changes the capture target. Only 'Continue' activates it. */
@Composable
internal fun ProjectDetailScreen(
    project: CaptureProject,
    summary: ProjectCaptureSummary,
    selected: Boolean,
    onBack: () -> Unit,
    onContinue: () -> Unit,
    onShowAll: () -> Unit,
    onRename: (String) -> Unit,
    onComplete: () -> Unit,
    onReopen: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    var showRename by remember { mutableStateOf(false) }
    var showComplete by remember { mutableStateOf(false) }
    Column(
        Modifier.fillMaxSize().background(DDZColor.Background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "프로젝트 목록")
            }
            Text("프로젝트 상세", style = DDZTypography.ScreenTitle,
                color = DDZColor.TextPrimary, modifier = Modifier.weight(1f))
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "프로젝트 관리")
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(text = { Text("이름 변경") }, onClick = {
                        menuOpen = false
                        showRename = true
                    })
                    if (project.isActive) DropdownMenuItem(
                        text = { Text("프로젝트 완료") },
                        onClick = { menuOpen = false; showComplete = true },
                    ) else DropdownMenuItem(
                        text = { Text("프로젝트 재개") },
                        onClick = { menuOpen = false; onReopen() },
                    )
                }
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(project.title, style = DDZTypography.ScreenTitle,
                color = DDZColor.TextPrimary, maxLines = 2,
                overflow = TextOverflow.Ellipsis)
            Text(
                if (project.isActive) "진행 중" else "완료",
                color = DDZColor.SelectedDark, style = DDZTypography.Caption,
            )
            if (selected) Text("현재 촬영 대상으로 선택됨",
                color = DDZColor.PrimaryDark, style = DDZTypography.Secondary)
            Text(summary.captureCount.toString() + "회 촬영",
                style = DDZTypography.SectionTitle, color = DDZColor.TextPrimary)
            Text(
                "최근 촬영 · " + (summary.lastCapturedAtMillis?.let(::formatProjectDate) ?: "없음"),
                color = DDZColor.TextSecondary, style = DDZTypography.Secondary,
            )
        }
        Text("최근 사진", style = DDZTypography.SectionTitle, color = DDZColor.TextPrimary)
        if (summary.recentPhotos.isEmpty()) {
            Text("아직 촬영한 사진이 없습니다.", style = DDZTypography.Secondary,
                color = DDZColor.TextSecondary)
        } else {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                summary.recentPhotos.forEach { photo ->
                    AsyncImage(
                        model = photo.uri,
                        contentDescription = photo.displayName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.weight(1f).aspectRatio(1f)
                            .background(DDZColor.SurfaceSoft, RoundedCornerShape(10.dp)),
                    )
                }
                repeat(3 - summary.recentPhotos.size) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
        Spacer(Modifier.weight(1f))
        if (project.isActive) {
            DDZButton("이어서 촬영", onClick = onContinue,
                modifier = Modifier.fillMaxWidth())
        } else {
            DDZButton("프로젝트 재개", onClick = onReopen,
                modifier = Modifier.fillMaxWidth())
        }
        DDZButton("전체 사진 보기", onClick = onShowAll,
            style = DDZButtonStyle.Secondary,
            modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp))
    }
    if (showRename) ProjectNameDialog(
        title = "프로젝트 이름 변경",
        confirmLabel = "저장",
        initialName = project.title,
        onDismiss = { showRename = false },
        onConfirm = { newName -> showRename = false; onRename(newName) },
    )
    if (showComplete) AlertDialog(
        onDismissRequest = { showComplete = false },
        title = { Text("프로젝트를 완료할까요?") },
        text = { Text("사진과 기록은 유지되며, 현재 촬영 대상이었다면 일반 촬영으로 돌아갑니다.") },
        confirmButton = {
            TextButton(onClick = { showComplete = false; onComplete() }) { Text("완료") }
        },
        dismissButton = {
            TextButton(onClick = { showComplete = false }) { Text("취소") }
        },
    )
}
