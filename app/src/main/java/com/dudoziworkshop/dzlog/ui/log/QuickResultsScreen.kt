package com.dudoziworkshop.dzlog.ui.log

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dudoziworkshop.dzlog.domain.gallery.GalleryCounterBadgePolicy
import com.dudoziworkshop.dzlog.domain.gallery.GalleryGridColumnsPolicy
import com.dudoziworkshop.dzlog.domain.quickresults.QuickResultCapture
import com.dudoziworkshop.dzlog.domain.quickresults.QuickResultsSessionPolicy
import com.dudoziworkshop.dzlog.ui.theme.DDZColor
import com.dudoziworkshop.dzlog.ui.theme.DDZTypography

/**
 * Read-only camera-session results surface.
 * The caller owns stored grid columns and viewer navigation; this screen never
 * changes captured files or the Undo stack.
 */
@Composable
internal fun QuickResultsScreen(
    captures: List<QuickResultCapture>,
    gridColumns: Int,
    showCounterBadges: Boolean,
    onGridColumnsChange: (Int) -> Unit,
    onOpenCapture: (List<QuickResultCapture>, Int) -> Unit,
    onReturnToCamera: () -> Unit,
) {
    val columns = GalleryGridColumnsPolicy.normalize(gridColumns)
    val folders = remember(captures) { QuickResultsSessionPolicy.folderChoices(captures) }
    var selectedFolder by rememberSaveable { mutableStateOf<String?>(null) }
    var menuExpanded by remember { mutableStateOf(false) }
    val collapsed = remember { mutableStateMapOf<String, Boolean>() }
    val listState = rememberLazyListState()
    val sections = remember(captures, selectedFolder) {
        QuickResultsSessionPolicy.sections(captures, selectedFolder)
    }

    val visibleCaptures = sections.flatMap { it.captures }

    Column(Modifier.fillMaxSize().background(DDZColor.Background)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("빠른결과", style = DDZTypography.ScreenTitle, color = DDZColor.TextPrimary)
                Text("이번 촬영 ${captures.size}건", color = DDZColor.TextSecondary)
            }
            Box {
                TextButton(onClick = { menuExpanded = true }) {
                    Text((selectedFolder?.trimEnd('/')?.substringAfterLast('/') ?: "전체 폴더") + " ▾")
                }
                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                    DropdownMenuItem(text = { Text("전체 폴더") }, onClick = {
                        selectedFolder = null
                        menuExpanded = false
                    })
                    folders.forEach { folder ->
                        DropdownMenuItem(
                            text = { Text(folder.trimEnd('/').substringAfterLast('/')) },
                            onClick = {
                                selectedFolder = folder
                                menuExpanded = false
                            },
                        )
                    }
                }
            }
        }

        if (captures.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("이번 촬영 사진이 없습니다.", color = DDZColor.TextSecondary)
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth().galleryGridPinch(
                    columns = columns,
                    enabled = true,
                    onColumnsChanged = onGridColumnsChange,
                ),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                sections.forEach { section ->
                    item(key = "header:" + section.key) {
                        Row(
                            Modifier.fillMaxWidth()
                                .background(DDZColor.Surface, RoundedCornerShape(12.dp))
                                .clickable { collapsed[section.key] = collapsed[section.key] != true }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                section.folderPath.trimEnd('/').substringAfterLast('/'),
                                modifier = Modifier.weight(1f),
                                color = DDZColor.TextPrimary,
                            )
                            Text("${section.captures.size}건 " + if (collapsed[section.key] == true) "▾" else "▴")
                        }
                    }
                    if (collapsed[section.key] != true) {
                        section.captures.chunked(columns).forEach { row ->
                            item(key = "row:${section.key}:${row.first().captureIndex}") {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                                ) {
                                    row.forEach { capture ->
                                        Box(
                                            modifier = Modifier.weight(1f)
                                                .aspectRatio(1f)
                                                .background(DDZColor.SurfaceSoft, RoundedCornerShape(8.dp))
                                                .clickable {
                                                    onOpenCapture(
                                                        visibleCaptures,
                                                        visibleCaptures.indexOfFirst { it.captureIndex == capture.captureIndex },
                                                    )
                                                },
                                        ) {
                                            DzThumbnail(capture.displayedPhoto.uri)
                                            val label = GalleryCounterBadgePolicy.labelFromDzlogFilename(
                                                capture.displayedPhoto.displayName,
                                                capture.displayedPhoto.relativePath,
                                                showCounterBadges,
                                            )
                                            if (label != null) {
                                                Text(
                                                    label,
                                                    modifier = Modifier.align(Alignment.BottomStart)
                                                        .background(DDZColor.PrimaryDark, RoundedCornerShape(4.dp))
                                                        .padding(horizontal = 5.dp, vertical = 2.dp),
                                                    color = androidx.compose.ui.graphics.Color.White,
                                                )
                                            }
                                            if (capture.isOriginalOnly) {
                                                Text(
                                                    "원본",
                                                    modifier = Modifier.align(Alignment.TopStart)
                                                        .background(DDZColor.PrimaryDark, RoundedCornerShape(4.dp))
                                                        .padding(horizontal = 5.dp, vertical = 2.dp),
                                                    color = androidx.compose.ui.graphics.Color.White,
                                                )
                                            }
                                        }
                                    }
                                    repeat((columns - row.size).coerceAtLeast(0)) {
                                        Box(Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Button(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            onClick = onReturnToCamera,
        ) { Text("카메라로") }
    }
}
