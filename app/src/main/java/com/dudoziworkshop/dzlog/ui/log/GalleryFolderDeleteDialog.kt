package com.dudoziworkshop.dzlog.ui.log

import com.dudoziworkshop.dzlog.feature.help.ui.LocalTutorialReadOnly
import com.dudoziworkshop.dzlog.ui.common.DDZButtonStyle
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import com.dudoziworkshop.dzlog.ui.common.DDZContentDialog
import com.dudoziworkshop.dzlog.ui.common.DDZButton
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.Modifier
import com.dudoziworkshop.dzlog.ui.theme.LocalDDZColor
import com.dudoziworkshop.dzlog.ui.theme.LocalDDZTypography
import com.dudoziworkshop.dzlog.ui.theme.LocalDDZSpacing
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import com.dudoziworkshop.dzlog.data.mediastore.GalleryFolderCatalog
import com.dudoziworkshop.dzlog.data.mediastore.GalleryFolderStorage
import com.dudoziworkshop.dzlog.feature.log.policy.GalleryFolderDeletePlan
import com.dudoziworkshop.dzlog.feature.log.policy.GalleryFolderIndexPolicy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Owns one confirmation at a time; every retry obtains a fresh, explicitly confirmed inventory. */
@Composable
internal fun GalleryFolderDeleteDialog(
    path: String,
    storage: GalleryFolderStorage,
    catalog: GalleryFolderCatalog,
    onCancel: () -> Unit,
    onChanged: () -> Unit,
    onDeleted: () -> Unit,
) {
    val readOnlyTour = LocalTutorialReadOnly.current
    val scope = rememberCoroutineScope()
    var version by remember(path) { mutableIntStateOf(0) }
    var busy by remember(path) { mutableStateOf(true) }
    var plan by remember(path) { mutableStateOf<GalleryFolderDeletePlan?>(null) }
    var error by remember(path) { mutableStateOf<String?>(null) }
    var remaining by remember(path) { mutableStateOf<GalleryFolderDeletePlan?>(null) }
    LaunchedEffect(path, version, readOnlyTour) {
        if (readOnlyTour) {
            // Virtual guide paths must never be passed to SAF inventory or delete operations.
            busy = false
            error = null
            plan = null
            return@LaunchedEffect
        }
        busy = true
        plan = null
        val result = withContext(Dispatchers.IO) { runCatching { storage.prepareDeletion(path) } }
        result.onSuccess { plan = it }
            .onFailure { error = it.message ?: "삭제 범위를 읽지 못했습니다." }
        busy = false
    }
    val colors = LocalDDZColor.current
    val typography = LocalDDZTypography.current
    val spacing = LocalDDZSpacing.current
    DDZContentDialog(
        onDismiss = { if (!busy) onCancel() },
        title = "이 폴더를 삭제할까요?",
        content = {
            Column(Modifier.heightIn(max = 320.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(spacing.controlGap)) {
                Text("삭제할 대상과 포함된 항목을 확인해 주세요.", style = typography.Body, color = colors.TextSecondary)
                GalleryFolderInfoCard(
                    name = path.trimEnd('/').substringAfterLast('/'),
                    detail = if (readOnlyTour) "안내용 사진·하위 폴더 포함"
                        else plan?.let { "사진 ${it.resultCount + it.originalCount}장 · 하위 폴더 ${it.folderCount}개" }
                            ?: path.removePrefix(GalleryFolderIndexPolicy.ROOT).trimEnd('/'),
                )
                if (busy) {
                    Row(horizontalArrangement = Arrangement.spacedBy(spacing.itemGap)) {
                        CircularProgressIndicator()
                        Text(if (plan == null) "삭제 범위 확인 중…" else "삭제 중…", style = typography.Body)
                    }
                }
                plan?.let {
                    if (it.otherCount > 0) Text("사진 외 파일 ${it.otherCount}개도 포함돼요.", style = typography.Caption, color = colors.TextSecondary)
                    Surface(color = colors.DestructiveSoft, shape = MaterialTheme.shapes.medium) {
                        Text("폴더 안의 사진과 하위 폴더도 기기에서 삭제되며 복구할 수 없어요.",
                            modifier = Modifier.padding(spacing.controlGap), style = typography.Secondary, color = colors.TextPrimary)
                    }
                }
                remaining?.let {
                    Text("남은 파일 ${it.fileCount}개 · 폴더 ${it.entries.count { entry -> entry.directory }}개", style = typography.Caption)
                }
                error?.let { Text(it, style = typography.Secondary, color = colors.Destructive) }
            }
        },
        actions = {
            DDZButton("취소", onClick = { if (!busy) onCancel() }, modifier = Modifier.weight(1f), style = DDZButtonStyle.Secondary, enabled = !busy)
            val confirmed = plan
            DDZButton(
                text = if (readOnlyTour) "삭제" else if (busy && confirmed != null) "삭제 중…" else if (confirmed == null) "다시 확인" else "삭제",
                modifier = Modifier.weight(1f),
                style = if (confirmed == null) DDZButtonStyle.Secondary else DDZButtonStyle.Primary,
                containerColorOverride = if (confirmed == null) null else colors.Destructive,
                enabled = !busy && !readOnlyTour, onClick = {
                if (busy || readOnlyTour) return@DDZButton
                if (confirmed == null) {
                    busy = true
                    error = null
                    remaining = null
                    version++
                } else {
                    busy = true
                    error = null
                    scope.launch {
                        var rest: GalleryFolderDeletePlan? = null
                        val result = withContext(Dispatchers.IO) {
                            runCatching {
                                catalog.deleteFolder(confirmed.source) {
                                    storage.deleteFolder(confirmed).also { rest = it }.entries.isEmpty()
                                }
                            }
                        }
                        // Writes may have succeeded even when final verification or catalog persistence failed.
                        onChanged()
                        busy = false
                        if (result.getOrNull() == true) {
                            onDeleted()
                        } else {
                            plan = null
                            remaining = rest
                            error = result.exceptionOrNull()?.message
                                ?: "일부 항목이 남아 폴더를 유지했습니다. 삭제 범위를 다시 확인해 주세요."
                        }
                    }
                }
            })
        },
    )
}
