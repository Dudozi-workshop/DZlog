@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
package com.dudoziworkshop.dzlog.poc

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dudoziworkshop.dzlog.data.backup.LocalTemplateBackup
import com.dudoziworkshop.dzlog.data.backup.LocalTemplateBackupCodec
import com.dudoziworkshop.dzlog.data.backup.LocalTemplateBackupMerge
import com.dudoziworkshop.dzlog.data.backup.LocalTemplateMergePreview
import com.dudoziworkshop.dzlog.data.backup.LocalTemplateMergeResult
import com.dudoziworkshop.dzlog.data.template.EditorWorkspace
import com.dudoziworkshop.dzlog.ui.common.DDZTopBar
import com.dudoziworkshop.dzlog.ui.theme.DDZColor
import com.dudoziworkshop.dzlog.ui.theme.DDZSpacing
import com.dudoziworkshop.dzlog.ui.theme.DDZTypography
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue

internal enum class IosWorkspaceBackupMode { EXPORT, IMPORT }

private const val IOS_EXPORT_REQUEST = "dzlog.exportWorkspace"
private const val IOS_IMPORT_REQUEST = "dzlog.importWorkspace"
private const val IOS_EXPORT_COMPLETED = "dzlog.workspaceExportCompleted"
private const val IOS_EXPORT_FAILED = "dzlog.workspaceExportFailed"
private const val IOS_IMPORT_SELECTED = "dzlog.workspaceImportSelected"
private const val IOS_IMPORT_FAILED = "dzlog.workspaceImportFailed"

@Composable
internal fun IosWorkspaceBackupScreen(
    mode: IosWorkspaceBackupMode,
    workspace: EditorWorkspace,
    onBack: () -> Unit,
    onImportConfirmed: (LocalTemplateBackup) -> LocalTemplateMergeResult?,
) {
    var busy by remember { mutableStateOf(false) }
    var pendingImport by remember { mutableStateOf<LocalTemplateBackup?>(null) }
    var preview by remember { mutableStateOf<LocalTemplateMergePreview?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val center = remember { NSNotificationCenter.defaultCenter }

    DisposableEffect(center, workspace) {
        val exportCompleted = center.addObserverForName(
            IOS_EXPORT_COMPLETED, null, NSOperationQueue.mainQueue,
        ) {
            busy = false
            error = null
            message = "백업 파일을 내보냈습니다."
        }
        val exportFailed = center.addObserverForName(
            IOS_EXPORT_FAILED, null, NSOperationQueue.mainQueue,
        ) { notification ->
            busy = false
            message = null
            error = notification.`object` as? String ?: "백업 파일을 내보내지 못했습니다."
        }
        val importSelected = center.addObserverForName(
            IOS_IMPORT_SELECTED, null, NSOperationQueue.mainQueue,
        ) { notification ->
            busy = false
            message = null
            runCatching {
                val raw = requireNotNull(notification.`object` as? String) { "백업 파일을 읽을 수 없습니다." }
                val archive = LocalTemplateBackupCodec.decode(raw)
                val summary = LocalTemplateBackupMerge.preview(
                    workspace.documents.map { it.template }, archive,
                )
                pendingImport = archive
                preview = summary
                error = null
            }.onFailure {
                pendingImport = null
                preview = null
                error = it.message ?: "백업 파일을 확인하지 못했습니다."
            }
        }
        val importFailed = center.addObserverForName(
            IOS_IMPORT_FAILED, null, NSOperationQueue.mainQueue,
        ) { notification ->
            busy = false
            message = null
            error = notification.`object` as? String ?: "백업 파일을 열지 못했습니다."
        }
        onDispose {
            center.removeObserver(exportCompleted)
            center.removeObserver(exportFailed)
            center.removeObserver(importSelected)
            center.removeObserver(importFailed)
        }
    }

    fun goBack() {
        if (busy) return
        if (pendingImport != null) {
            pendingImport = null
            preview = null
            error = null
        } else onBack()
    }

    val title = if (mode == IosWorkspaceBackupMode.EXPORT) {
        "작업 환경 내보내기"
    } else {
        "작업 환경 가져오기"
    }
    Scaffold(
        containerColor = DDZColor.Background,
        topBar = { DDZTopBar(title = title, onBack = ::goBack) },
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding)
                .verticalScroll(rememberScrollState()).padding(DDZSpacing.screenPadding),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                if (mode == IosWorkspaceBackupMode.EXPORT) {
                    "표 템플릿과 저장 규칙을 파일로 보관해요."
                } else {
                    "백업한 템플릿을 기존 작업 환경에 추가해요."
                },
                style = DDZTypography.Body,
                color = DDZColor.TextSecondary,
            )
            if (mode == IosWorkspaceBackupMode.EXPORT) {
                IosBackupInfoCard {
                    Text("파일에 포함돼요", style = DDZTypography.SettingLabel,
                        color = DDZColor.TextPrimary)
                    Text("표 템플릿·셀·스타일, 순환문구, 파일명과 저장 폴더 구성",
                        style = DDZTypography.Body, color = DDZColor.TextSecondary)
                    HorizontalDivider(color = DDZColor.Border)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = null,
                            tint = DDZColor.TextSecondary)
                        Text("사진, 앨범, 현재 자동번호, 촬영 배치·저장 방식, 카메라 공통 설정은 포함되지 않아요.",
                            modifier = Modifier.padding(start = 8.dp),
                            style = DDZTypography.Caption, color = DDZColor.TextSecondary)
                    }
                }
                IosBackupActionButton(if (busy) "처리 중…" else "백업 파일 내보내기", !busy) {
                    message = null
                    error = null
                    runCatching {
                        LocalTemplateBackupCodec.encode(
                            workspace.documents.map { it.template }, workspace.activeTemplateId,
                        )
                    }.onSuccess { archive ->
                        busy = true
                        center.postNotificationName(IOS_EXPORT_REQUEST, archive)
                    }.onFailure {
                        error = it.message ?: "백업 파일을 만들지 못했습니다."
                    }
                }
            } else if (pendingImport == null) {
                IosBackupInfoCard {
                    Text("기존 템플릿은 유지돼요", style = DDZTypography.SettingLabel,
                        color = DDZColor.TextPrimary)
                    Text("파일을 선택해도 바로 적용되지 않아요. 내용을 확인한 뒤 가져올 수 있어요.",
                        style = DDZTypography.Body, color = DDZColor.TextSecondary)
                }
                IosBackupActionButton(if (busy) "검증 중…" else "백업 파일 선택", !busy) {
                    busy = true
                    message = null
                    error = null
                    center.postNotificationName(IOS_IMPORT_REQUEST, null)
                }
            } else {
                val summary = preview
                if (summary != null) {
                    IosBackupInfoCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.InsertDriveFile, contentDescription = null,
                                tint = DDZColor.PrimaryDark)
                            Text("선택한 DZlog 백업", modifier = Modifier.padding(start = 8.dp),
                                style = DDZTypography.Body, color = DDZColor.TextPrimary)
                        }
                        HorizontalDivider(color = DDZColor.Border)
                        IosBackupSummaryRow("기존 템플릿", "${summary.existingCount}개 유지")
                        IosBackupSummaryRow("가져올 템플릿", "${summary.incomingCount}개 추가")
                        IosBackupSummaryRow("이름 중복", "${summary.renamedCount}개 자동 구분")
                        Text("기존 활성 템플릿은 유지되며 사진과 자동번호는 복원되지 않아요.",
                            style = DDZTypography.Caption, color = DDZColor.TextSecondary)
                    }
                    IosBackupActionButton(if (busy) "적용 중…" else "가져오기", !busy) {
                        val archive = pendingImport ?: return@IosBackupActionButton
                        busy = true
                        message = null
                        error = null
                        val result = runCatching { onImportConfirmed(archive) }.getOrNull()
                        busy = false
                        if (result == null) {
                            error = "작업 환경이 변경되어 가져오기를 중단했습니다. 다시 선택해 주세요."
                        } else {
                            pendingImport = null
                            preview = null
                            message = "${result.preview.incomingCount}개 템플릿을 추가했습니다."
                        }
                    }
                }
            }
            message?.let {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null,
                        tint = DDZColor.SelectedDark)
                    Text(it, modifier = Modifier.padding(start = 8.dp),
                        style = DDZTypography.Body, color = DDZColor.TextPrimary)
                }
            }
            error?.let {
                Text(it, style = DDZTypography.Body, color = DDZColor.PrimaryDark)
            }
        }
    }
}

@Composable
private fun IosBackupInfoCard(
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = DDZColor.Surface,
        border = BorderStroke(1.dp, DDZColor.Border),
    ) {
        Column(modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
    }
}

@Composable
private fun IosBackupSummaryRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = DDZTypography.Body, color = DDZColor.TextSecondary)
        Text(value, style = DDZTypography.Body, color = DDZColor.TextPrimary)
    }
}

@Composable
private fun IosBackupActionButton(label: String, enabled: Boolean, onClick: () -> Unit) {
    Button(
        modifier = Modifier.fillMaxWidth(),
        enabled = enabled,
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = DDZColor.PrimaryDark),
    ) { Text(label) }
}
