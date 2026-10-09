package com.dudoziworkshop.dzlog.feature.settings.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.dudoziworkshop.dzlog.data.backup.LocalTemplateBackup
import com.dudoziworkshop.dzlog.data.backup.LocalTemplateBackupRepository
import com.dudoziworkshop.dzlog.data.backup.LocalTemplateMergePreview
import com.dudoziworkshop.dzlog.data.backup.LocalTemplateMergeResult
import com.dudoziworkshop.dzlog.ui.common.DDZTopBar
import com.dudoziworkshop.dzlog.ui.theme.DDZColor
import com.dudoziworkshop.dzlog.ui.theme.DDZSpacing
import com.dudoziworkshop.dzlog.ui.theme.DDZTypography
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

enum class BackupScreenMode { EXPORT, IMPORT }

@Composable
fun SettingsBackupScreen(
    mode: BackupScreenMode,
    onBack: () -> Unit,
    onImported: (LocalTemplateMergeResult) -> Unit,
) {
    val context = LocalContext.current
    val repo = remember(context) { LocalTemplateBackupRepository(context) }
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var pendingExport by remember { mutableStateOf<String?>(null) }
    var pendingImport by remember { mutableStateOf<LocalTemplateBackup?>(null) }
    var preview by remember { mutableStateOf<LocalTemplateMergePreview?>(null) }
    var filename by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    fun goBack() {
        if (busy) return
        if (pendingImport != null) {
            pendingImport = null
            preview = null
        } else onBack()
    }
    BackHandler { goBack() }

    val createDocument = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream"),
    ) { uri ->
        val prepared = pendingExport
        pendingExport = null
        if (uri != null && prepared != null) {
            scope.launch {
                busy = true
                error = null
                try {
                    repo.writeTo(uri, prepared)
                    message = "백업 파일을 저장했습니다."
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (e: Exception) {
                    error = e.message ?: "백업 파일을 저장하지 못했습니다."
                } finally {
                    busy = false
                }
            }
        }
    }

    val openDocument = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            scope.launch {
                busy = true
                error = null
                message = null
                try {
                    val archive = repo.readFrom(uri)
                    val summary = repo.preview(archive)
                    pendingImport = archive
                    preview = summary
                    filename = repo.nameFrom(uri)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (e: Exception) {
                    pendingImport = null
                    preview = null
                    error = e.message ?: "백업 파일을 확인하지 못했습니다."
                } finally {
                    busy = false
                }
            }
        }
    }

    val title = if (mode == BackupScreenMode.EXPORT) "작업 환경 내보내기" else "작업 환경 가져오기"
    Scaffold(
        containerColor = DDZColor.Background,
        topBar = { DDZTopBar(title = title, onBack = ::goBack) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(DDZSpacing.screenPadding),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = if (mode == BackupScreenMode.EXPORT) {
                    "표 템플릿과 저장 규칙을 파일로 보관해요."
                } else {
                    "백업한 템플릿을 기존 작업 환경에 추가해요."
                },
                style = DDZTypography.Body,
                color = DDZColor.TextSecondary,
            )
            if (mode == BackupScreenMode.EXPORT) {
                BackupInfoCard {
                    Text("파일에 포함돼요", style = DDZTypography.SettingLabel, color = DDZColor.TextPrimary)
                    Text(
                        "표 템플릿·셀·스타일, 순환문구, 파일명과 저장 폴더 구성",
                        style = DDZTypography.Body, color = DDZColor.TextSecondary,
                    )
                    HorizontalDivider(color = DDZColor.Border)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = DDZColor.TextSecondary)
                        Text(
                            "사진, 앨범, 현재 자동번호, 카메라 공통 설정은 포함되지 않아요.",
                            modifier = Modifier.padding(start = 8.dp),
                            style = DDZTypography.Caption,
                            color = DDZColor.TextSecondary,
                        )
                    }
                }
                BackupActionButton(
                    label = if (busy) "처리 중…" else "저장 위치 선택",
                    enabled = !busy,
                ) {
                    scope.launch {
                        busy = true
                        error = null
                        message = null
                        try {
                            pendingExport = repo.exportArchive()
                            val name = SimpleDateFormat("yyyyMMdd", Locale.KOREA).format(Date())
                            createDocument.launch("DZlog_backup_${name}.dzlog")
                        } catch (cancelled: CancellationException) {
                            throw cancelled
                        } catch (e: Exception) {
                            pendingExport = null
                            error = e.message ?: "백업 파일을 만들지 못했습니다."
                        } finally {
                            busy = false
                        }
                    }
                }
            } else if (pendingImport == null) {
                BackupInfoCard {
                    Text("기존 템플릿은 유지돼요", style = DDZTypography.SettingLabel, color = DDZColor.TextPrimary)
                    Text(
                        "파일을 선택해도 바로 적용되지 않아요. 내용을 확인한 뒤 가져올 수 있어요.",
                        style = DDZTypography.Body, color = DDZColor.TextSecondary,
                    )
                }
                BackupActionButton(label = if (busy) "검증 중…" else "백업 파일 선택", enabled = !busy) {
                    openDocument.launch(arrayOf("*/*"))
                }
            } else {
                val summary = preview
                if (summary != null) {
                    BackupInfoCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.InsertDriveFile, contentDescription = null, tint = DDZColor.PrimaryDark)
                            Text(filename, modifier = Modifier.padding(start = 8.dp), style = DDZTypography.Body, color = DDZColor.TextPrimary)
                        }
                        HorizontalDivider(color = DDZColor.Border)
                        SummaryRow("기존 템플릿", "${summary.existingCount}개 유지")
                        SummaryRow("가져올 템플릿", "${summary.incomingCount}개 추가")
                        SummaryRow("이름 중복", "${summary.renamedCount}개 자동 구분")
                        Text(
                            "기존 활성 템플릿은 유지되며 사진과 자동번호는 복원되지 않아요.",
                            style = DDZTypography.Caption,
                            color = DDZColor.TextSecondary,
                        )
                    }
                    BackupActionButton(label = if (busy) "적용 중…" else "가져오기", enabled = !busy) {
                        val archive = pendingImport ?: return@BackupActionButton
                        scope.launch {
                            busy = true
                            error = null
                            try {
                                val result = repo.importConfirmed(archive)
                                pendingImport = null
                                preview = null
                                onImported(result)
                                message = "${result.preview.incomingCount}개 템플릿을 추가했습니다."
                            } catch (cancelled: CancellationException) {
                                throw cancelled
                            } catch (e: Exception) {
                                error = e.message ?: "가져오기에 실패했습니다."
                            } finally {
                                busy = false
                            }
                        }
                    }
                }
            }
            if (message != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, null, tint = DDZColor.SelectedDark)
                    Text(message.orEmpty(), modifier = Modifier.padding(start = 8.dp), style = DDZTypography.Body)
                }
            }
            if (error != null) {
                Text(error.orEmpty(), style = DDZTypography.Body, color = DDZColor.PrimaryDark)
            }
        }
    }
}

@Composable
private fun BackupInfoCard(content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = DDZColor.Surface,
        border = BorderStroke(1.dp, DDZColor.Border),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
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
private fun BackupActionButton(label: String, enabled: Boolean, onClick: () -> Unit) {
    Button(
        modifier = Modifier.fillMaxWidth(),
        enabled = enabled,
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = DDZColor.PrimaryDark),
    ) {
        Text(label)
    }
}
