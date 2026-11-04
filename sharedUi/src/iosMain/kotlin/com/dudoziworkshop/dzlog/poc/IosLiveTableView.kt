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
            it.bind(placement, enabled, revision, session::renderLiveOverlay, session::savePlacement,
                onReady, onEditing, onSaved, onError)
        })
}

internal class IosLiveTableView(
    private val preview: AVCaptureVideoPreviewLayer?,
    private val fixtureAspect: Double?,
) : UIView(frame = CGRectMake(0.0, 0.0, 1.0, 1.0)) {
    private val image = UIImageView()
    private val moveHandle = UIView()
    private val resizeHandle = UILabel()
    private val target = LiveTableGestureTarget(this)
    private val movePan = UIPanGestureRecognizer(target, NSSelectorFromString("move:"))
    private val resizePan = UIPanGestureRecognizer(target, NSSelectorFromString("resize:"))
    private var placement: IosCaptureTablePlacement? = null
    private var start: IosCaptureTablePlacement? = null
    private var enabled = false
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
        addSubview(image)
        addSubview(moveHandle)
        addSubview(resizeHandle)
        image.isAccessibilityElement = true
        image.accessibilityLabel = "촬영 표 미리보기"
        moveHandle.isAccessibilityElement = true
        moveHandle.accessibilityLabel = "표 이동"
        moveHandle.accessibilityIdentifier = "ios.table.move"
        resizeHandle.isAccessibilityElement = true
        resizeHandle.accessibilityLabel = "표 크기 조절"
        resizeHandle.accessibilityIdentifier = "ios.table.resize"
        resizeHandle.text = "+"
        resizeHandle.textAlignment = NSTextAlignment.NSTextAlignmentCenter
        resizeHandle.backgroundColor = UIColor.whiteColor
        resizeHandle.textColor = UIColor.blackColor
        resizeHandle.userInteractionEnabled = true
        resizeHandle.layer.cornerRadius = 12.0
        resizeHandle.clipsToBounds = true
        moveHandle.addGestureRecognizer(movePan)
        resizeHandle.addGestureRecognizer(resizePan)
    }

    fun bind(p: IosCaptureTablePlacement?, active: Boolean, version: Int,
        renderer: (Int, Int) -> ByteArray, writer: (IosCaptureTablePlacement) -> Boolean,
        onReady: (Boolean) -> Unit, onEditing: (Boolean) -> Unit,
        onSaved: () -> Unit, onError: (String) -> Unit) {
        render = renderer; commit = writer; ready = onReady; editing = onEditing; saved = onSaved; error = onError
        if (start == null && (placement != p || revision != version)) {
            if (placement != p) { previewReady = false; onReady(false) }
            start = null
            placement = p; revision = version; dirty = true
            setNeedsLayout()
        }
        if (!active && start != null) {
            start = null; dirty = true; setNeedsLayout(); onEditing(false)
        }
        enabled = active
        moveHandle.hidden = p == null || !active
        resizeHandle.hidden = p == null || !active
        movePan.enabled = active && p != null
        resizePan.enabled = active && p != null
    }

    override fun layoutSubviews() {
        super.layoutSubviews()
        preview?.frame = bounds
        val rect = preview?.layerRectConvertedFromMetadataOutputRect(CGRectMake(0.0, 0.0, 1.0, 1.0)) ?: fixtureRect()
        val frame = rect.useContents { IosTableBounds(origin.x.toFloat(), origin.y.toFloat(),
            size.width.toFloat(), size.height.toFloat(), 0f, 0f) }
        if (frame.width < 1 || frame.height < 1) return
        if (frame != viewport) {
            if (start != null) { start = null; editing?.invoke(false) }
            viewport = frame
            dirty = true
        }
        if (!dirty || start != null) return
        dirty = false
        image.transform = CGAffineTransformMakeTranslation(0.0, 0.0)
        image.frame = rect
        val p = placement
        moveHandle.hidden = p == null || !enabled
        resizeHandle.hidden = p == null || !enabled
        image.hidden = p == null
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
        moveHandle.frame = CGRectMake((viewport.x + b.x).toDouble(), (viewport.y + b.y).toDouble(),
            b.width.toDouble(), b.height.toDouble())
        // Keep the 32pt handle inside the image at flush right/bottom anchors.
        val handleX = (viewport.x + b.x + b.width - 32).coerceIn(viewport.x, maxOf(viewport.x, viewport.x + viewport.width - 32))
        val handleY = (viewport.y + b.y + b.height - 32).coerceIn(viewport.y, maxOf(viewport.y, viewport.y + viewport.height - 32))
        resizeHandle.frame = CGRectMake(handleX.toDouble(), handleY.toDouble(), 32.0, 32.0)
        moveHandle.accessibilityValue = "가로 ${p.offsetXPercent}, 세로 ${p.offsetYPercent}"
        resizeHandle.accessibilityValue = "너비 ${p.widthRatio}, 높이 ${p.heightRatio}"
    }

    fun pan(sender: UIPanGestureRecognizer, resizing: Boolean) {
        if (!enabled || !previewReady) return
        when (sender.state) {
            UIGestureRecognizerState.UIGestureRecognizerStateBegan -> {
                start = placement
                editing?.invoke(true)
            }
            UIGestureRecognizerState.UIGestureRecognizerStateChanged,
            UIGestureRecognizerState.UIGestureRecognizerStateEnded -> {
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
                if (sender.state == UIGestureRecognizerState.UIGestureRecognizerStateEnded) {
                    start = null
                    if (runCatching { commit?.invoke(p) == true }.getOrDefault(false)) { placement = p; saved?.invoke() }
                    else error?.invoke("표 배치를 저장하지 못했습니다. 다시 시도해 주세요.")
                    dirty = true; setNeedsLayout()
                    editing?.invoke(false)
                }
            }
            UIGestureRecognizerState.UIGestureRecognizerStateCancelled,
            UIGestureRecognizerState.UIGestureRecognizerStateFailed -> {
                start = null; dirty = true; setNeedsLayout(); editing?.invoke(false)
            }
            else -> Unit
        }
    }

    fun release() {
        moveHandle.removeGestureRecognizer(movePan)
        resizeHandle.removeGestureRecognizer(resizePan)
        target.owner = null
        image.image = null
        render = null; commit = null; ready = null; editing = null; saved = null; error = null
        preview?.removeFromSuperlayer()
    }
}

private class LiveTableGestureTarget(var owner: IosLiveTableView?) : NSObject() {
    @ObjCAction fun move(sender: UIPanGestureRecognizer) { owner?.pan(sender, false) }
    @ObjCAction fun resize(sender: UIPanGestureRecognizer) { owner?.pan(sender, true) }
}
