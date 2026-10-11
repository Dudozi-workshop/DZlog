package com.dudoziworkshop.dzlog.ui.navigation

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.dudoziworkshop.dzlog.feature.log.policy.GalleryMediaAccess
import com.dudoziworkshop.dzlog.feature.log.policy.GalleryMediaAccessPolicy

/**
 * Ask once, early, rather than interrupting every gallery visit.
 * Skipping is allowed: the camera remains usable without photo-read access.
 */
@Composable
internal fun GalleryPermissionOnboarding() {
    val context = LocalContext.current
    val preferences = remember(context) {
        context.getSharedPreferences("dzlog_gallery_permissions", Context.MODE_PRIVATE)
    }
    var shown by remember { mutableStateOf(preferences.getBoolean("onboarding_done", false)) }
    val requester = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        preferences.edit().putBoolean("onboarding_done", true).apply()
        shown = true
    }
    if (!shown && GalleryMediaAccessPolicy.state(context) == GalleryMediaAccess.FULL) {
        preferences.edit().putBoolean("onboarding_done", true).apply()
        shown = true
    }
    if (!shown) {
        AlertDialog(
            onDismissRequest = {
                preferences.edit().putBoolean("onboarding_done", true).apply()
                shown = true
            },
            title = { Text("DZlog 사진 접근") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("이전에 촬영한 사진을 갤러리에서 바로 불러올 수 있도록 사진 접근 권한을 요청합니다.")
                    Text("지금 건너뛰어도 카메라는 사용할 수 있습니다. 새 폴더는 갤러리에서 바로 만들 수 있습니다.")
                }
            },
            confirmButton = {
                Button(onClick = {
                    requester.launch(GalleryMediaAccessPolicy.requiredPermissions())
                }) { Text("사진 접근 허용") }
            },
            dismissButton = {
                TextButton(onClick = {
                    preferences.edit().putBoolean("onboarding_done", true).apply()
                    shown = true
                }) { Text("나중에") }
            },
        )
    }
}
