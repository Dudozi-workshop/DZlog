package com.dudoziworkshop.dzlog.poc

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.dudoziworkshop.dzlog.data.media.CaptureMediaAvailability
import com.dudoziworkshop.dzlog.data.media.PhotoKitCaptureMediaRepository
import com.dudoziworkshop.dzlog.data.media.SavedCaptureMedia
import com.dudoziworkshop.dzlog.domain.media.CaptureMediaRole
import com.dudoziworkshop.dzlog.ui.theme.DDZColor
import kotlinx.coroutines.launch
import org.jetbrains.skia.Image as SkiaImage

/** Read-only M3-A bridge: the existing Android gallery and its database are untouched. */
@Composable
internal fun IosPhotoKitGalleryScreen(
    repository: PhotoKitCaptureMediaRepository,
    onBack: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var photos by remember { mutableStateOf<List<SavedCaptureMedia>>(emptyList()) }
    var selectedId by remember { mutableStateOf<String?>(null) }
    var image by remember { mutableStateOf<ImageBitmap?>(null) }
    var message by remember { mutableStateOf("사진 확인 중") }
    var loadError by remember { mutableStateOf<String?>(null) }

    fun refresh() {
        scope.launch {
            try {
                val fetched = repository.listSavedAssets().asReversed()
                photos = fetched
                if (selectedId == null || fetched.none { it.asset.assetId == selectedId }) {
                    val latestCapture = fetched.firstOrNull()?.captureId
                    selectedId = fetched.firstOrNull {
                        it.captureId == latestCapture && it.asset.target.role == CaptureMediaRole.RESULT
                    }?.asset?.assetId ?: fetched.firstOrNull()?.asset?.assetId
                }
                message = if (fetched.isEmpty()) "저장된 사진이 없습니다." else "저장 사진 ${fetched.size}개"
                loadError = null
            } catch (error: Throwable) {
                loadError = "갤러리 조회 실패: ${error.message}"
            }
        }
    }

    LaunchedEffect(Unit) { refresh() }
    LaunchedEffect(selectedId, photos) {
        image = null
        val selected = photos.firstOrNull { it.asset.assetId == selectedId } ?: return@LaunchedEffect
        if (selected.availability != CaptureMediaAvailability.PRESENT) {
            message = if (selected.availability == CaptureMediaAvailability.INACCESSIBLE)
                "사진 접근 권한이 없습니다." else "저장된 사진을 찾을 수 없습니다."
            return@LaunchedEffect
        }
        try {
            val bytes = repository.readAsset(selected.asset.assetId)
            image = bytes?.takeIf { it.isNotEmpty() }?.let {
                SkiaImage.makeFromEncoded(it).toComposeImageBitmap()
            }
            if (image == null) message = "사진 파일을 읽을 수 없습니다."
            else message = "사진 복원 완료"
        } catch (error: Throwable) {
            message = "사진 조회 실패: ${error.message}"
        }
    }

    Column(
        Modifier.fillMaxSize().background(DDZColor.Background)
            .safeDrawingPadding().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = onBack) { Text("뒤로가기") }
            Button(onClick = ::refresh) { Text("새로고침") }
        }
        Text("갤러리")
        Text("사진 ${photos.size}개")
        loadError?.let { Text(it) }
        Text(message)
        image?.let {
            Image(bitmap = it, contentDescription = "갤러리 사진 미리보기",
                modifier = Modifier.fillMaxWidth().height(190.dp), contentScale = ContentScale.Fit)
        }
        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(photos, key = { it.asset.assetId }) { entry ->
                val role = if (entry.asset.target.role == CaptureMediaRole.RESULT) "결과" else "원본"
                Column(Modifier.fillMaxWidth()
                    .clickable { selectedId = entry.asset.assetId }
                    .padding(8.dp)) {
                    Text("${entry.asset.target.displayName} · $role")
                    Text(entry.asset.target.relativePath)
                    Text(
                        when (entry.availability) {
                            CaptureMediaAvailability.PRESENT -> "사진 보관됨"
                            CaptureMediaAvailability.INACCESSIBLE -> "사진 접근 불가"
                            CaptureMediaAvailability.MISSING -> "사진 누락"
                        },
                    )
                }
            }
        }
    }
}
