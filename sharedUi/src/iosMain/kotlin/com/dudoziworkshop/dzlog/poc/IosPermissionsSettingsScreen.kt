@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
package com.dudoziworkshop.dzlog.poc

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.material3.Scaffold
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
import com.dudoziworkshop.dzlog.ui.common.DDZTopBar
import com.dudoziworkshop.dzlog.ui.theme.DDZColor
import com.dudoziworkshop.dzlog.ui.theme.DDZSpacing
import com.dudoziworkshop.dzlog.ui.theme.DDZTypography
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.Photos.PHAccessLevelReadWrite
import platform.Photos.PHAuthorizationStatusAuthorized
import platform.Photos.PHAuthorizationStatusDenied
import platform.Photos.PHAuthorizationStatusLimited
import platform.Photos.PHAuthorizationStatusNotDetermined
import platform.Photos.PHAuthorizationStatusRestricted
import platform.Photos.PHPhotoLibrary

internal enum class IosAccess(val label: String) {
    UNKNOWN("확인 중"), UNDETERMINED("요청 전"), DENIED("거부됨"), RESTRICTED("제한됨"),
    LIMITED("선택한 사진만"), ALLOWED("허용됨");

    val readable get() = this == ALLOWED || this == LIMITED
}

internal data class IosPermissions(val camera: IosAccess, val photos: IosAccess, val photoScopeVersion: Int = 0) {
    val captureAllowed get() = camera == IosAccess.ALLOWED && photos.readable
}

private fun currentPhotosAccess(): IosAccess = when (PHPhotoLibrary.authorizationStatusForAccessLevel(PHAccessLevelReadWrite)) {
    PHAuthorizationStatusAuthorized -> IosAccess.ALLOWED
    PHAuthorizationStatusLimited -> IosAccess.LIMITED
    PHAuthorizationStatusDenied -> IosAccess.DENIED
    PHAuthorizationStatusRestricted -> IosAccess.RESTRICTED
    PHAuthorizationStatusNotDetermined -> IosAccess.UNDETERMINED
    else -> IosAccess.UNKNOWN
}

internal fun openIosSystemSettings() {
    NSNotificationCenter.defaultCenter.postNotificationName("dzlog.openSystemSettings", null)
}

private fun refreshPermissions() {
    NSNotificationCenter.defaultCenter.postNotificationName("dzlog.permissionStatusRequest", null)
}

@Composable
internal fun rememberIosPermissions(
    initialCamera: IosAccess = IosAccess.UNKNOWN,
): IosPermissions {
    var state by remember { mutableStateOf(IosPermissions(initialCamera, currentPhotosAccess())) }
    DisposableEffect(Unit) {
        val center = NSNotificationCenter.defaultCenter
        val observer = center.addObserverForName("dzlog.permissionStatusUpdated", null, NSOperationQueue.mainQueue) { event ->
            val camera = when (event?.`object` as? String) {
                "allowed" -> IosAccess.ALLOWED
                "denied" -> IosAccess.DENIED
                "restricted" -> IosAccess.RESTRICTED
                "undetermined" -> IosAccess.UNDETERMINED
                else -> IosAccess.UNKNOWN
            }
            val photos = currentPhotosAccess()
            val scopeMayDiffer = state.photos != photos || photos == IosAccess.LIMITED
            val version = state.photoScopeVersion + if (scopeMayDiffer) 1 else 0
            state = IosPermissions(camera, photos, version)
        }
        refreshPermissions()
        onDispose { center.removeObserver(observer) }
    }
    return state
}

@Composable
internal fun PermissionCard(camera: Boolean, access: IosAccess) {
    val title = if (camera) "카메라 권한" else "사진 권한"
    val explanation = when {
        access == IosAccess.RESTRICTED -> "기기의 사용 제한 설정을 확인해 주세요."
        access == IosAccess.DENIED -> "DZlog 설정에서 ${if (camera) "카메라" else "사진"} 접근을 허용해 주세요. 앱 목록이 열리면 DZlog를 선택하세요."
        access == IosAccess.LIMITED -> "DZlog가 접근할 수 있는 사진을 선택하거나 변경할 수 있어요."
        camera -> "사진과 기록표를 촬영할 때 사용해요."
        else -> "촬영한 사진을 저장하고 갤러리에서 확인할 때 사용해요."
    }
    Surface(Modifier.fillMaxWidth(), color = DDZColor.Surface, shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(title, Modifier.weight(1f), style = DDZTypography.SettingLabel, color = DDZColor.PrimaryDark)
                Text(if (!camera && access == IosAccess.ALLOWED) "전체 허용" else access.label,
                    style = DDZTypography.Caption, color = DDZColor.TextSecondary)
            }
            Text(explanation, style = DDZTypography.Body, color = DDZColor.TextSecondary)
            when (access) {
                IosAccess.UNDETERMINED, IosAccess.LIMITED -> TextButton(onClick = {
                    val action = if (camera) "camera" else if (access == IosAccess.LIMITED) "selection" else "photos"
                    NSNotificationCenter.defaultCenter.postNotificationName("dzlog.permissionAction", action)
                }) {
                    Text(if (camera) "카메라 권한 허용하기" else if (access == IosAccess.LIMITED) "선택한 사진 변경" else "사진 접근 허용하기")
                }
                IosAccess.DENIED -> TextButton(onClick = ::openIosSystemSettings) { Text("iPhone 설정 열기") }
                else -> Unit
            }
        }
    }
}

@Composable
internal fun IosPermissionsSettingsScreen(onBack: () -> Unit) {
    val permission = rememberIosPermissions()
    Scaffold(containerColor = DDZColor.Background,
        topBar = { DDZTopBar(title = "권한 관리", onBack = onBack) },
    ) { innerPadding ->
        Column(Modifier.fillMaxSize().padding(innerPadding).verticalScroll(rememberScrollState())
            .padding(horizontal = DDZSpacing.screenPadding, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            PermissionCard(camera = true, access = permission.camera)
            PermissionCard(camera = false, access = permission.photos)
            Text("촬영 결과는 iPhone 사진 보관함에 저장돼요. DZlog 안에서는 프로젝트와 폴더별로 확인할 수 있어요.",
                style = DDZTypography.Caption, color = DDZColor.TextSecondary)
            TextButton(onClick = ::refreshPermissions) { Text("상태 새로고침") }
            if (permission.camera != IosAccess.DENIED && permission.photos != IosAccess.DENIED) {
                TextButton(onClick = ::openIosSystemSettings) { Text("iPhone 설정 열기") }
            }
        }
    }
}
