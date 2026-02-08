package com.example.dzlog.ui.camera.preview

import android.content.Context
import androidx.compose.runtime.Stable
import androidx.lifecycle.LifecycleOwner
import com.example.dzlog.domain.model.CaptureAspect
import com.example.dzlog.domain.model.ContinuousPreviewMode
import com.example.dzlog.domain.model.SaveMode
import com.example.dzlog.domain.model.TableTemplateState
import com.example.dzlog.domain.model.WatermarkTableAnchor
import com.example.dzlog.domain.table.TableResolver
import kotlinx.coroutines.CoroutineScope
import java.util.Date

@Stable
internal data class WatermarkUiArgs(
    val anchor: WatermarkTableAnchor,
    val tableWidthRatio: Int,
    val tableHeightRatio: Int,
    val offsetXRatio: Int,
    val offsetYRatio: Int,
    val bgAlpha: Int,
    val bgStyle: Int,
    val labelScale: Int,
    val valueScale: Int
)

/**
 * CameraPreviewAreaArgs
 * - CameraPreviewArea에 필요한 입력을 묶어서 전달 실수(누락/순서)를 줄임
 * - 동작 불변(값 묶음만 수행)
 */
@Stable
internal data class CameraPreviewAreaArgs(
    val context: Context,
    val lifecycleOwner: LifecycleOwner,
    val scope: CoroutineScope,
    val captureAspect: CaptureAspect,
    val saveMode: SaveMode,
    val continuousPreviewMode: ContinuousPreviewMode,
    val counterDigits: Int,
    val dateFormat: String,
    val timeFormat: String,
    val fnDelim: String,
    val tableTemplateState: TableTemplateState,
    val tableResolver: TableResolver,
    val now: Date,
    val showWmPreview: Boolean,
    val watermarkUi: WatermarkUiArgs
)
