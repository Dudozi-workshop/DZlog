package com.dudoziworkshop.dzlog.poc

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dudoziworkshop.dzlog.data.media.PhotoKitProjectDeletion
import com.dudoziworkshop.dzlog.data.project.IosCaptureProject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import com.dudoziworkshop.dzlog.data.project.IosCaptureProjectRegistry
import com.dudoziworkshop.dzlog.ui.common.DDZButton
import com.dudoziworkshop.dzlog.ui.common.DDZButtonStyle
import com.dudoziworkshop.dzlog.ui.theme.DDZColor
import com.dudoziworkshop.dzlog.ui.theme.DDZSpacing
import com.dudoziworkshop.dzlog.ui.theme.DDZTypography
import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.dateWithTimeIntervalSince1970

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun IosProjectPickerSheet(
    registry: IosCaptureProjectRegistry,
    onSelect: (String?) -> Unit,
    onCreate: () -> Unit,
    onManage: () -> Unit,
    onDismiss: () -> Unit,
) {
    val openProjects = registry.projects.filter(IosCaptureProject::isOpen)
        .sortedByDescending(IosCaptureProject::createdAt)
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = DDZColor.Surface) {
        Column(
            modifier = Modifier.fillMaxWidth().navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("촬영 프로젝트 선택", modifier = Modifier.weight(1f),
                    style = DDZTypography.ScreenTitle, color = DDZColor.TextPrimary)
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "닫기")
                }
            }
            LazyColumn(
                modifier = Modifier.fillMaxWidth().heightIn(max = 370.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                item(key = "general") {
                    IosProjectChoice("일반 촬영", registry.activeId == null) { onSelect(null) }
                }
                item(key = "project-section") {
                    Text("진행 중인 프로젝트", modifier = Modifier.padding(top = 10.dp, bottom = 2.dp),
                        style = DDZTypography.Caption, color = DDZColor.TextSecondary)
                }
                if (openProjects.isEmpty()) item(key = "empty") {
                    Text("진행 중인 프로젝트가 없습니다.", modifier = Modifier.padding(12.dp),
                        style = DDZTypography.Secondary, color = DDZColor.TextSecondary)
                }
                items(openProjects, key = { it.id }) { project ->
                    IosProjectChoice(project.title, project.id == registry.activeId) {
                        onSelect(project.id)
                    }
                }
            }
            HorizontalDivider()
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onClick = onCreate) { Text("+ 새 프로젝트") }
                TextButton(onClick = onManage) { Text("전체 프로젝트 관리") }
            }
            Spacer(Modifier.height(4.dp))
        }
    }
}

@Composable
private fun IosProjectChoice(title: String, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().semantics {
            contentDescription = title
            selected = isSelected
        },
        color = if (isSelected) DDZColor.SurfaceSoft else DDZColor.Surface,
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Folder, contentDescription = null, tint = DDZColor.SelectedDark)
            Spacer(Modifier.width(10.dp))
            Text(title, modifier = Modifier.weight(1f), style = DDZTypography.Body,
                color = DDZColor.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (isSelected) Icon(Icons.Default.Check, contentDescription = null,
                tint = DDZColor.Selected)
        }
    }
}

@Composable
internal fun IosProjectListScreen(
    registry: IosCaptureProjectRegistry,
    createInitially: Boolean,
    onBack: () -> Unit,
    onCreate: (String) -> Unit,
    onOpen: (String) -> Unit,
) {
    var showCreate by remember { mutableStateOf(createInitially) }
    val openProjects = registry.projects.filter(IosCaptureProject::isOpen)
        .sortedByDescending(IosCaptureProject::createdAt)
    val completed = registry.projects.filterNot(IosCaptureProject::isOpen)
        .sortedByDescending { it.completedAt ?: it.createdAt }
    Column(
        Modifier.fillMaxSize().background(DDZColor.Background)
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로")
            }
            Text("프로젝트", style = DDZTypography.ScreenTitle, color = DDZColor.TextPrimary,
                modifier = Modifier.weight(1f))
            TextButton(onClick = { showCreate = true }) { Text("+ 만들기") }
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            item { IosProjectSectionTitle("진행 중 · ${openProjects.size}") }
            if (openProjects.isEmpty()) item { IosProjectEmpty("진행 중인 프로젝트가 없습니다.") }
            items(openProjects, key = { "open-${it.id}" }) { project ->
                IosProjectRow(project, project.id == registry.activeId) { onOpen(project.id) }
            }
            item { IosProjectSectionTitle("완료 · ${completed.size}", topPadding = 24.dp) }
            if (completed.isEmpty()) item { IosProjectEmpty("완료된 프로젝트가 없습니다.") }
            items(completed, key = { "completed-${it.id}" }) { project ->
                IosProjectRow(project, false) { onOpen(project.id) }
            }
        }
    }
    if (showCreate) IosProjectNameDialog(
        title = "새 프로젝트",
        confirmLabel = "만들기",
        onDismiss = { showCreate = false },
        onConfirm = { name -> showCreate = false; onCreate(name) },
    )
}

@Composable
private fun IosProjectSectionTitle(text: String, topPadding: androidx.compose.ui.unit.Dp = 10.dp) {
    Text(text, style = DDZTypography.SectionTitle, color = DDZColor.TextPrimary,
        modifier = Modifier.padding(top = topPadding, bottom = 10.dp))
}

@Composable
private fun IosProjectEmpty(text: String) {
    Text(text, color = DDZColor.TextSecondary, style = DDZTypography.Secondary,
        modifier = Modifier.padding(12.dp))
}

@Composable
private fun IosProjectRow(project: IosCaptureProject, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().background(DDZColor.Surface, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick).semantics { contentDescription = "프로젝트 상세 · ${project.title}" }.padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(project.title, maxLines = 1, overflow = TextOverflow.Ellipsis,
                style = DDZTypography.SettingLabel, color = DDZColor.TextPrimary)
            Text("${project.captureIds.size}회 촬영 · ${formatIosProjectDate(project.createdAt)} 시작",
                style = DDZTypography.Caption, color = DDZColor.TextSecondary)
            if (selected) Text("현재 촬영 대상", style = DDZTypography.Caption,
                color = DDZColor.SelectedDark)
        }
        Icon(Icons.Default.ChevronRight, contentDescription = "상세 보기", tint = DDZColor.IconMuted)
    }
}

@Composable
internal fun IosProjectDetailScreen(
    project: IosCaptureProject,
    selected: Boolean,
    onBack: () -> Unit,
    onContinue: () -> Unit,
    onShowPhotos: () -> Unit,
    onRename: (String) -> Unit,
    onComplete: () -> Unit,
    onReopen: () -> Unit,
    onDeleteMetadata: () -> Unit,
    prepareDeletion: suspend () -> PhotoKitProjectDeletion,
    deleteWithPhotos: suspend (PhotoKitProjectDeletion) -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    var showRename by remember { mutableStateOf(false) }
    var showComplete by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }
    var showPhotoDelete by remember(project.id) { mutableStateOf(false) }
    Column(
        Modifier.fillMaxSize().background(DDZColor.Background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = DDZSpacing.screenPadding),
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
                        menuOpen = false; showRename = true
                    })
                    if (project.isOpen) DropdownMenuItem(text = { Text("프로젝트 완료") }, onClick = {
                        menuOpen = false; showComplete = true
                    }) else DropdownMenuItem(text = { Text("프로젝트 재개") }, onClick = {
                        menuOpen = false; onReopen()
                    })
                    DropdownMenuItem(text = { Text("프로젝트만 삭제 · 사진 유지") }, onClick = {
                        menuOpen = false; showDelete = true
                    })
                    DropdownMenuItem(text = { Text("프로젝트 및 연결 사진 삭제") }, onClick = {
                        menuOpen = false; showPhotoDelete = true
                    })
                }
            }
        }
        Text(project.title, style = DDZTypography.ScreenTitle, color = DDZColor.TextPrimary,
            maxLines = 2, overflow = TextOverflow.Ellipsis)
        Text(if (project.isOpen) "진행 중" else "완료", color = DDZColor.SelectedDark,
            style = DDZTypography.Caption)
        if (selected) Text("현재 촬영 대상으로 선택됨", color = DDZColor.PrimaryDark,
            style = DDZTypography.Secondary)
        Text("${project.captureIds.size}회 촬영", style = DDZTypography.SectionTitle,
            color = DDZColor.TextPrimary)
        Text("시작 · ${formatIosProjectDate(project.createdAt)}", style = DDZTypography.Secondary,
            color = DDZColor.TextSecondary)
        if (project.captureIds.isEmpty()) Text("아직 촬영한 사진이 없습니다.",
            style = DDZTypography.Secondary, color = DDZColor.TextSecondary)
        Spacer(Modifier.weight(1f))
        if (project.isOpen) {
            DDZButton("이어서 촬영", onClick = onContinue, modifier = Modifier.fillMaxWidth())
        } else {
            DDZButton("프로젝트 재개", onClick = onReopen, modifier = Modifier.fillMaxWidth())
        }
        DDZButton("프로젝트 사진 보기", onClick = onShowPhotos,
            enabled = project.captureIds.isNotEmpty(),
            style = DDZButtonStyle.Secondary,
            modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp))
    }
    if (showRename) IosProjectNameDialog(
        title = "프로젝트 이름 변경", confirmLabel = "저장", initialName = project.title,
        onDismiss = { showRename = false },
        onConfirm = { name -> showRename = false; onRename(name) },
    )
    if (showComplete) AlertDialog(
        onDismissRequest = { showComplete = false },
        title = { Text("프로젝트를 완료할까요?") },
        text = { Text("사진과 기록은 유지되며, 현재 촬영 대상이었다면 일반 촬영으로 돌아갑니다.") },
        confirmButton = { TextButton(onClick = { showComplete = false; onComplete() }) { Text("완료") } },
        dismissButton = { TextButton(onClick = { showComplete = false }) { Text("취소") } },
    )
    if (showPhotoDelete) IosProjectPhotoDeleteDialog(project.title, prepareDeletion, deleteWithPhotos) {
        showPhotoDelete = false
    }
    if (showDelete) AlertDialog(
        onDismissRequest = { showDelete = false },
        title = { Text("프로젝트만 삭제할까요?") },
        text = { Text("프로젝트 기록만 삭제합니다. 기존 사진은 그대로 유지됩니다.") },
        confirmButton = { TextButton(onClick = { showDelete = false; onDeleteMetadata() }) {
            Text("프로젝트만 삭제")
        } },
        dismissButton = { TextButton(onClick = { showDelete = false }) { Text("취소") } },
    )
}

@Composable
private fun IosProjectPhotoDeleteDialog(
    title: String,
    prepare: suspend () -> PhotoKitProjectDeletion,
    delete: suspend (PhotoKitProjectDeletion) -> Unit,
    dismiss: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var revision by remember { mutableIntStateOf(0) }
    var plan by remember { mutableStateOf<PhotoKitProjectDeletion?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var deleting by remember { mutableStateOf(false) }
    LaunchedEffect(revision) {
        try {
            plan = prepare()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            error = failure.message ?: "연결 사진을 확인하지 못했습니다."
        }
    }
    AlertDialog(
        onDismissRequest = { if (!deleting) dismiss() },
        title = { Text("프로젝트 및 연결 사진 삭제") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(title)
                plan?.let {
                    Text("결과 사진 ${it.resultCount}장 · 원본 사진 ${it.originalCount}장")
                    if (it.alreadyMissingCount > 0) Text("이미 삭제된 사진 ${it.alreadyMissingCount}장의 기록도 정리합니다.")
                    Text("프로젝트와 여기에 연결된 사진을 삭제합니다. 같은 폴더의 다른 사진은 유지됩니다.")
                }
                if (plan == null && error == null) Text("연결 사진을 확인하고 있습니다.")
                if (deleting) Text("삭제 결과를 확인하고 있습니다.")
                error?.let {
                    Text(it)
                    if (rememberIosPermissions().photos != IosAccess.ALLOWED) {
                        TextButton(onClick = ::openIosSystemSettings) { Text("iPhone 설정 열기") }
                    }
                }
            }
        },
        confirmButton = {
            if (error != null) TextButton(onClick = { error = null; revision++ }) { Text("범위 다시 확인") }
            else TextButton(enabled = plan != null && !deleting, onClick = {
                val confirmed = plan ?: return@TextButton
                deleting = true
                scope.launch {
                    try {
                        delete(confirmed)
                        dismiss()
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (failure: Exception) {
                        plan = null
                        error = failure.message ?: "삭제 결과를 확인하지 못했습니다."
                    } finally {
                        deleting = false
                    }
                }
            }) { Text("프로젝트 및 사진 삭제") }
        },
        dismissButton = { TextButton(enabled = !deleting, onClick = dismiss) { Text("취소") } },
    )
}

@Composable
private fun IosProjectNameDialog(
    title: String,
    confirmLabel: String,
    initialName: String = "",
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var name by remember(initialName) { mutableStateOf(initialName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, style = DDZTypography.SectionTitle) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it.take(80) },
                label = { Text("프로젝트 이름") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().semantics { contentDescription = "프로젝트 이름" },
            )
        },
        confirmButton = { TextButton(onClick = { onConfirm(name.trim()) },
            enabled = name.trim().isNotEmpty()) { Text(confirmLabel) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("취소") } },
    )
}

private fun formatIosProjectDate(timestampMillis: Long): String {
    val formatter = NSDateFormatter().apply { dateFormat = "yyyy.MM.dd HH:mm" }
    return formatter.stringFromDate(NSDate.dateWithTimeIntervalSince1970(timestampMillis / 1_000.0))
}
