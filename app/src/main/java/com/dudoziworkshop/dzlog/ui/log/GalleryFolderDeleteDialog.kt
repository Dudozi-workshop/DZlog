package com.dudoziworkshop.dzlog.ui.log

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.dudoziworkshop.dzlog.ui.theme.DDZColor
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
    val scope = rememberCoroutineScope()
    var version by remember(path) { mutableIntStateOf(0) }
    var busy by remember(path) { mutableStateOf(true) }
    var plan by remember(path) { mutableStateOf<GalleryFolderDeletePlan?>(null) }
    var error by remember(path) { mutableStateOf<String?>(null) }
    var remaining by remember(path) { mutableStateOf<GalleryFolderDeletePlan?>(null) }
    LaunchedEffect(path, version) {
        busy = true
        plan = null
        val result = withContext(Dispatchers.IO) { runCatching { storage.prepareDeletion(path) } }
        result.onSuccess { plan = it }
            .onFailure { error = it.message ?: "삭제 범위를 읽지 못했습니다." }
        busy = false
    }
    AlertDialog(
        onDismissRequest = { if (!busy) onCancel() },
        title = { Text("폴더 삭제 확인") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(path.trimEnd('/').substringAfterLast('/'))
                Text(path.removePrefix(GalleryFolderIndexPolicy.ROOT), color = DDZColor.TextSecondary)
                if (busy) Text(if (plan == null) "삭제 범위 확인 중…" else "폴더 및 파일 삭제 중…")
                plan?.let {
                    Text("결과사진 ${it.resultCount}장 · 원본사진 ${it.originalCount}장")
                    Text("일반 하위 폴더 ${it.folderCount}개 · 총 파일 ${it.fileCount}개")
                    if (it.otherCount > 0) Text("사진 외 파일 ${it.otherCount}개 포함")
                    Text("모든 깊이의 하위 폴더와 원본이 함께 삭제됩니다.")
                    Text("삭제한 폴더와 파일은 되돌릴 수 없습니다.", color = DDZColor.Destructive)
                }
                remaining?.let {
                    Text("남은 파일 ${it.fileCount}개 · 폴더 ${it.entries.count { entry -> entry.directory }}개")
                }
                error?.let { Text(it, color = DDZColor.Destructive) }
            }
        },
        confirmButton = {
            val confirmed = plan
            TextButton(enabled = !busy, onClick = {
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
            }) {
                Text(if (confirmed == null) "삭제 범위 다시 확인" else "폴더 및 사진 삭제",
                    color = if (!busy && confirmed != null) DDZColor.Destructive else DDZColor.TextSecondary)
            }
        },
        dismissButton = {
            TextButton(enabled = !busy, onClick = onCancel) { Text("취소") }
        },
    )
}
