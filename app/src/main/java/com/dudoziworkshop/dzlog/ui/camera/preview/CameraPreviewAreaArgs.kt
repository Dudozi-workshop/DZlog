package com.dudoziworkshop.dzlog.ui.camera.preview

import android.content.Context
import androidx.compose.runtime.Stable
import androidx.lifecycle.LifecycleOwner
import com.dudoziworkshop.dzlog.domain.model.CaptureAspect
import com.dudoziworkshop.dzlog.domain.model.ContinuousPreviewMode
import com.dudoziworkshop.dzlog.domain.model.PhotoQualityMode
import com.dudoziworkshop.dzlog.domain.model.SaveMode
import com.dudoziworkshop.dzlog.domain.model.TableTemplateState
import com.dudoziworkshop.dzlog.domain.model.WatermarkTableAnchor
import com.dudoziworkshop.dzlog.domain.table.TableResolver
import kotlinx.coroutines.CoroutineScope
import java.util.Date

@Stable
internal data class WatermarkUiArgs(
    val anchor: WatermarkTableAnchor,
    val tableWidthRatio: Int,
    val tableHeightRatio: Int,
    val offsetXRatio: Int,
    val offsetYRatio: Int,
    val boundsOffsetX10000: Int,
    val boundsOffsetY10000: Int,
    val bgAlpha: Int,
    val bgStyle: Int,
    val valueScale: Int,
    val textColorMode: Int,
    val manualTextColor: Int,
    val textAlign: Int,
    val wmGridEnabled: Boolean,
    val rotationCwDeg: Int
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
    val photoQualityMode: PhotoQualityMode,
    val counterDigits: Int,
    val dateFormat: String,
    val timeFormat: String,
    val fnDelim: String,
    /**
     * 단일 소스 카운터(프리뷰 표기용).
     * null이면 카운터 readback 동기화 전 상태로 간주하고 COUNTER 표시는 보류한다.
     */
    val scopeNextCounter: Int?,
    /**
     * 문구 진행 커서(순환문구 전용). 카운터 seed(scopeNextCounter)와 절대 혼용하지 않는다.
     */
    val phraseProgressCursor: Int,
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
    val shutterButtonTopY: Float?,
    val safeTopY: Float?,
    val safeBottomY: Float?,
    val usableVerticalMarginPx: Float,
    val onUsableVerticalRatioChange: (Float, Float) -> Unit,
    val onWatermarkOffsetRatioPreview: (Int, Int) -> Unit,
    val onWatermarkOffsetRatioCommit: (Int, Int) -> Unit,
    val onWatermarkBoundsOffset10000Preview: (Int, Int) -> Unit,
    val onWatermarkBoundsOffset10000Commit: (Int, Int) -> Unit,
    val onOpenTableEditor: () -> Unit,
    val watermarkUi: WatermarkUiArgs
)
