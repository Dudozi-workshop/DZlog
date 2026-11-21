@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, androidx.compose.ui.ExperimentalComposeUiApi::class)
package com.dudoziworkshop.dzlog.poc

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitView
import androidx.compose.ui.viewinterop.UIKitInteropProperties
import androidx.compose.ui.viewinterop.UIKitInteropInteractionMode
import com.dudoziworkshop.dzlog.feature.capture.policy.*
import kotlinx.cinterop.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import platform.QuartzCore.CABasicAnimation
import platform.AVFoundation.*
import platform.CoreGraphics.*
import platform.Foundation.*
import platform.UIKit.*
import platform.darwin.NSObject

private const val FOCUS_RESULT_HOLD_MS = 1_200L
private const val FOCUS_REQUEST_TIMEOUT_MS = 2_000L
private const val FOCUS_FAILURE_FLASH_MS = 500L

/** One native surface keeps the table above the AVFoundation layer, including during gestures. */
@Composable
internal fun IosLiveTablePreview(
    modifier: Modifier,
    session: IosCameraCaptureSession,
    previewLayer: AVCaptureVideoPreviewLayer? = null,
    fixtureAspect: Double? = null,
    captureAspect: Double? = null,
    enabled: Boolean,
    showGrid: Boolean = false,
    showTable: Boolean = true,
    focusEnabled: Boolean = false,
    focusCollapseToken: Int = 0,
    minimumExposureBias: Float = 0f,
    maximumExposureBias: Float = 0f,
    onFocusAt: suspend (Double, Double, Boolean) -> Boolean = { _, _, _ -> false },
    onExposureLock: (Boolean) -> Boolean = { false },
    onExposureBias: (Float) -> Boolean = { false },
    autoCollapseFocus: Boolean = true,
    zoomEnabled: Boolean = false,
    currentZoom: Float = 1f,
    maximumZoom: Float = 1f,
    onZoomChange: (Float) -> Boolean = { false },
    onZoomFinished: (Float) -> Unit = {},
    revision: Int,
    onReady: (Boolean) -> Unit,
    onEditing: (Boolean) -> Unit,
    onSaved: () -> Unit,
    onError: (String) -> Unit,
) {
    val view = remember(session, previewLayer, fixtureAspect) {
        IosLiveTableView(previewLayer, fixtureAspect, captureAspect)
    }
    var focusInteraction by remember(view) { mutableIntStateOf(0) }
    var draggingExposure by remember(view) { mutableStateOf(false) }
    val waitingForFocus = view.waitingForFocus
    LaunchedEffect(view, autoCollapseFocus, focusInteraction, draggingExposure, waitingForFocus) {
        if (autoCollapseFocus && focusInteraction > 0 && !draggingExposure && !waitingForFocus) {
            delay(FOCUS_RESULT_HOLD_MS)
            view.collapseFocusControls()
        }
    }
    val placement = if (session.selectedDocument.saveMode == com.dudoziworkshop.dzlog.domain.model.SaveMode.ORIGINAL_ONLY)
        null else session.tablePlacement
    DisposableEffect(view) { onDispose { view.release() } }
    LaunchedEffect(view) {
        // Video dimensions can arrive after the first native layout. No assumed sensor ratio.
        repeat(50) {
            if (view.previewReady) return@LaunchedEffect
            delay(100)
            view.setNeedsLayout()
        }
        if (!view.hasViewport) onError("미리보기 사진 영역을 확인할 수 없습니다.")
    }
    UIKitView(factory = { view }, modifier = modifier,
        properties = UIKitInteropProperties(interactionMode = UIKitInteropInteractionMode.NonCooperative,
            isNativeAccessibilityEnabled = true),
        update = {
            it.updateCaptureAspect(captureAspect)
            it.bind(placement, enabled, showGrid, showTable, focusEnabled,
                focusCollapseToken, minimumExposureBias, maximumExposureBias,
                onFocusAt, onExposureLock, onExposureBias, { dragging ->
                    draggingExposure = dragging
                    focusInteraction += 1
                }, revision,
                zoomEnabled, currentZoom, maximumZoom, onZoomChange, onZoomFinished,
                session::renderLiveOverlay, session::savePlacement,
                onReady, onEditing, onSaved, onError)
        })
}

internal class IosLiveTableView(
    private val preview: AVCaptureVideoPreviewLayer?,
    private val fixtureAspect: Double?,
    private var captureAspect: Double?,
) : UIView(frame = CGRectMake(0.0, 0.0, 1.0, 1.0)) {
    private val image = UIImageView()
    private val gridLines = List(4) { UIView() }
    private val moveHandle = UIView()
    private val resizeHandle = UILabel()
    private val lockedColor = UIColor(red = 172.0 / 255, green = 219.0 / 255, blue = 114.0 / 255, alpha = 1.0)
    private val focusRing = UIView()
    private val focusLock = UIButton.buttonWithType(UIButtonTypeSystem)
    private val expandBrightness = UIButton.buttonWithType(UIButtonTypeCustom)
    private val exposureSlider = FocusExposureSlider()
    private val target = LiveTableGestureTarget(this)
    private val movePan = UIPanGestureRecognizer(target, NSSelectorFromString("move:"))
    private val resizePan = UIPanGestureRecognizer(target, NSSelectorFromString("resize:"))
    private val focusTap = UITapGestureRecognizer(target, NSSelectorFromString("focus:"))
    private val pinchZoom = UIPinchGestureRecognizer(target, NSSelectorFromString("pinchZoom:"))
    private var placement: IosCaptureTablePlacement? = null
    private var start: IosCaptureTablePlacement? = null
    private var enabled = false
    private var showGrid = false
    private var showTable = true
    private var focusEnabled = false
    private var focusCollapseToken = -1
    private var focusExpanded = false
    private val feedbackScope = MainScope()
    private var focusRequest: Job? = null
    var waitingForFocus by mutableStateOf(false)
        private set
    private var focusTimedOut = false
    private var exposureLocked = false
    private var awaitingLockedRefocusTap = false
    private var focusX = Float.NaN
    private var focusY = Float.NaN
    private var minimumExposureBias = 0f
    private var maximumExposureBias = 0f
    private var focusAt: (suspend (Double, Double, Boolean) -> Boolean)? = null
    private var exposureLock: ((Boolean) -> Boolean)? = null
    private var exposureBias: ((Float) -> Boolean)? = null
    private var focusInteraction: ((Boolean) -> Unit)? = null
    private var trackingExposure = false
    private var zoomEnabled = false
    private var currentZoom = 1f
    private var maximumZoom = 1f
    private var pinchStartZoom = 1f
    private var zoomWriter: ((Float) -> Boolean)? = null
    private var zoomCommit: ((Float) -> Unit)? = null
    private var revision = -1
    private var contentAspect: Float? = null
    private var dirty = true
    private var viewport = IosTableBounds(0f, 0f, 0f, 0f, 0f, 0f)
    private var render: ((Int, Int, (Float) -> Unit) -> ByteArray)? = null
    private var commit: ((IosCaptureTablePlacement) -> Boolean)? = null
    private var ready: ((Boolean) -> Unit)? = null
    private var editing: ((Boolean) -> Unit)? = null
    private var saved: (() -> Unit)? = null
    private var error: ((String) -> Unit)? = null
    var previewReady = false
        private set
    val hasViewport get() = viewport.width > 0f && viewport.height > 0f

    init {
        clipsToBounds = true
        backgroundColor = UIColor.blackColor
        preview?.let { layer.addSublayer(it) }
        gridLines.forEach { line ->
            line.backgroundColor = UIColor.whiteColor.colorWithAlphaComponent(0.42)
            line.userInteractionEnabled = false
            addSubview(line)
        }
        addSubview(image)
        addSubview(moveHandle)
        addSubview(resizeHandle)
        addSubview(focusRing)
        addSubview(focusLock)
        addSubview(expandBrightness)
        addSubview(exposureSlider)
        image.isAccessibilityElement = true
        image.accessibilityLabel = "촬영 표 미리보기"
        moveHandle.isAccessibilityElement = true
        moveHandle.accessibilityLabel = "표 이동"
        resizeHandle.isAccessibilityElement = true
        resizeHandle.accessibilityLabel = "표 크기 조절"
        resizeHandle.text = "+"
        resizeHandle.textAlignment = NSTextAlignmentCenter
        resizeHandle.backgroundColor = UIColor.whiteColor
        resizeHandle.textColor = UIColor.blackColor
        resizeHandle.userInteractionEnabled = true
        resizeHandle.layer.cornerRadius = 12.0
        resizeHandle.clipsToBounds = true
        focusRing.hidden = true
        focusRing.userInteractionEnabled = false
        focusRing.layer.borderWidth = 1.25
        focusRing.isAccessibilityElement = true
        focusRing.accessibilityLabel = "카메라 초점"
        focusLock.hidden = true
        updateLockIcon()
        focusLock.accessibilityLabel = "노출 잠금"
        focusLock.imageView?.layer?.apply {
            shadowColor = UIColor.blackColor.CGColor
            shadowOpacity = 0.8f
            shadowRadius = 1.0
            shadowOffset = CGSizeMake(0.0, 1.0)
        }
        expandBrightness.hidden = true
        expandBrightness.accessibilityLabel = "밝기 조절 열기"
        exposureSlider.setThumbImage(UIImage.systemImageNamed("sun.max.fill"), forState = UIControlStateNormal)
        exposureSlider.hidden = true
        exposureSlider.accessibilityLabel = "밝기 조절"
        exposureSlider.maximumTrackTintColor = UIColor.whiteColor.colorWithAlphaComponent(0.55)
        focusTap.cancelsTouchesInView = false
        pinchZoom.cancelsTouchesInView = false
        moveHandle.addGestureRecognizer(movePan)
        resizeHandle.addGestureRecognizer(resizePan)
        addGestureRecognizer(focusTap)
        addGestureRecognizer(pinchZoom)
        expandBrightness.addTarget(target, NSSelectorFromString("openBrightness:"), UIControlEventTouchUpInside)
        focusLock.addTarget(target, NSSelectorFromString("toggleFocusLock:"), UIControlEventTouchUpInside)
        exposureSlider.addTarget(target, NSSelectorFromString("exposureChanged:"), UIControlEventValueChanged)
        exposureSlider.addTarget(target, NSSelectorFromString("exposureBegan:"), UIControlEventTouchDown)
        exposureSlider.addTarget(target, NSSelectorFromString("exposureEnded:"),
            UIControlEventTouchUpInside or UIControlEventTouchUpOutside or UIControlEventTouchCancel)
    }

    fun updateCaptureAspect(aspect: Double?) {
        if (captureAspect == aspect) return
        captureAspect = aspect
        previewReady = false
        ready?.invoke(false)
        dirty = true
        setNeedsLayout()
    }

    fun bind(p: IosCaptureTablePlacement?, active: Boolean, gridVisible: Boolean,
        tableVisible: Boolean, focusActive: Boolean, collapseToken: Int,
        minExposureBias: Float, maxExposureBias: Float,
        focusWriter: suspend (Double, Double, Boolean) -> Boolean,
        exposureLockWriter: (Boolean) -> Boolean,
        exposureBiasWriter: (Float) -> Boolean,
        onFocusInteraction: (Boolean) -> Unit, version: Int,
        zoomActive: Boolean, zoomValue: Float, maxZoom: Float,
        zoomValueWriter: (Float) -> Boolean, onZoomCommit: (Float) -> Unit,
        renderer: (Int, Int, (Float) -> Unit) -> ByteArray, writer: (IosCaptureTablePlacement) -> Boolean,
        onReady: (Boolean) -> Unit, onEditing: (Boolean) -> Unit,
        onSaved: () -> Unit, onError: (String) -> Unit) {
        render = renderer; commit = writer; ready = onReady; editing = onEditing; saved = onSaved; error = onError
        focusAt = focusWriter
        exposureLock = exposureLockWriter
        exposureBias = exposureBiasWriter
        focusInteraction = onFocusInteraction
        zoomEnabled = zoomActive && maxZoom > 1f
        currentZoom = zoomValue.coerceIn(1f, maxOf(1f, maxZoom))
        maximumZoom = maxOf(1f, maxZoom)
        zoomWriter = zoomValueWriter
        zoomCommit = onZoomCommit
        focusEnabled = focusActive
        minimumExposureBias = minExposureBias
        maximumExposureBias = maxExposureBias
        exposureSlider.minimumValue = minExposureBias
        exposureSlider.maximumValue = maxExposureBias
        focusTap.enabled = focusActive
        pinchZoom.enabled = zoomEnabled
        if (!focusActive) suspendFocusControls()
        if (focusCollapseToken != collapseToken) {
            focusCollapseToken = collapseToken
            collapseFocusControls()
        }
        if (start == null && (placement != p || revision != version ||
                showGrid != gridVisible || showTable != tableVisible)) {
            if (placement != p) { previewReady = false; onReady(false) }
            start = null
            placement = p; revision = version
            showGrid = gridVisible; showTable = tableVisible; dirty = true
            setNeedsLayout()
        }
        if (!active && start != null) {
            start = null; dirty = true; setNeedsLayout(); onEditing(false)
        }
        enabled = active
        moveHandle.hidden = p == null || !active || !tableVisible
        resizeHandle.hidden = p == null || !active || !tableVisible
        movePan.enabled = active && p != null && tableVisible
        resizePan.enabled = active && p != null && tableVisible
        layoutFocusControls()
    }

    override fun layoutSubviews() {
        super.layoutSubviews()
        val requestedAspect = captureAspect ?: fixtureAspect
        val captureRect = requestedAspect?.let(::centeredAspectRect) ?: bounds
        preview?.apply {
            videoGravity = AVLayerVideoGravityResizeAspectFill
            frame = captureRect
        }
        val rect = if (requestedAspect != null) captureRect
            else preview?.rectForMetadataOutputRectOfInterest(CGRectMake(0.0, 0.0, 1.0, 1.0)) ?: fixtureRect()
        val frame = rect.useContents { IosTableBounds(origin.x.toFloat(), origin.y.toFloat(),
            size.width.toFloat(), size.height.toFloat(), 0f, 0f) }
        if (frame.width < 1 || frame.height < 1) return
        if (frame != viewport) {
            if (start != null) { start = null; editing?.invoke(false) }
            viewport = frame
            dirty = true
        }
        layoutFocusControls()
        updateGrid(frame)
        if (!dirty || start != null) return
        dirty = false
        image.transform = CGAffineTransformMakeTranslation(0.0, 0.0)
        image.setFrame(rect)
        val p = placement
        moveHandle.hidden = p == null || !enabled || !showTable
        resizeHandle.hidden = p == null || !enabled || !showTable
        image.hidden = p == null || !showTable
        if (p != null) {
            try {
                val w = 720
                val h = (w * frame.height / frame.width).toInt().coerceAtLeast(1)
                val bytes = checkNotNull(render)(w, h) { contentAspect = it }
                val data = bytes.usePinned { NSData.create(bytes = it.addressOf(0), length = bytes.size.toULong()) }
                image.image = checkNotNull(UIImage.imageWithData(data))
                updateHandles(p)
            } catch (failure: Throwable) {
                image.image = null
                moveHandle.hidden = true; resizeHandle.hidden = true
                previewReady = false; ready?.invoke(false)
                error?.invoke(failure.message ?: "표 미리보기를 표시할 수 없습니다.")
                return
            }
        } else image.image = null
        previewReady = true
        ready?.invoke(true)
    }

    private fun updateGrid(frame: IosTableBounds) {
        gridLines.forEach { it.hidden = !showGrid }
        if (!showGrid) return
        val oneThirdW = frame.width / 3f
        val oneThirdH = frame.height / 3f
        val stroke = 1.0
        gridLines[0].setFrame(CGRectMake((frame.x + oneThirdW).toDouble(), frame.y.toDouble(),
            stroke, frame.height.toDouble()))
        gridLines[1].setFrame(CGRectMake((frame.x + oneThirdW * 2).toDouble(), frame.y.toDouble(),
            stroke, frame.height.toDouble()))
        gridLines[2].setFrame(CGRectMake(frame.x.toDouble(), (frame.y + oneThirdH).toDouble(),
            frame.width.toDouble(), stroke))
        gridLines[3].setFrame(CGRectMake(frame.x.toDouble(), (frame.y + oneThirdH * 2).toDouble(),
            frame.width.toDouble(), stroke))
    }

    fun focus(sender: UITapGestureRecognizer) {
        if (!focusEnabled || sender.state != UIGestureRecognizerStateEnded) return
        val location = sender.locationInView(this)
        if (!hasViewport || !location.useContents {
                x >= viewport.x && x <= viewport.x + viewport.width &&
                    y >= viewport.y && y <= viewport.y + viewport.height
            }) return
        if ((!moveHandle.hidden && contains(moveHandle, location)) ||
            (!resizeHandle.hidden && contains(resizeHandle, location)) ||
            (!expandBrightness.hidden && contains(expandBrightness, location)) ||
            (!focusLock.hidden && contains(focusLock, location)) ||
            (!exposureSlider.hidden && contains(exposureSlider, location))) return

        if (exposureLocked && !awaitingLockedRefocusTap) {
            // Confirmed Android behavior: the first locked tap only reveals EV;
            // the following tap chooses a new AF point while keeping AE locked.
            focusExpanded = true
            awaitingLockedRefocusTap = true
            focusInteraction?.invoke(trackingExposure)
            layoutFocusControls()
            return
        }
        val devicePoint = preview?.let { it.captureDevicePointOfInterestForPoint(it.convertPoint(location, fromLayer = layer)) } ?: location.useContents {
            CGPointMake(
                ((x - viewport.x) / viewport.width).coerceIn(0.0, 1.0),
                ((y - viewport.y) / viewport.height).coerceIn(0.0, 1.0),
            )
        }
        val (deviceX, deviceY) = devicePoint.useContents { x to y }
        val lockedAtTap = exposureLocked
        // A locked refocus keeps both the native exposure bias and the slider value.
        if (!lockedAtTap) {
            exposureBias?.invoke(0f)
            exposureSlider.value = 0f
        }
        cancelFocusFeedback()
        focusX = location.useContents { x.toFloat() }
        focusY = location.useContents { y.toFloat() }
        focusExpanded = true
        awaitingLockedRefocusTap = false
        waitingForFocus = true
        focusRing.accessibilityValue = "초점 조정 중"
        focusRing.layer.addAnimation(CABasicAnimation().apply {
            keyPath = "opacity"
            fromValue = NSNumber.numberWithDouble(0.55)
            toValue = NSNumber.numberWithDouble(1.0)
            duration = 0.28
            autoreverses = true
            repeatCount = Float.MAX_VALUE
        }, forKey = "focusPulse")
        layoutFocusControls()
        focusRequest = feedbackScope.launch {
            val settled = withTimeoutOrNull(FOCUS_REQUEST_TIMEOUT_MS) {
                focusAt?.invoke(deviceX, deviceY, lockedAtTap) == true
            }
            waitingForFocus = false
            focusRing.layer.removeAnimationForKey("focusPulse")
            focusInteraction?.invoke(trackingExposure)
            when (settled) {
                true -> focusRing.accessibilityValue = "초점 조정 완료"
                false -> {
                    focusRing.accessibilityValue = "초점 요청 실패"
                    focusRing.layer.borderColor = UIColor.systemRedColor.CGColor
                    delay(FOCUS_FAILURE_FLASH_MS)
                    restoreRingColor()
                }
                null -> {
                    focusTimedOut = true
                    focusRing.accessibilityValue = "초점 응답 시간 초과"
                    if (!trackingExposure) collapseFocusControls()
                }
            }
        }
    }

    private fun restoreRingColor() {
        focusRing.layer.borderColor = if (exposureLocked) lockedColor.CGColor else UIColor.whiteColor.CGColor
    }

    private fun cancelFocusFeedback() {
        if (waitingForFocus) {
            focusRing.accessibilityValue = if (exposureLocked) "노출 잠금" else "초점 조정 종료"
        }
        focusRequest?.cancel()
        focusRequest = null
        waitingForFocus = false
        focusTimedOut = false
        focusRing.layer.removeAnimationForKey("focusPulse")
        restoreRingColor()
    }

    fun toggleFocusLock() {
        if (!focusEnabled || !focusX.isFinite() || !focusY.isFinite()) return
        val desired = !exposureLocked
        if (exposureLock?.invoke(desired) != true) {
            focusRing.layer.borderColor = UIColor.systemRedColor.CGColor
            focusRing.accessibilityValue = "노출 잠금 실패"
            focusInteraction?.invoke(trackingExposure)
            return
        }
        if (!desired) cancelFocusFeedback()
        exposureLocked = desired
        awaitingLockedRefocusTap = false
        focusRing.layer.borderColor = if (desired) lockedColor.CGColor
            else UIColor.whiteColor.CGColor
        updateLockIcon()
        focusLock.accessibilityLabel = if (desired) "노출 잠금 해제" else "노출 잠금"
        if (!waitingForFocus) focusRing.accessibilityValue = if (desired) "노출 잠금" else "노출 자동"
        if (!desired) focusExpanded = false
        focusInteraction?.invoke(trackingExposure)
        layoutFocusControls()
    }

    private fun updateLockIcon() {
        val symbol = if (exposureLocked) "lock.fill" else "lock.open"
        focusLock.setImage(UIImage.systemImageNamed(symbol), forState = UIControlStateNormal)
        focusLock.tintColor = if (exposureLocked) lockedColor else UIColor.whiteColor
        val sliderColor = if (exposureLocked) lockedColor else UIColor.systemYellowColor
        exposureSlider.minimumTrackTintColor = sliderColor
        exposureSlider.tintColor = sliderColor
    }

    fun openBrightness() {
        if (!focusEnabled || !exposureLocked) return
        focusExpanded = true
        focusInteraction?.invoke(trackingExposure)
        layoutFocusControls()
    }

    fun setExposureTracking(tracking: Boolean) {
        if (trackingExposure == tracking) return
        trackingExposure = tracking
        focusInteraction?.invoke(tracking)
    }

    fun exposureChanged(sender: UISlider) {
        if (!focusEnabled || !focusExpanded) return
        exposureBias?.invoke(sender.value)
        focusInteraction?.invoke(trackingExposure)
    }

    fun pinchZoom(sender: UIPinchGestureRecognizer) {
        if (!zoomEnabled) return
        when (sender.state) {
            UIGestureRecognizerStateBegan -> {
                pinchStartZoom = currentZoom
                collapseFocusControls()
            }
            UIGestureRecognizerStateChanged -> {
                val next = (pinchStartZoom * sender.scale.toFloat()).coerceIn(1f, maximumZoom)
                if (zoomWriter?.invoke(next) == true) currentZoom = next
            }
            UIGestureRecognizerStateEnded -> zoomCommit?.invoke(currentZoom)
            UIGestureRecognizerStateCancelled,
            UIGestureRecognizerStateFailed -> Unit
            else -> Unit
        }
    }

    fun collapseFocusControls() {
        val hideTimedOutRing = focusTimedOut && !exposureLocked
        cancelFocusFeedback()
        if (hideTimedOutRing) {
            clearFocusControls()
            return
        }
        setExposureTracking(false)
        focusExpanded = false
        awaitingLockedRefocusTap = false
        if (!exposureLocked) {
            focusRing.hidden = true
            focusLock.hidden = true
        }
        exposureSlider.hidden = true
        layoutFocusControls()
    }

    private fun clearFocusControls() {
        focusExpanded = false
        exposureLocked = false
        awaitingLockedRefocusTap = false
        focusX = Float.NaN
        focusY = Float.NaN
        focusRing.hidden = true
        focusLock.hidden = true
        expandBrightness.hidden = true
        exposureSlider.hidden = true
    }

    private fun suspendFocusControls() {
        cancelFocusFeedback()
        setExposureTracking(false)
        // Capture and table gestures temporarily disable input, not AVFoundation
        // AE lock. Keep the locked state until the same camera view is usable
        // again; otherwise the UI incorrectly reports automatic exposure.
        if (!exposureLocked) {
            clearFocusControls()
            return
        }
        focusExpanded = false
        awaitingLockedRefocusTap = false
        focusRing.hidden = true
        focusLock.hidden = true
        expandBrightness.hidden = true
        exposureSlider.hidden = true
    }

    private fun layoutFocusControls() {
        if (!focusEnabled || !focusX.isFinite() || !focusY.isFinite()) return
        if (!hasViewport) return
        val width = viewport.width
        val height = viewport.height
        val radius = (if (focusExpanded) FOCUS_RING_RADIUS_DP else FOCUS_RING_COMPACT_RADIUS_DP)
            .toFloat().coerceAtMost(minOf(width, height) / 2f)
        val centerX = visibleFocusCenter(focusX - viewport.x, width, radius)
        val centerY = visibleFocusCenter(focusY - viewport.y, height, radius)
        val lockSize = minOf(FOCUS_LOCK_TOUCH_DP.toFloat(), width, height)
        val sliderHeight = minOf(FOCUS_EV_TOUCH_HEIGHT_DP.toFloat(), height)
        val placement = placeFocusControlsVertically(centerY, height, radius, lockSize, sliderHeight, 2f)

        fun centeredFrame(top: Float, w: Float, h: Float): CValue<CGRect> {
            val left = (centerX - w / 2f).coerceIn(0f, width - w)
            return CGRectMake((viewport.x + left).toDouble(), (viewport.y + top).toDouble(),
                w.toDouble(), h.toDouble())
        }
        focusRing.setFrame(centeredFrame(centerY - radius, 2f * radius, 2f * radius))
        focusRing.layer.cornerRadius = radius.toDouble()
        focusRing.hidden = false
        focusLock.hidden = !(focusExpanded || exposureLocked)
        focusLock.setFrame(centeredFrame(placement.lockTop, lockSize, lockSize))
        // Added above the lock so its overlapping hit area cannot unlock a center tap.
        val centerSize = minOf(36f, 2f * radius)
        expandBrightness.setFrame(centeredFrame(centerY - centerSize / 2f, centerSize, centerSize))
        expandBrightness.hidden = !exposureLocked
        exposureSlider.hidden = !focusExpanded || maximumExposureBias <= minimumExposureBias
        val sliderWidth = minOf(FOCUS_EV_TOUCH_WIDTH_DP.toFloat(), width)
        exposureSlider.setFrame(centeredFrame(placement.exposureTop, sliderWidth, sliderHeight))
        exposureSlider.placeAboveRing(placement.exposureAbove)
    }

    private fun contains(view: UIView, point: CValue<CGPoint>): Boolean {
        val (px, py) = point.useContents { x to y }
        return view.frame.useContents {
            px >= origin.x && px <= origin.x + size.width &&
                py >= origin.y && py <= origin.y + size.height
        }
    }

    private fun fixtureRect(): CValue<CGRect> {
        val aspect = checkNotNull(fixtureAspect) { "카메라 사진 비율이 없습니다." }
        return centeredAspectRect(aspect)
    }

    private fun centeredAspectRect(aspect: Double): CValue<CGRect> {
        require(aspect > 0.0 && aspect.isFinite())
        return bounds.useContents {
            val w = minOf(size.width, size.height * aspect)
            val h = w / aspect
            CGRectMake((size.width - w) / 2, (size.height - h) / 2, w, h)
        }
    }

    private fun box(p: IosCaptureTablePlacement) = iosCaptureTableBounds(
        viewport.width.toInt(), viewport.height.toInt(), p, contentAspect)

    private fun updateHandles(p: IosCaptureTablePlacement) {
        val b = box(p)
        moveHandle.setFrame(CGRectMake((viewport.x + b.x).toDouble(), (viewport.y + b.y).toDouble(),
            b.width.toDouble(), b.height.toDouble()))
        // Keep the 32pt handle inside the image at flush right/bottom anchors.
        val handleX = (viewport.x + b.x + b.width - 32).coerceIn(viewport.x, maxOf(viewport.x, viewport.x + viewport.width - 32))
        val handleY = (viewport.y + b.y + b.height - 32).coerceIn(viewport.y, maxOf(viewport.y, viewport.y + viewport.height - 32))
        resizeHandle.setFrame(CGRectMake(handleX.toDouble(), handleY.toDouble(), 32.0, 32.0))
        moveHandle.accessibilityValue = "가로 ${p.offsetXPercent}, 세로 ${p.offsetYPercent}"
        resizeHandle.accessibilityValue = "너비 ${p.widthRatio}, 높이 ${p.heightRatio}"
    }

    fun pan(sender: UIPanGestureRecognizer, resizing: Boolean) {
        if (!enabled || !previewReady) return
        when (sender.state) {
            UIGestureRecognizerStateBegan -> {
                start = placement
                editing?.invoke(true)
            }
            UIGestureRecognizerStateChanged,
            UIGestureRecognizerStateEnded -> {
                val initial = start ?: return
                val delta = sender.translationInView(this).useContents { x.toFloat() to y.toFloat() }
                val p = if (resizing) resizeIosCaptureTable(initial, viewport.width.toInt(), viewport.height.toInt(), delta.first, delta.second, contentAspect)
                    else moveIosCaptureTable(initial, viewport.width.toInt(), viewport.height.toInt(), delta.first, delta.second, contentAspect)
                val old = box(initial)
                val next = box(p)
                val sx = next.width / old.width
                val sy = next.height / old.height
                val cx = viewport.width / 2f
                val cy = viewport.height / 2f
                val tx = next.x - (cx + sx * (old.x - cx))
                val ty = next.y - (cy + sy * (old.y - cy))
                image.transform = CGAffineTransformMake(sx.toDouble(), 0.0, 0.0, sy.toDouble(), tx.toDouble(), ty.toDouble())
                updateHandles(p)
                if (sender.state == UIGestureRecognizerStateEnded) {
                    start = null
                    if (runCatching { commit?.invoke(p) == true }.getOrDefault(false)) { placement = p; saved?.invoke() }
                    else error?.invoke("표 배치를 저장하지 못했습니다. 다시 시도해 주세요.")
                    dirty = true; setNeedsLayout()
                    editing?.invoke(false)
                }
            }
            UIGestureRecognizerStateCancelled,
            UIGestureRecognizerStateFailed -> {
                start = null; dirty = true; setNeedsLayout(); editing?.invoke(false)
            }
            else -> Unit
        }
    }

    fun release() {
        feedbackScope.cancel()
        moveHandle.removeGestureRecognizer(movePan)
        resizeHandle.removeGestureRecognizer(resizePan)
        removeGestureRecognizer(focusTap)
        removeGestureRecognizer(pinchZoom)
        expandBrightness.removeTarget(target, NSSelectorFromString("openBrightness:"), UIControlEventTouchUpInside)
        focusLock.removeTarget(target, NSSelectorFromString("toggleFocusLock:"), UIControlEventTouchUpInside)
        exposureSlider.removeTarget(target, NSSelectorFromString("exposureChanged:"), UIControlEventValueChanged)
        exposureSlider.removeTarget(target, NSSelectorFromString("exposureBegan:"), UIControlEventTouchDown)
        exposureSlider.removeTarget(target, NSSelectorFromString("exposureEnded:"),
            UIControlEventTouchUpInside or UIControlEventTouchUpOutside or UIControlEventTouchCancel)
        target.owner = null
        image.image = null
        gridLines.forEach { it.removeFromSuperview() }
        render = null; commit = null; ready = null; editing = null; saved = null; error = null
        focusAt = null; exposureLock = null; exposureBias = null; focusInteraction = null
        zoomWriter = null; zoomCommit = null
        preview?.removeFromSuperlayer()
    }
}

/** Keep the full slider frame interactive while drawing the shorter track next to the ring. */
private class FocusExposureSlider : UISlider(frame = CGRectMake(0.0, 0.0, 0.0, 0.0)) {
    private var aboveRing = false

    fun placeAboveRing(above: Boolean) {
        if (aboveRing == above) return
        aboveRing = above
        setNeedsLayout()
    }

    override fun trackRectForBounds(bounds: CValue<CGRect>): CValue<CGRect> = bounds.useContents {
        val width = minOf(size.width, FOCUS_EV_WIDTH_DP.toDouble())
        val inset = minOf(size.height / 2.0, FOCUS_EV_TRACK_INSET_DP.toDouble())
        val y = if (aboveRing) size.height - inset else inset
        CGRectMake(origin.x + (size.width - width) / 2.0, origin.y + y - 1.0, width, 2.0)
    }

    override fun beginTrackingWithTouch(touch: UITouch, withEvent: UIEvent?): Boolean = updateFromTouch(touch)

    override fun continueTrackingWithTouch(touch: UITouch, withEvent: UIEvent?): Boolean = updateFromTouch(touch)

    private fun updateFromTouch(touch: UITouch): Boolean {
        val x = touch.locationInView(this).useContents { x }
        val fraction = trackRectForBounds(bounds).useContents {
            ((x - origin.x) / size.width.coerceAtLeast(1.0)).coerceIn(0.0, 1.0)
        }
        value = minimumValue + (maximumValue - minimumValue) * fraction.toFloat()
        sendActionsForControlEvents(UIControlEventValueChanged)
        return true
    }
}

private class LiveTableGestureTarget(var owner: IosLiveTableView?) : NSObject() {
    @ObjCAction fun move(sender: UIPanGestureRecognizer) { owner?.pan(sender, false) }
    @ObjCAction fun resize(sender: UIPanGestureRecognizer) { owner?.pan(sender, true) }
    @ObjCAction fun focus(sender: UITapGestureRecognizer) { owner?.focus(sender) }
    @ObjCAction fun openBrightness(sender: UIButton) { owner?.openBrightness() }
    @ObjCAction fun toggleFocusLock(sender: UIButton) { owner?.toggleFocusLock() }
    @ObjCAction fun exposureBegan(sender: UISlider) { owner?.setExposureTracking(true) }
    @ObjCAction fun exposureEnded(sender: UISlider) { owner?.setExposureTracking(false) }
    @ObjCAction fun exposureChanged(sender: UISlider) { owner?.exposureChanged(sender) }
    @ObjCAction fun pinchZoom(sender: UIPinchGestureRecognizer) { owner?.pinchZoom(sender) }
}
