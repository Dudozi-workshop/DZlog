package com.dudoziworkshop.dzlog.ui.navigation

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.dudoziworkshop.dzlog.data.mediastore.GalleryFolderStorage
import com.dudoziworkshop.dzlog.data.mediastore.GallerySnapshotMemory
import com.dudoziworkshop.dzlog.feature.log.policy.GalleryMediaAccess
import com.dudoziworkshop.dzlog.feature.log.policy.GalleryMediaAccessPolicy
import com.dudoziworkshop.dzlog.ui.common.*
import com.dudoziworkshop.dzlog.ui.log.GalleryFolderAccessGuide
import com.dudoziworkshop.dzlog.ui.theme.LocalDDZColor
import com.dudoziworkshop.dzlog.ui.theme.LocalDDZSpacing
import com.dudoziworkshop.dzlog.ui.theme.LocalDDZTypography
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal data class RequiredPermissionState(val camera: Boolean, val photos: GalleryMediaAccess) {
    val ready: Boolean get() = camera && photos == GalleryMediaAccess.FULL
}

private fun readPermissionState(context: Context) = RequiredPermissionState(
    context.checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED,
    GalleryMediaAccessPolicy.state(context),
)

@Composable
private fun rememberPermissionState(): MutableState<RequiredPermissionState> {
    val context = LocalContext.current
    val state = remember(context) { mutableStateOf(readPermissionState(context)) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        val current = readPermissionState(context)
        if (state.value != current) GallerySnapshotMemory.cache.invalidate()
        state.value = current
    }
    return state
}

private fun Context.activity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.activity()
    else -> null
}

internal fun openAppPermissionSettings(context: Context) {
    runCatching {
        context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.parse("package:${context.packageName}")))
    }.onFailure {
        Toast.makeText(context, "설정을 열지 못했어요. 휴대폰 설정에서 DZlog 앱 권한을 확인해 주세요.", Toast.LENGTH_LONG).show()
    }
}

private fun dzlogFolderInitialUri(): Uri = DocumentsContract.buildTreeDocumentUri(
    "com.android.externalstorage.documents",
    "primary:Pictures/DZlog",
)

/** Actual grants, never a stored onboarding-completed flag, control access. */
@Composable
internal fun RequiredPermissionsGate(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val permissionState = rememberPermissionState()
    val state by permissionState
    val storage = remember(context) { GalleryFolderStorage(context) }
    var folderConnected by remember { mutableStateOf(storage.isConnected()) }
    var showFolderGuide by remember { mutableStateOf(false) }
    var selectedFolderPath by remember { mutableStateOf<String?>(null) }
    val preferences = remember(context) { context.getSharedPreferences("dzlog_required_permissions", Context.MODE_PRIVATE) }
    var requesting by remember { mutableStateOf(false) }
    var connectingFolder by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val requester = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        requesting = false
        GallerySnapshotMemory.cache.invalidate()
        permissionState.value = readPermissionState(context)
    }
    val folderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        connectingFolder = true
        scope.launch {
            val result = withContext(Dispatchers.IO) { runCatching { storage.connect(uri) } }
            connectingFolder = false
            folderConnected = storage.isConnected()
            result.onSuccess {
                GallerySnapshotMemory.cache.invalidate()
            }.onFailure {
                selectedFolderPath = runCatching {
                    DocumentsContract.getTreeDocumentId(uri).substringAfter(':')
                }.getOrDefault("선택한 폴더")
                showFolderGuide = true
            }
        }
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        folderConnected = storage.isConnected()
    }
    if (state.ready && folderConnected) {
        content()
        return
    }
    val colors = LocalDDZColor.current
    val typography = LocalDDZTypography.current
    Surface(Modifier.fillMaxSize(), color = colors.Background) {
        Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = androidx.compose.ui.Alignment.Center) {
            Text("DZlog", style = typography.ScreenTitle, color = colors.PrimaryDark)
        }
    }
    if (!showFolderGuide) DDZContentDialog(
        title = "필요한 설정을 완료해 주세요",
        onDismiss = { context.activity()?.finish() },
        properties = DialogProperties(dismissOnClickOutside = false, dismissOnBackPress = true),
        content = {
            Column(Modifier.heightIn(max = 380.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("사진 촬영과 갤러리·폴더 관리를 위해 아래 설정이 필요해요.", style = typography.Body, color = colors.TextSecondary)
                PermissionStatusCards(state)
                PermissionCard(
                    "폴더 관리",
                    if (folderConnected) "연결됨" else "연결 필요",
                    if (folderConnected) "Pictures / DZlog 폴더가 연결되어 있어요."
                    else "폴더 삭제·이동·이름 변경에 사용해요. 한 번만 연결하면 돼요.",
                )
                if (state.photos != GalleryMediaAccess.FULL) {
                    Text("사진 접근은 ‘모두 허용’으로 설정해 주세요. 일부 사진만 허용하면 갤러리 목록이 제한돼요.", style = typography.Caption, color = colors.TextSecondary)
                }
            }
        },
        actions = {
            DDZButton(if (state.ready) "폴더 연결" else "권한 설정", onClick = {
                if (!requesting && !connectingFolder) {
                    if (state.ready) {
                        selectedFolderPath = null
                        showFolderGuide = true
                        return@DDZButton
                    }
                    val missing = buildList {
                        if (!state.camera) add(Manifest.permission.CAMERA)
                        if (state.photos != GalleryMediaAccess.FULL) addAll(GalleryMediaAccessPolicy.requiredPermissions())
                    }
                    val neverRequested = missing.any { !preferences.getBoolean(it, false) }
                    if (neverRequested) {
                        preferences.edit().apply { missing.forEach { putBoolean(it, true) } }.apply()
                        requesting = true
                        requester.launch(missing.toTypedArray())
                    } else openAppPermissionSettings(context)
                }
            }, modifier = Modifier.fillMaxWidth(), enabled = !requesting && !connectingFolder)
        },
    )
    else {
        GalleryFolderAccessGuide(
            selectedPath = selectedFolderPath,
            showCancelAction = false,
            onCancel = { showFolderGuide = false },
            onSelect = {
                showFolderGuide = false
                folderPicker.launch(dzlogFolderInitialUri())
            },
        )
    }
}

@Composable
private fun PermissionStatusCards(state: RequiredPermissionState) {
    val rows = listOf(
        Triple("카메라", if (state.camera) "허용됨" else "허용 필요", "사진 촬영에 사용해요."),
        Triple("사진 접근", when (state.photos) {
            GalleryMediaAccess.FULL -> "전체 허용"
            GalleryMediaAccess.SELECTED_ONLY -> "일부 허용"
            GalleryMediaAccess.NOT_GRANTED -> "허용 안 됨"
        }, "저장된 사진을 갤러리에서 모두 확인하려면 필요해요."),
    ).sortedBy { if (it.first == "카메라") state.camera else state.photos == GalleryMediaAccess.FULL }
    rows.forEach { (title, status, detail) -> PermissionCard(title, status, detail) }
}

@Composable
private fun PermissionCard(title: String, status: String, detail: String, content: @Composable () -> Unit = {}) {
    val colors = LocalDDZColor.current
    val typography = LocalDDZTypography.current
    val spacing = LocalDDZSpacing.current
    Surface(Modifier.fillMaxWidth(), color = colors.SurfaceSoft, shape = MaterialTheme.shapes.medium) {
        Column(Modifier.padding(spacing.controlGap), verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(when (title) { "카메라" -> Icons.Default.CameraAlt; "사진 접근" -> Icons.Default.Image; else -> Icons.Default.Folder }, null, tint = colors.PrimaryDark)
                Text(title, Modifier.weight(1f), style = typography.SettingLabel, color = colors.TextPrimary)
                Text(status, style = typography.Caption, color = if (status == "허용됨" || status == "전체 허용" || status == "연결됨") colors.SelectedDark else colors.PrimaryDark)
            }
            Text(detail, style = typography.Caption, color = colors.TextSecondary)
            content()
        }
    }
}

@Composable
internal fun PermissionManagementScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val state by rememberPermissionState()
    val storage = remember(context) { GalleryFolderStorage(context) }
    var connected by remember { mutableStateOf(storage.isConnected()) }
    var showGuide by remember { mutableStateOf(false) }
    var selectedPath by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { connected = storage.isConnected() }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            busy = true
            scope.launch {
                val result = withContext(Dispatchers.IO) { runCatching { storage.connect(uri) } }
                busy = false
                connected = storage.isConnected()
                result.onSuccess { GallerySnapshotMemory.cache.invalidate() }.onFailure {
                    selectedPath = runCatching { android.provider.DocumentsContract.getTreeDocumentId(uri).substringAfter(':') }.getOrDefault("선택한 폴더")
                    showGuide = true
                }
            }
        }
    }
    BackHandler { if (!busy) onBack() }
    val colors = LocalDDZColor.current
    val typography = LocalDDZTypography.current
    Scaffold(containerColor = colors.Background, topBar = { DDZTopBar("권한 관리", onBack = { if (!busy) onBack() }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(LocalDDZSpacing.current.screenPadding), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("현재 권한과 폴더 연결 상태를 확인해요.", style = typography.Body, color = colors.TextSecondary)
            PermissionStatusCards(state)
            PermissionCard("폴더 관리", if (connected) "연결됨" else "필요할 때 연결",
                if (connected) "Pictures / DZlog 폴더가 연결되어 있어요." else "폴더 삭제·이동·이름 변경 시 연결해요.") {
                DDZButton(if (connected) "연결 변경" else "폴더 연결", { selectedPath = null; showGuide = true }, Modifier.fillMaxWidth(), style = DDZButtonStyle.Secondary, enabled = !busy)
            }
            Text("권한은 휴대폰 설정에서 변경할 수 있어요.", style = typography.Caption, color = colors.TextSecondary)
            DDZButton("앱 권한 설정 열기", { openAppPermissionSettings(context) }, Modifier.fillMaxWidth(), style = DDZButtonStyle.Secondary, enabled = !busy)
        }
    }
    if (showGuide) GalleryFolderAccessGuide(onCancel = { showGuide = false }, onSelect = {
        showGuide = false
        picker.launch(dzlogFolderInitialUri())
    }, selectedPath = selectedPath)
}
