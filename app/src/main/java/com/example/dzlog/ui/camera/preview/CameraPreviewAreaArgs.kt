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
    val valueScale: Int,
    val textColorMode: Int,
    val manualTextColor: Int,
    val textAlign: Int
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
    /**
     * 단일 소스 카운터(프리뷰 표기용). COUNTER 셀 ON/OFF에 의해 프리뷰 숫자가 흔들리지 않게 하기 위함.
     */
    val scopeNextCounter: Int,
    val tableTemplateState: TableTemplateState,
    val tableResolver: TableResolver,
    val now: Date,
    val showWmPreview: Boolean,
    val showGrid: Boolean,
    val zoomRatioTenths: Int,
    val maxZoomTenths: Int,
    val onActualZoomTenthsChange: (Int) -> Unit,
    val onRequestedZoomTenthsCommit: (Int) -> Unit,
    val onMaxZoomTenthsChange: (Int) -> Unit,
    val settingsButtonBottomY: Float?,
    val shutterButtonTopY: Float?,
    val safeTopY: Float?,
    val safeBottomY: Float?,
    val usableVerticalMarginPx: Float,
    val onUsableVerticalRatioChange: (Float, Float) -> Unit,
    val onWatermarkOffsetRatioPreview: (Int, Int) -> Unit,
    val onWatermarkOffsetRatioCommit: (Int, Int) -> Unit,
    val onOpenTableEditor: () -> Unit,
    val watermarkUi: WatermarkUiArgs
)
