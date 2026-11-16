@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
package com.dudoziworkshop.dzlog.poc

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dudoziworkshop.dzlog.ui.common.DDZTopBar
import com.dudoziworkshop.dzlog.ui.theme.DDZColor
import com.dudoziworkshop.dzlog.ui.theme.DDZSpacing
import com.dudoziworkshop.dzlog.ui.theme.DDZTypography
import platform.AVFoundation.AVAuthorizationStatusAuthorized
import platform.AVFoundation.AVAuthorizationStatusDenied
import platform.AVFoundation.AVAuthorizationStatusNotDetermined
import platform.AVFoundation.AVAuthorizationStatusRestricted
import platform.AVFoundation.AVCaptureDevice
import platform.AVFoundation.AVMediaTypeVideo
import platform.Foundation.NSNotificationCenter
import platform.Photos.PHAccessLevelReadWrite
import platform.Photos.PHAuthorizationStatusAuthorized
import platform.Photos.PHAuthorizationStatusDenied
import platform.Photos.PHAuthorizationStatusLimited
import platform.Photos.PHAuthorizationStatusNotDetermined
import platform.Photos.PHAuthorizationStatusRestricted
import platform.Photos.PHPhotoLibrary

@Composable
internal fun IosPermissionsSettingsScreen(onBack: () -> Unit) {
    var refreshRevision by remember { mutableIntStateOf(0) }
    val cameraStatus = remember(refreshRevision) { iosCameraPermissionLabel() }
    val photosStatus = remember(refreshRevision) { iosPhotosPermissionLabel() }

    Scaffold(
        containerColor = DDZColor.Background,
        topBar = { DDZTopBar(title = "권한 관리", onBack = onBack) },
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = DDZSpacing.screenPadding, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            PermissionStatusRow("카메라 권한", cameraStatus,
                "사진 촬영과 초점·줌 제어에 사용합니다.")
            HorizontalDivider(color = DDZColor.Border)
            PermissionStatusRow("사진 권한", photosStatus,
                "촬영 결과 저장, 갤러리 조회와 직전 촬영 삭제에 사용합니다.")
            Text(
                "권한 요청은 카메라 또는 사진 기능을 처음 사용할 때 표시됩니다. " +
                    "거부한 권한은 iPhone 설정에서 변경할 수 있습니다.",
                style = DDZTypography.Body,
                color = DDZColor.TextSecondary,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Button(onClick = { refreshRevision += 1 }) { Text("상태 새로고침") }
                Button(onClick = {
                    NSNotificationCenter.defaultCenter.postNotificationName(
                        "dzlog.openSystemSettings", null,
                    )
                }) { Text("iPhone 설정 열기") }
            }
        }
    }
}

@Composable
private fun PermissionStatusRow(title: String, status: String, explanation: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = DDZTypography.ScreenTitle, color = DDZColor.PrimaryDark)
            Text(explanation, style = DDZTypography.Caption, color = DDZColor.TextSecondary)
        }
        Text(status, style = DDZTypography.Body, color = DDZColor.TextPrimary)
    }
}

private fun iosCameraPermissionLabel(): String = when (
    AVCaptureDevice.authorizationStatusForMediaType(AVMediaTypeVideo)
) {
    AVAuthorizationStatusAuthorized -> "허용됨"
    AVAuthorizationStatusDenied -> "거부됨"
    AVAuthorizationStatusRestricted -> "제한됨"
    AVAuthorizationStatusNotDetermined -> "요청 전"
    else -> "확인 필요"
}

private fun iosPhotosPermissionLabel(): String = when (
    PHPhotoLibrary.authorizationStatusForAccessLevel(PHAccessLevelReadWrite)
) {
    PHAuthorizationStatusAuthorized -> "전체 허용"
    PHAuthorizationStatusLimited -> "선택한 사진만"
    PHAuthorizationStatusDenied -> "거부됨"
    PHAuthorizationStatusRestricted -> "제한됨"
    PHAuthorizationStatusNotDetermined -> "요청 전"
    else -> "확인 필요"
}
