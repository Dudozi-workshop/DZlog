@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, androidx.compose.ui.ExperimentalComposeUiApi::class)
package com.dudoziworkshop.dzlog.poc

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitView
import androidx.compose.ui.viewinterop.UIKitInteropProperties
import androidx.compose.ui.viewinterop.UIKitInteropInteractionMode
import kotlinx.cinterop.*
import kotlinx.coroutines.delay
import platform.AVFoundation.*
import platform.CoreGraphics.*
import platform.Foundation.*
import platform.UIKit.*
import platform.darwin.NSObject

/** One native surface keeps the table above the AVFoundation layer, including during gestures. */
@Composable
internal fun IosLiveTablePreview(
    modifier: Modifier,
    session: IosCameraCaptureSession,
    previewLayer: AVCaptureVideoPreviewLayer? = null,
    fixtureAspect: Double? = null,
    enabled: Boolean,
    showGrid: Boolean = false,
    showTable: Boolean = true,
    focusEnabled: Boolean = false,
    focusCollapseToken: Int = 0,
    minimumExposureBias: Float = 0f,
    maximumExposureBias: Float = 0f,
    onFocusAt: (Double, Double, Boolean) -> Boolean = { _, _, _ -> false },
    onExposureLock: (Boolean) -> Boolean = { false },
    onExposureBias: (Float) -> Boolean = { false },
    onFocusInteraction: () -> Unit = {},
    revision: Int,
    onReady: (Boolean) -> Unit,
    onEditing: (Boolean) -> Unit,
    onSaved: () -> Unit,
    onError: (String) -> Unit,
) {
    val view = remember(session, previewLayer, fixtureAspect) { IosLiveTableView(previewLayer, fixtureAspect) }
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
            it.bind(placement, enabled, showGrid, showTable, focusEnabled,
                focusCollapseToken, minimumExposureBias, maximumExposureBias,
                onFocusAt, onExposureLock, onExposureBias, onFocusInteraction, revision,
                session::renderLiveOverlay, session::savePlacement,
                onReady, onEditing, onSaved, onError)
        })
}

internal class IosLiveTableView(
    private val preview: AVCaptureVideoPreviewLayer?,
    private val fixtureAspect: Double?,
) : UIView(frame = CGRectMake(0.0, 0.0, 1.0, 1.0)) {
    private val image = UIImageView()
    private val gridLines = List(4) { UIView() }
    private val moveHandle = UIView()
    private val resizeHandle = UILabel()
    private val focusRing = UIView()
    private val focusLock = UIButton.buttonWithType(UIButtonTypeSystem)
    private val exposureSlider = UISlider()
    private val target = LiveTableGestureTarget(this)
    private val movePan = UIPanGestureRecognizer(target, NSSelectorFromString("move:"))
    private val resizePan = UIPanGestureRecognizer(target, NSSelectorFromString("resize:"))
    private val focusTap = UITapGestureRecognizer(target, NSSelectorFromString("focus:"))
    private var placement: IosCaptureTablePlacement? = null
    private var start: IosCaptureTablePlacement? = null
    private var enabled = false
    private var showGrid = false
    private var showTable = true
    private var focusEnabled = false
    private var focusCollapseToken = -1
    private var focusExpanded = false
    private var exposureLocked = false
    private var awaitingLockedRefocusTap = false
    private var focusX = Float.NaN
    private var focusY = Float.NaN
    private var minimumExposureBias = 0f
    private var maximumExposureBias = 0f
    private var focusAt: ((Double, Double, Boolean) -> Boolean)? = null
    private var exposureLock: ((Boolean) -> Boolean)? = null
    private var exposureBias: ((Float) -> Boolean)? = null
    private var focusInteraction: (() -> Unit)? = null
    private var revision = -1
    private var dirty = true
    private var viewport = IosTableBounds(0f, 0f, 0f, 0f, 0f, 0f)
    private var render: ((Int, Int) -> ByteArray)? = null
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
        focusRing.layer.borderWidth = 2.0
        focusRing.layer.cornerRadius = 27.0
        focusRing.isAccessibilityElement = true
        focusRing.accessibilityLabel = "카메라 초점"
        focusLock.hidden = true
        focusLock.setTitle("AE", forState = UIControlStateNormal)
        focusLock.accessibilityLabel = "노출 잠금"
        focusLock.backgroundColor = UIColor.blackColor.colorWithAlphaComponent(0.48)
        focusLock.layer.cornerRadius = 10.0
        exposureSlider.hidden = true
        exposureSlider.accessibilityLabel = "밝기 조절"
        exposureSlider.minimumTrackTintColor = UIColor.systemYellowColor
        exposureSlider.maximumTrackTintColor = UIColor.whiteColor.colorWithAlphaComponent(0.55)
        focusTap.cancelsTouchesInView = false
        moveHandle.addGestureRecognizer(movePan)
        resizeHandle.addGestureRecognizer(resizePan)
        addGestureRecognizer(focusTap)
        focusLock.addTarget(target, NSSelectorFromString("toggleFocusLock:"), UIControlEventTouchUpInside)
        exposureSlider.addTarget(target, NSSelectorFromString("exposureChanged:"), UIControlEventValueChanged)
    }

    fun bind(p: IosCaptureTablePlacement?, active: Boolean, gridVisible: Boolean,
        tableVisible: Boolean, focusActive: Boolean, collapseToken: Int,
        minExposureBias: Float, maxExposureBias: Float,
        focusWriter: (Double, Double, Boolean) -> Boolean,
        exposureLockWriter: (Boolean) -> Boolean,
        exposureBiasWriter: (Float) -> Boolean,
        onFocusInteraction: () -> Unit, version: Int,
        renderer: (Int, Int) -> ByteArray, writer: (IosCaptureTablePlacement) -> Boolean,
        onReady: (Boolean) -> Unit, onEditing: (Boolean) -> Unit,
        onSaved: () -> Unit, onError: (String) -> Unit) {
        render = renderer; commit = writer; ready = onReady; editing = onEditing; saved = onSaved; error = onError
        focusAt = focusWriter
        exposureLock = exposureLockWriter
        exposureBias = exposureBiasWriter
        focusInteraction = onFocusInteraction
        focusEnabled = focusActive
        minimumExposureBias = minExposureBias
        maximumExposureBias = maxExposureBias
        exposureSlider.minimumValue = minExposureBias
        exposureSlider.maximumValue = maxExposureBias
        focusTap.enabled = focusActive
        if (!focusActive) clearFocusControls()
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
        preview?.frame = bounds
        val rect = preview?.rectForMetadataOutputRectOfInterest(CGRectMake(0.0, 0.0, 1.0, 1.0)) ?: fixtureRect()
        val frame = rect.useContents { IosTableBounds(origin.x.toFloat(), origin.y.toFloat(),
            size.width.toFloat(), size.height.toFloat(), 0f, 0f) }
        if (frame.width < 1 || frame.height < 1) return
        if (frame != viewport) {
            if (start != null) { start = null; editing?.invoke(false) }
            viewport = frame
            dirty = true
        }
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
                val bytes = checkNotNull(render)(w, h)
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
        if ((!moveHandle.hidden && contains(moveHandle, location)) ||
            (!resizeHandle.hidden && contains(resizeHandle, location)) ||
            (!focusLock.hidden && contains(focusLock, location)) ||
            (!exposureSlider.hidden && contains(exposureSlider, location))) return

        if (exposureLocked && !awaitingLockedRefocusTap) {
            // Confirmed Android behavior: the first locked tap only reveals EV;
            // the following tap chooses a new AF point while keeping AE locked.
            focusExpanded = true
            awaitingLockedRefocusTap = true
            focusInteraction?.invoke()
            layoutFocusControls()
            return
        }
        val devicePoint = preview?.captureDevicePointOfInterestForPoint(location) ?: location.useContents {
            CGPointMake(
                ((x - viewport.x) / viewport.width).coerceIn(0.0, 1.0),
                ((y - viewport.y) / viewport.height).coerceIn(0.0, 1.0),
            )
        }
        val applied = devicePoint.useContents {
            exposureBias?.invoke(0f)
            focusAt?.invoke(x, y, exposureLocked) == true
        }
        exposureSlider.value = 0f
        focusX = location.useContents { x.toFloat() }
        focusY = location.useContents { y.toFloat() }
        focusExpanded = true
        awaitingLockedRefocusTap = false
        focusRing.layer.borderColor = when {
            !applied -> UIColor.systemRedColor.CGColor
            exposureLocked -> UIColor.systemOrangeColor.CGColor
            else -> UIColor.whiteColor.CGColor
        }
        focusRing.accessibilityValue = if (applied) "초점 요청 적용" else "초점 요청 실패"
        focusInteraction?.invoke()
        layoutFocusControls()
    }

    fun toggleFocusLock() {
        if (!focusEnabled || !focusX.isFinite() || !focusY.isFinite()) return
        val desired = !exposureLocked
        if (exposureLock?.invoke(desired) != true) {
            focusRing.layer.borderColor = UIColor.systemRedColor.CGColor
            focusRing.accessibilityValue = "노출 잠금 실패"
            focusInteraction?.invoke()
            return
        }
        exposureLocked = desired
        awaitingLockedRefocusTap = false
        focusRing.layer.borderColor = if (desired) UIColor.systemOrangeColor.CGColor
            else UIColor.whiteColor.CGColor
        focusLock.setTitle(if (desired) "AE 🔒" else "AE", forState = UIControlStateNormal)
        focusLock.accessibilityLabel = if (desired) "노출 잠금 해제" else "노출 잠금"
        focusRing.accessibilityValue = if (desired) "노출 잠금" else "노출 자동"
        if (!desired) focusExpanded = false
        focusInteraction?.invoke()
        layoutFocusControls()
    }

    fun exposureChanged(sender: UISlider) {
        if (!focusEnabled || !focusExpanded) return
        exposureBias?.invoke(sender.value)
        focusInteraction?.invoke()
    }

    private fun collapseFocusControls() {
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
        exposureSlider.hidden = true
    }

    private fun layoutFocusControls() {
        if (!focusEnabled || !focusX.isFinite() || !focusY.isFinite()) return
        val (viewWidth, viewHeight) = bounds.useContents { size.width to size.height }
        if (viewWidth <= 0.0 || viewHeight <= 0.0) return
        val ringSize = if (focusExpanded) 54.0 else 38.0
        val ringX = (focusX - ringSize.toFloat() / 2f).toDouble().coerceIn(0.0, maxOf(0.0, viewWidth - ringSize))
        val ringY = (focusY - ringSize.toFloat() / 2f).toDouble().coerceIn(0.0, maxOf(0.0, viewHeight - ringSize))
        focusRing.setFrame(CGRectMake(ringX, ringY, ringSize, ringSize))
        focusRing.layer.cornerRadius = ringSize / 2.0
        focusRing.hidden = false

        focusLock.hidden = !(focusExpanded || exposureLocked)
        if (!focusLock.hidden) {
            val lockX = if (ringX + ringSize + 30.0 <= viewWidth) ringX + ringSize + 3.0
                else maxOf(0.0, ringX - 33.0)
            focusLock.setFrame(CGRectMake(lockX, (ringY + ringSize / 2.0 - 11.0)
                .coerceIn(0.0, maxOf(0.0, viewHeight - 22.0)), 30.0, 22.0))
        }

        val sliderAvailable = maximumExposureBias > minimumExposureBias
        exposureSlider.hidden = !focusExpanded || !sliderAvailable
        if (!exposureSlider.hidden) {
            // 78pt is the approved compact length (roughly 2/3 of the former control).
            val sliderWidth = 78.0
            val sliderHeight = 36.0
            val sliderX = (focusX.toDouble() - sliderWidth / 2.0)
                .coerceIn(0.0, maxOf(0.0, viewWidth - sliderWidth))
            val aboveY = ringY - sliderHeight - 4.0
            val sliderY = if (aboveY >= 0.0) aboveY
                else (ringY + ringSize + 4.0).coerceAtMost(maxOf(0.0, viewHeight - sliderHeight))
            exposureSlider.setFrame(CGRectMake(sliderX, sliderY, sliderWidth, sliderHeight))
        }
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
        return bounds.useContents {
            val w = minOf(size.width, size.height * aspect)
            val h = w / aspect
            CGRectMake((size.width - w) / 2, (size.height - h) / 2, w, h)
        }
    }

    private fun box(p: IosCaptureTablePlacement) = iosCaptureTableBounds(
        viewport.width.toInt(), viewport.height.toInt(), p)

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
                val p = if (resizing) resizeIosCaptureTable(initial, viewport.width.toInt(), viewport.height.toInt(), delta.first, delta.second)
                    else moveIosCaptureTable(initial, viewport.width.toInt(), viewport.height.toInt(), delta.first, delta.second)
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
        moveHandle.removeGestureRecognizer(movePan)
        resizeHandle.removeGestureRecognizer(resizePan)
        removeGestureRecognizer(focusTap)
        focusLock.removeTarget(target, NSSelectorFromString("toggleFocusLock:"), UIControlEventTouchUpInside)
        exposureSlider.removeTarget(target, NSSelectorFromString("exposureChanged:"), UIControlEventValueChanged)
        target.owner = null
        image.image = null
        gridLines.forEach { it.removeFromSuperview() }
        render = null; commit = null; ready = null; editing = null; saved = null; error = null
        focusAt = null; exposureLock = null; exposureBias = null; focusInteraction = null
        preview?.removeFromSuperlayer()
    }
}

private class LiveTableGestureTarget(var owner: IosLiveTableView?) : NSObject() {
    @ObjCAction fun move(sender: UIPanGestureRecognizer) { owner?.pan(sender, false) }
    @ObjCAction fun resize(sender: UIPanGestureRecognizer) { owner?.pan(sender, true) }
    @ObjCAction fun focus(sender: UITapGestureRecognizer) { owner?.focus(sender) }
    @ObjCAction fun toggleFocusLock(sender: UIButton) { owner?.toggleFocusLock() }
    @ObjCAction fun exposureChanged(sender: UISlider) { owner?.exposureChanged(sender) }
}
