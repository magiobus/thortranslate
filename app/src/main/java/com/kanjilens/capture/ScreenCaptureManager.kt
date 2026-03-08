package com.kanjilens.capture

import android.content.Context
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class ScreenCaptureManager(private val context: Context) {

    companion object {
        private const val TAG = "KanjiLens"
    }

    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private val handler = Handler(Looper.getMainLooper())

    // Callback for when projection is ready
    private var onProjectionReady: (() -> Unit)? = null

    val projectionManager: MediaProjectionManager =
        context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager

    val isReady: Boolean
        get() = mediaProjection != null

    fun setProjection(projection: MediaProjection) {
        Log.d(TAG, "MediaProjection received")
        mediaProjection = projection
        onProjectionReady?.invoke()
        onProjectionReady = null
    }

    fun awaitProjectionReady(callback: () -> Unit) {
        if (mediaProjection != null) {
            callback()
        } else {
            onProjectionReady = callback
        }
    }

    suspend fun captureScreen(): Bitmap? = suspendCancellableCoroutine { continuation ->
        val projection = mediaProjection
        if (projection == null) {
            Log.e(TAG, "captureScreen called but mediaProjection is null")
            continuation.resume(null)
            return@suspendCancellableCoroutine
        }

        val metrics = getScreenMetrics()
        val width = metrics.widthPixels
        val height = metrics.heightPixels
        val density = metrics.densityDpi

        Log.d(TAG, "Capturing screen: ${width}x${height} @ ${density}dpi")

        // Create ImageReader for single frame capture
        val reader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
        imageReader = reader

        var captured = false

        reader.setOnImageAvailableListener({ imgReader ->
            if (!captured) {
                captured = true
                val image = imgReader.acquireLatestImage()
                if (image != null) {
                    Log.d(TAG, "Image acquired: ${image.width}x${image.height}")
                    val bitmap = imageToBitmap(image, width, height)
                    image.close()

                    // Clean up VirtualDisplay and ImageReader after capture
                    // Keep MediaProjection alive for next capture
                    virtualDisplay?.release()
                    reader.close()
                    virtualDisplay = null
                    imageReader = null

                    continuation.resume(bitmap)
                } else {
                    Log.e(TAG, "acquireLatestImage returned null")
                    virtualDisplay?.release()
                    reader.close()
                    virtualDisplay = null
                    imageReader = null
                    continuation.resume(null)
                }
            }
        }, handler)

        // Create VirtualDisplay AFTER setting the listener
        try {
            val display = projection.createVirtualDisplay(
                "KanjiLensCapture",
                width, height, density,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                reader.surface,
                null, null
            )
            virtualDisplay = display
            Log.d(TAG, "VirtualDisplay created")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create VirtualDisplay", e)
            reader.close()
            imageReader = null
            continuation.resume(null)
        }

        continuation.invokeOnCancellation {
            virtualDisplay?.release()
            reader.close()
            virtualDisplay = null
            imageReader = null
        }
    }

    fun release() {
        virtualDisplay?.release()
        imageReader?.close()
        mediaProjection?.stop()
        virtualDisplay = null
        imageReader = null
        mediaProjection = null
    }

    private fun imageToBitmap(image: android.media.Image, width: Int, height: Int): Bitmap {
        val plane = image.planes[0]
        val buffer = plane.buffer
        val pixelStride = plane.pixelStride
        val rowStride = plane.rowStride
        val rowPadding = rowStride - pixelStride * width

        val bitmapWidth = width + rowPadding / pixelStride
        val bitmap = Bitmap.createBitmap(bitmapWidth, height, Bitmap.Config.ARGB_8888)
        bitmap.copyPixelsFromBuffer(buffer)

        return if (bitmapWidth != width) {
            val cropped = Bitmap.createBitmap(bitmap, 0, 0, width, height)
            bitmap.recycle()
            cropped
        } else {
            bitmap
        }
    }

    private fun getScreenMetrics(): DisplayMetrics {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        windowManager.defaultDisplay.getRealMetrics(metrics)
        return metrics
    }
}
