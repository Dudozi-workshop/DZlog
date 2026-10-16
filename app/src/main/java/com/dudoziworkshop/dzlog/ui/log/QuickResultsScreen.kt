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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dudoziworkshop.dzlog.domain.gallery.GalleryCounterBadgePolicy
import com.dudoziworkshop.dzlog.domain.gallery.GalleryGridColumnsPolicy
import com.dudoziworkshop.dzlog.domain.quickresults.QuickResultsGridAnchorPolicy
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
    var collapsedKeys by rememberSaveable { mutableStateOf(emptySet<String>()) }
    val listState = rememberLazyListState()
    var pendingPinchAnchor by remember {
        mutableStateOf<Pair<Int, QuickResultsGridAnchorPolicy.Position>?>(null)
    }
    val sections = remember(captures, selectedFolder) {
        QuickResultsSessionPolicy.sections(captures, selectedFolder)
    }

    val visibleCaptures = sections.flatMap { it.captures }
    LaunchedEffect(columns, pendingPinchAnchor) {
        val pending = pendingPinchAnchor
        if (pending != null && pending.first == columns) {
            listState.scrollToItem(pending.second.itemIndex, pending.second.scrollOffset)
            pendingPinchAnchor = null
        }
    }

    Column(Modifier.fillMaxSize().background(DDZColor.Background).windowInsetsPadding(WindowInsets.safeDrawing)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("빠른결과", style = DDZTypography.ScreenTitle, color = DDZColor.TextPrimary)
                Text("이번 촬영 ${captures.size}건", color = DDZColor.TextSecondary)
            }
            Box {
                TextButton(onClick = { menuExpanded = true }) {
                    Text((selectedFolder?.let(QuickResultsSessionPolicy::folderLabel) ?: "전체 폴더") + " ▾", color = DDZColor.Primary, maxLines = 1, overflow = TextOverflow.Ellipsis, style = DDZTypography.TextAction)
                }
                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                    DropdownMenuItem(text = { Text("전체 폴더") }, onClick = {
                        selectedFolder = null
                        menuExpanded = false
                    })
                    folders.forEach { folder ->
                        DropdownMenuItem(
                            text = { Text(QuickResultsSessionPolicy.folderLabel(folder)) },
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
        } else if (visibleCaptures.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("선택한 폴더에 이번 촬영 사진이 없습니다.", color = DDZColor.TextSecondary)
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth().galleryGridPinch(
                    columns = columns,
                    enabled = true,
                    onColumnsChanged = { nextColumns ->
                        pendingPinchAnchor = nextColumns to QuickResultsGridAnchorPolicy.remap(
                            sections = sections,
                            collapsedKeys = collapsedKeys,
                            firstVisibleItemIndex = listState.firstVisibleItemIndex,
                            firstVisibleScrollOffset = listState.firstVisibleItemScrollOffset,
                            oldColumns = columns,
                            newColumns = nextColumns,
                        )
                        onGridColumnsChange(nextColumns)
                    },
                ),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                sections.forEach { section ->
                    item(key = "header:" + section.key) {
                        Column(
                            modifier = Modifier.fillMaxWidth()
                                .background(DDZColor.Surface, RoundedCornerShape(14.dp))
                                .clickable {
                                    collapsedKeys = if (section.key in collapsedKeys) {
                                        collapsedKeys - section.key
                                    } else {
                                        collapsedKeys + section.key
                                    }
                                }
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                        ) {
                            // A leaf folder/date alone is ambiguous. Keep every path
                            // segment readable, even on a narrow device.
                            Text(
                                text = QuickResultsSessionPolicy.folderLabel(section.folderPath),
                                color = DDZColor.TextPrimary,
                                style = DDZTypography.SettingLabel,
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    QuickResultsSessionPolicy.sectionLabel(section) +
                                        if (section.key in collapsedKeys) " ▾" else " ▴",
                                    color = DDZColor.TextSecondary,
                                    style = DDZTypography.Caption,
                                )
                            }
                        }
                    }
                    if (section.key !in collapsedKeys) {
                        section.captures.chunked(columns).forEach { row ->
                            item(key = "row:${section.key}:${row.first().captureIndex}") {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
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
                                                GalleryCounterBadgeLabel(
                                                    label = label,
                                                    modifier = Modifier.align(Alignment.BottomStart)
                                                        .padding(5.dp),
                                                )
                                            }
                                            if (capture.isOriginalOnly) {
                                                Text(
                                                    "원본",
                                                    modifier = Modifier.align(Alignment.TopStart)
                                                        .background(DDZColor.SageLight, RoundedCornerShape(10.dp))
                                                        .padding(horizontal = 8.dp, vertical = 3.dp),
                                                    color = DDZColor.Primary,
                                                    style = DDZTypography.Caption,
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
            modifier = Modifier.fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .height(52.dp),
            onClick = onReturnToCamera,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = DDZColor.PrimaryDark),
        ) { Text("카메라로", style = DDZTypography.ButtonText) }
    }
}
