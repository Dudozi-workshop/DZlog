package com.dudoziworkshop.dzlog.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.RectF
import android.util.Log
import android.net.Uri
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.core.content.ContextCompat
import androidx.exifinterface.media.ExifInterface
import com.dudoziworkshop.dzlog.data.log.LogEntity
import com.dudoziworkshop.dzlog.data.log.LogRepository
import com.dudoziworkshop.dzlog.data.mediastore.MediaStoreSaver
import com.dudoziworkshop.dzlog.domain.camera.computeAnchoredCaptureRect
import com.dudoziworkshop.dzlog.domain.model.CaptureRequest
import com.dudoziworkshop.dzlog.domain.model.LogEntry
import com.dudoziworkshop.dzlog.domain.model.PhotoQualityMode
import com.dudoziworkshop.dzlog.domain.model.SaveMode
import com.dudoziworkshop.dzlog.domain.naming.buildGalleryRelativePath
import com.dudoziworkshop.dzlog.watermark.WatermarkRenderer
import com.dudoziworkshop.dzlog.watermark.renderWatermarkForRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File


class DzlogRepositoryImpl(
    private val saver: MediaStoreSaver,
    private val watermarkRenderer: WatermarkRenderer
) : DzlogRepository {

    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun buildRelativePath(group1: String, group2: String): String {
        return buildGalleryRelativePath(group1, group2)
    }

    override fun buildOriginalRelativePath(
        group1: String,
        group2: String
    ): String {
        return "${buildGalleryRelativePath(group1, group2)}original/"
    }

    override fun captureAndSave(
        context: Context,
        imageCapture: ImageCapture,
        request: CaptureRequest,
        onDone: (LogEntry) -> Unit,
        onFail: (String) -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(context)

        // ✅ 임시 파일로 촬영 → Bitmap 디코딩 → MediaStoreSaver.saveJpeg로 최종 저장
        val tmpFile = kotlin.runCatching {
            File.createTempFile("dzlog_", ".jpg", context.cacheDir)
        }.getOrElse {
            onFail("임시파일 생성 실패: ${it.message}")
            return
        }

        val metadata = ImageCapture.Metadata().apply {
            isReversedHorizontal = false
            isReversedVertical = false
        }
        val outputOptions = ImageCapture.OutputFileOptions.Builder(tmpFile)
            .setMetadata(metadata)
            .build()

        imageCapture.takePicture(
            outputOptions,
            executor,
            object : ImageCapture.OnImageSavedCallback {

                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    ioScope.launch {
                        val result = runCatching {
                            val decodedBmp = BitmapFactory.decodeFile(tmpFile.absolutePath)
                                ?: throw IllegalStateException("촬영 이미지 디코딩 실패")
                            val exif = ExifInterface(tmpFile)
                            val orientedBmp = applyExifOrientation(decodedBmp, exif)
                            val originalBmp = cropToAspect(orientedBmp, request)

                            val baseRel = buildRelativePath(request.group1, request.group2)
                            val origRel = buildOriginalRelativePath(request.group1, request.group2)

                            val displayName = request.displayName
                            val qualityMode = request.photoQualityMode
                            val jpegQuality = qualityMode.jpegQuality
                            when (request.saveMode) {
                                SaveMode.WATERMARK_ONLY -> {
                                    val wmBmp = renderWatermarkForRequest(
                                        renderer = watermarkRenderer,
                                        originalBmp = originalBmp,
                                        request = request
                                    )

                                    val saved = kotlin.runCatching {
                                        saver.saveJpeg(
                                            context = context,
                                            bitmap = applyPhotoQualityPolicy(wmBmp, qualityMode),
                                            displayName = displayName,
                                            relativePath = baseRel,
                                            jpegQuality = jpegQuality
                                        )
                                    }.getOrNull()
                                    val entry = LogEntry(
                                        mediaStoreId = saved?.mediaStoreId ?: -1L,
                                        contentUri = saved?.uri ?: Uri.EMPTY,
                                        savedContentUris = listOfNotNull(saved?.uri),
                                        displayName = saved?.displayName ?: displayName,
                                        isNameAdjusted = saved?.isNameAdjusted ?: false,
                                        createdAt = System.currentTimeMillis(),
                                        group1 = request.group1,
                                        group2 = request.group2
                                    )
                                    insertLogEntry(context, entry, saved?.uri, displayName, baseRel)
                                    entry
                                }

                                SaveMode.BOTH -> {
                                    // 1) 워터마크 먼저(대표 파일)
                                    val wmBmp = renderWatermarkForRequest(
                                        renderer = watermarkRenderer,
                                        originalBmp = originalBmp,
                                        request = request
                                    )

                                    val savedWm = kotlin.runCatching {
                                        saver.saveJpeg(
                                            context = context,
                                            bitmap = applyPhotoQualityPolicy(wmBmp, qualityMode),
                                            displayName = displayName,
                                            relativePath = baseRel,
                                            jpegQuality = jpegQuality
                                        )
                                    }.getOrNull()

                                    // 2) 원본은 original/ 하위 (실패해도 워터마크는 이미 저장됨)
                                    val savedOriginal = kotlin.runCatching {
                                        saver.saveJpeg(
                                            context = context,
                                            bitmap = applyPhotoQualityPolicy(originalBmp, qualityMode),
                                            displayName = displayName,
                                            relativePath = origRel,
                                            jpegQuality = jpegQuality
                                        )
                                    }.getOrNull()
                                    val entry = LogEntry(
                                        mediaStoreId = savedWm?.mediaStoreId ?: -1L,
                                        contentUri = savedWm?.uri ?: Uri.EMPTY,
                                        savedContentUris = listOfNotNull(savedWm?.uri, savedOriginal?.uri),
                                        displayName = savedWm?.displayName ?: displayName,
                                        isNameAdjusted = savedWm?.isNameAdjusted ?: false,
                                        createdAt = System.currentTimeMillis(),
                                        group1 = request.group1,
                                        group2 = request.group2
                                    )
                                    insertLogEntry(context, entry, savedWm?.uri, displayName, baseRel)
                                    entry
                                }

                                SaveMode.ORIGINAL_ONLY -> {
                                    val saved = kotlin.runCatching {
                                        saver.saveJpeg(
                                            context = context,
                                            bitmap = applyPhotoQualityPolicy(originalBmp, qualityMode),
                                            displayName = displayName,
                                            relativePath = origRel,
                                            jpegQuality = jpegQuality
                                        )
                                    }.getOrNull()

                                    LogEntry(
                                        mediaStoreId = saved?.mediaStoreId ?: -1L,
                                        contentUri = saved?.uri ?: Uri.EMPTY,
                                        savedContentUris = listOfNotNull(saved?.uri),
                                        displayName = saved?.displayName ?: displayName,
                                        isNameAdjusted = saved?.isNameAdjusted ?: false,
                                        createdAt = System.currentTimeMillis(),
                                        group1 = request.group1,
                                        group2 = request.group2
                                    )
                                }
                            }
                        }

                        tmpFile.delete()

                        withContext(Dispatchers.Main) {
                            result.onSuccess { entry ->
                                onDone(entry)
                            }.onFailure { error ->
                                onFail(error.message ?: "captureAndSave 실패")
                            }
                        }
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    tmpFile.delete()
                    onFail(exception.message ?: "촬영 실패")
                }
            }
        )
    }

    private suspend fun insertLogEntry(
        context: Context,
        entry: LogEntry,
        uri: Uri?,
        fileName: String,
        relativePath: String
    ) {
        val imageUri = uri?.toString().orEmpty()
        if (imageUri.isBlank()) return
        val log = LogEntity(
            createdAt = entry.createdAt,
            imageUri = imageUri,
            fileName = fileName,
            relativePath = relativePath,
            templateId = null,
            templateName = null,
            valuesJson = null
        )
        LogRepository.getInstance(context).insert(log)
    }
    private fun applyExifOrientation(source: Bitmap, exif: ExifInterface): Bitmap {
        val orientation = exif.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        )
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.postRotate(90f)
                matrix.postScale(-1f, 1f)
            }

            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.postRotate(270f)
                matrix.postScale(-1f, 1f)
            }

            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            else -> return source
        }
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }


    private fun applyPhotoQualityPolicy(source: Bitmap, mode: PhotoQualityMode): Bitmap {
        val maxLongEdge = mode.maxLongEdgePx ?: return source
        val width = source.width
        val height = source.height
        if (width <= 0 || height <= 0) return source

        val longEdge = maxOf(width, height)
        if (longEdge <= maxLongEdge) return source

        val scale = maxLongEdge.toFloat() / longEdge.toFloat()
        val targetWidth = (width * scale).toInt().coerceAtLeast(1)
        val targetHeight = (height * scale).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(source, targetWidth, targetHeight, true)
    }

    private fun cropToAspect(source: Bitmap, request: CaptureRequest): Bitmap {
        if (source.width == 0 || source.height == 0) return source

        val fullRect = RectF(0f, 0f, source.width.toFloat(), source.height.toFloat())
        val framing = computeAnchoredCaptureRect(
            contentRect = fullRect,
            captureAspectRatio = request.captureAspect.ratioF
        )
        val targetRect = framing.captureRect

        val left = targetRect.left.toInt().coerceIn(0, source.width - 1)
        val top = targetRect.top.toInt().coerceIn(0, source.height - 1)
        val right = kotlin.math.ceil(targetRect.right.toDouble()).toInt().coerceIn(left + 1, source.width)
        val bottom = kotlin.math.ceil(targetRect.bottom.toDouble()).toInt().coerceIn(top + 1, source.height)

        val width = (right - left).coerceAtLeast(1)
        val height = (bottom - top).coerceAtLeast(1)

        Log.d(
            "DZlogCrop",
            "crop aspect=${request.captureAspect.label} src=${source.width}x${source.height} rect=($left,$top)-($right,$bottom) anchorY=${framing.anchorY}"
        )

        return Bitmap.createBitmap(source, left, top, width, height)
    }
}
