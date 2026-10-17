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
import androidx.compose.foundation.BorderStroke
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

internal enum class RequiredSetupStep { CAMERA, PHOTOS, FOLDER }

internal fun nextRequiredSetupStep(
    state: RequiredPermissionState,
    folderConnected: Boolean,
): RequiredSetupStep? = when {
    !state.camera -> RequiredSetupStep.CAMERA
    state.photos != GalleryMediaAccess.FULL -> RequiredSetupStep.PHOTOS
    !folderConnected -> RequiredSetupStep.FOLDER
    else -> null
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
internal fun RequiredPermissionsGate(
    onboarding: Boolean = false,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val permissionState = rememberPermissionState()
    val state by permissionState
    val storage = remember(context) { GalleryFolderStorage(context) }
    var folderConnected by remember { mutableStateOf(storage.isConnected()) }
    var showFolderGuide by remember { mutableStateOf(false) }
    var selectedFolderPath by remember { mutableStateOf<String?>(null) }
    var folderGuideNotice by remember { mutableStateOf<String?>(null) }
    var cameraRequestAttempted by remember { mutableStateOf(false) }
    var photoRequestAttempted by remember { mutableStateOf(false) }
    var requesting by remember { mutableStateOf(false) }
    var connectingFolder by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val requester = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        requesting = false
        GallerySnapshotMemory.cache.invalidate()
        permissionState.value = readPermissionState(context)
    }
    val folderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri == null) {
            selectedFolderPath = null
            folderGuideNotice = "폴더 연결을 완료해야 DZlog를 사용할 수 있어요."
            showFolderGuide = !onboarding
            return@rememberLauncherForActivityResult
        }
        connectingFolder = true
        scope.launch {
            val result = withContext(Dispatchers.IO) { runCatching { storage.connect(uri) } }
            connectingFolder = false
            folderConnected = storage.isConnected()
            result.onSuccess {
                folderGuideNotice = null
                GallerySnapshotMemory.cache.invalidate()
            }.onFailure {
                selectedFolderPath = runCatching {
                    DocumentsContract.getTreeDocumentId(uri).substringAfter(':')
                }.getOrDefault("선택한 폴더")
                folderGuideNotice = "선택한 위치가 달라요. Pictures/DZlog 폴더 자체를 선택해 주세요."
                showFolderGuide = !onboarding
            }
        }
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        folderConnected = storage.isConnected()
    }
    LaunchedEffect(state.camera) {
        if (state.camera) cameraRequestAttempted = false
    }
    LaunchedEffect(state.photos) {
        if (state.photos == GalleryMediaAccess.FULL) photoRequestAttempted = false
    }
    val currentStep = nextRequiredSetupStep(state, folderConnected)
    if (currentStep == null) {
        content()
        return
    }
    if (!showFolderGuide) {
        RequiredPermissionSetupScreen(
            onboarding = onboarding,
            state = state,
            folderConnected = folderConnected,
            currentStep = currentStep,
            cameraRequestAttempted = cameraRequestAttempted,
            photoRequestAttempted = photoRequestAttempted,
            notice = folderGuideNotice,
            working = requesting || connectingFolder,
            onAction = {
                if (!requesting && !connectingFolder) {
                    when (currentStep) {
                        RequiredSetupStep.CAMERA -> if (cameraRequestAttempted) {
                            openAppPermissionSettings(context)
                        } else {
                            cameraRequestAttempted = true
                            requesting = true
                            runCatching { requester.launch(arrayOf(Manifest.permission.CAMERA)) }
                                .onFailure { requesting = false; openAppPermissionSettings(context) }
                        }
                        RequiredSetupStep.PHOTOS -> if (photoRequestAttempted ||
                            state.photos == GalleryMediaAccess.SELECTED_ONLY) {
                            openAppPermissionSettings(context)
                        } else {
                            photoRequestAttempted = true
                            requesting = true
                            runCatching { requester.launch(GalleryMediaAccessPolicy.requiredPermissions()) }
                                .onFailure { requesting = false; openAppPermissionSettings(context) }
                        }
                        RequiredSetupStep.FOLDER -> {
                            selectedFolderPath = null
                            folderGuideNotice = null
                            if (onboarding) {
                                // Android's document picker opens directly from the
                                // in-onboarding permission page (no extra DZlog dialog).
                                runCatching { folderPicker.launch(dzlogFolderInitialUri()) }
                                    .onFailure {
                                        folderGuideNotice =
                                            "폴더 선택 화면을 열지 못했어요. 다시 시도해 주세요."
                                    }
                            } else {
                                showFolderGuide = true
                            }
                        }
                    }
                }
            },
        )
    }
    else {
        GalleryFolderAccessGuide(
            selectedPath = selectedFolderPath,
            showCancelAction = false,
            required = true,
            notice = folderGuideNotice,
            onCancel = { context.activity()?.finish() },
            onSelect = {
                showFolderGuide = false
                runCatching { folderPicker.launch(dzlogFolderInitialUri()) }
                    .onFailure {
                        folderGuideNotice = "폴더 선택 화면을 열지 못했어요. 잠시 후 다시 시도해 주세요."
                        showFolderGuide = true
                    }
            },
        )
    }
}

@Composable
internal fun PermissionStatusCards(
    state: RequiredPermissionState,
    activeStep: RequiredSetupStep? = null,
    compact: Boolean = false,
) {
    val rows = listOf(
        Triple("카메라", if (state.camera) "허용됨" else "허용 필요", "사진 촬영에 사용해요."),
        Triple("사진 접근", when (state.photos) {
            GalleryMediaAccess.FULL -> "전체 허용"
            GalleryMediaAccess.SELECTED_ONLY -> "일부 허용"
            GalleryMediaAccess.NOT_GRANTED -> "허용 안 됨"
        }, "저장된 사진을 갤러리에서 모두 확인하려면 필요해요."),
    )
    rows.forEachIndexed { index, (title, status, detail) ->
        PermissionCard(
            title,
            status,
            detail,
            active = (title == "카메라" && activeStep == RequiredSetupStep.CAMERA) ||
                (title == "사진 접근" && activeStep == RequiredSetupStep.PHOTOS),
            stepNumber = if (activeStep != null) index + 1 else null,
            compact = compact,
        )
    }
}

@Composable
internal fun PermissionCard(
    title: String,
    status: String,
    detail: String,
    active: Boolean = false,
    stepNumber: Int? = null,
    compact: Boolean = false,
    content: @Composable () -> Unit = {},
) {
    val colors = LocalDDZColor.current
    val typography = LocalDDZTypography.current
    val spacing = LocalDDZSpacing.current
    Surface(
        Modifier.fillMaxWidth(),
        color = if (active) colors.SelectedSoft else colors.SurfaceSoft,
        shape = MaterialTheme.shapes.medium,
        border = if (active) BorderStroke(1.dp, colors.SelectedDark) else null,
    ) {
        Column(
            Modifier.padding(if (compact) 8.dp else spacing.controlGap),
            verticalArrangement = Arrangement.spacedBy(if (compact) 3.dp else spacing.xs),
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                stepNumber?.let { Text("$it", style = typography.SettingLabel, color = colors.SelectedDark) }
                Icon(when (title) { "카메라" -> Icons.Default.CameraAlt; "사진 접근" -> Icons.Default.Image; else -> Icons.Default.Folder }, null, tint = colors.PrimaryDark)
                Text(title, Modifier.weight(1f), style = typography.SettingLabel, color = colors.TextPrimary)
                Text(status, style = typography.Caption, color = if (status == "허용됨" || status == "전체 허용" || status == "연결됨") colors.SelectedDark else colors.PrimaryDark)
            }
            Text(detail, style = typography.Caption, color = colors.TextSecondary,
                maxLines = if (compact) 2 else Int.MAX_VALUE)
            content()
        }
    }
}

@Composable
internal fun CurrentSetupGuide(
    step: RequiredSetupStep,
    photoAccess: GalleryMediaAccess,
    cameraRequestAttempted: Boolean,
    photoRequestAttempted: Boolean,
) {
    val colors = LocalDDZColor.current
    val typography = LocalDDZTypography.current
    val spacing = LocalDDZSpacing.current
    val (title, detail) = when (step) {
        RequiredSetupStep.CAMERA -> if (cameraRequestAttempted) {
            "1단계 · 카메라 권한 확인" to "권한 창에서 허용하지 않았어요. 아래 버튼을 누르면 휴대폰 설정이 열려요. 카메라를 허용한 뒤 DZlog로 돌아와 주세요."
        } else {
            "1단계 · 카메라 권한" to "아래 버튼을 누르면 Android 권한 창이 열려요. 사진을 촬영하려면 ‘허용’을 선택해 주세요."
        }
        RequiredSetupStep.PHOTOS -> if (photoRequestAttempted || photoAccess == GalleryMediaAccess.SELECTED_ONLY) {
            "2단계 · 사진 접근 확인" to "사진 접근은 ‘모두 허용’이 필요해요. 아래 버튼으로 휴대폰 설정을 열고 사진 및 동영상을 모두 허용해 주세요."
        } else {
            "2단계 · 사진 접근" to "아래 버튼을 누른 뒤 ‘모든 사진 허용’을 선택해 주세요. 최근 촬영과 DZlog 갤러리를 빠짐없이 보여주는 데 사용해요."
        }
        RequiredSetupStep.FOLDER -> "3단계 · DZlog 폴더 연결" to
            "마지막으로 Pictures 안의 DZlog 폴더를 연결해 주세요. 폴더를 이동·이름 변경·삭제할 때 사용하며, 연결만으로 사진이 변경되지는 않아요."
    }
    Surface(color = colors.SelectedSoft, shape = MaterialTheme.shapes.medium) {
        Column(Modifier.fillMaxWidth().padding(spacing.controlGap), verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
            Text(title, style = typography.SettingLabel, color = colors.SelectedDark)
            Text(detail, style = typography.Caption, color = colors.TextSecondary)
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
            PermissionCard("폴더 관리", if (connected) "연결됨" else "연결 필요",
                if (connected) "Pictures / DZlog 폴더가 연결되어 있어요." else "DZlog를 사용하려면 Pictures/DZlog 폴더 연결이 필요해요.") {
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
