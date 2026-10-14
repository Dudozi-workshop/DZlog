package com.dudoziworkshop.dzlog.ui.log

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.dudoziworkshop.dzlog.feature.log.policy.GalleryChildFolder
import com.dudoziworkshop.dzlog.feature.log.policy.GalleryFolderSummary
import com.dudoziworkshop.dzlog.ui.theme.LocalDDZSpacing
import com.dudoziworkshop.dzlog.ui.theme.LocalDDZTypography
import java.util.Locale

internal enum class GalleryFolderFilter(val label: String) {
    ALL("전체"), WITH_PHOTOS("사진 있음"), EMPTY("빈 폴더")
}
internal enum class GalleryFolderSort(val label: String) {
    NAME("이름순"), RECENT("최근 사진순")
}

internal fun filterGalleryFolders(
    folders: List<GalleryChildFolder>,
    summaries: Map<String, GalleryFolderSummary>,
    filter: GalleryFolderFilter,
    sort: GalleryFolderSort,
): List<GalleryChildFolder> {
    val filtered = folders.filter {
        val count = summaries[it.relativePath]?.totalPhotoCount ?: it.totalImageCount
        when (filter) {
            GalleryFolderFilter.ALL -> true
            GalleryFolderFilter.WITH_PHOTOS -> count > 0
            GalleryFolderFilter.EMPTY -> count == 0
        }
    }
    val nameOrder = compareBy<GalleryChildFolder> { it.name.lowercase(Locale.ROOT) }.thenBy { it.relativePath }
    return filtered.sortedWith(when (sort) {
        GalleryFolderSort.NAME -> nameOrder
        GalleryFolderSort.RECENT -> compareByDescending<GalleryChildFolder> {
            summaries[it.relativePath]?.latestPhotoEpochMillis ?: Long.MIN_VALUE
        }.then(nameOrder)
    })
}

@Composable
internal fun GalleryFolderFilterControls(
    filter: GalleryFolderFilter,
    sort: GalleryFolderSort,
    enabled: Boolean,
    onFilter: (GalleryFolderFilter) -> Unit,
    onSort: (GalleryFolderSort) -> Unit,
) {
    var filterOpen by remember { mutableStateOf(false) }
    var sortOpen by remember { mutableStateOf(false) }
    val typography = LocalDDZTypography.current
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(LocalDDZSpacing.current.itemGap)) {
        Box {
            TextButton(onClick = { filterOpen = true }, enabled = enabled) { Text("${filter.label} ▾", style = typography.TextAction) }
            DropdownMenu(expanded = filterOpen, onDismissRequest = { filterOpen = false }) {
                GalleryFolderFilter.entries.forEach { item ->
                    DropdownMenuItem(text = { Text(if (filter == item) "✓ ${item.label}" else item.label) }, onClick = { onFilter(item); filterOpen = false })
                }
            }
        }
        Box {
            TextButton(onClick = { sortOpen = true }, enabled = enabled) { Text("${sort.label} ▾", style = typography.TextAction) }
            DropdownMenu(expanded = sortOpen, onDismissRequest = { sortOpen = false }) {
                GalleryFolderSort.entries.forEach { item ->
                    DropdownMenuItem(text = { Text(if (sort == item) "✓ ${item.label}" else item.label) }, onClick = { onSort(item); sortOpen = false })
                }
            }
        }
    }
}
