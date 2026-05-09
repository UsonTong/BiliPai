package com.android.purebilibili.feature.screenshot

import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.view.PixelCopy
import android.view.View
import android.view.Window
import com.android.purebilibili.core.util.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

private const val APP_SCREENSHOT_TAG = "AppScreenshot"
private const val APP_SCREENSHOT_RELATIVE_PATH = "Pictures/BiliPai/Screenshots"

suspend fun captureAndSaveAppScreenshot(
    activity: Activity,
    timestampMs: Long = System.currentTimeMillis()
): AppScreenshotResult {
    val bitmap = captureCurrentAppWindow(activity = activity)
        ?: return AppScreenshotResult.CaptureFailed
    val fileName = buildAppScreenshotFileName(timestampMs = timestampMs)

    return try {
        if (saveAppScreenshotBitmapToGallery(activity, bitmap, fileName)) {
            AppScreenshotResult.Success
        } else {
            AppScreenshotResult.SaveFailed
        }
    } finally {
        bitmap.recycle()
    }
}

suspend fun captureCurrentAppWindow(activity: Activity): Bitmap? = withContext(Dispatchers.Main.immediate) {
    val window = activity.window
    val decorView = window.decorView
    if (!decorView.isAttachedToWindow || decorView.width <= 0 || decorView.height <= 0) {
        Logger.w(APP_SCREENSHOT_TAG, "Window capture skipped because decorView is not ready")
        return@withContext null
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        captureWindowWithPixelCopy(window = window, decorView = decorView)
            ?: drawDecorViewFallback(decorView)
    } else {
        drawDecorViewFallback(decorView)
    }
}

private suspend fun captureWindowWithPixelCopy(
    window: Window,
    decorView: View
): Bitmap? = suspendCancellableCoroutine { continuation ->
    val bitmap = Bitmap.createBitmap(decorView.width, decorView.height, Bitmap.Config.ARGB_8888)
    val sourceRect = Rect(0, 0, decorView.width, decorView.height)

    try {
        PixelCopy.request(
            window,
            sourceRect,
            bitmap,
            { result ->
                if (!continuation.isActive) {
                    bitmap.recycle()
                    return@request
                }
                if (result == PixelCopy.SUCCESS) {
                    continuation.resume(bitmap)
                } else {
                    Logger.w(APP_SCREENSHOT_TAG, "PixelCopy window capture failed with code: $result")
                    bitmap.recycle()
                    continuation.resume(null)
                }
            },
            Handler(Looper.getMainLooper())
        )
    } catch (e: Exception) {
        Logger.e(APP_SCREENSHOT_TAG, "PixelCopy window capture exception", e)
        bitmap.recycle()
        if (continuation.isActive) {
            continuation.resume(null)
        }
    }

    continuation.invokeOnCancellation {
        bitmap.recycle()
    }
}

private fun drawDecorViewFallback(decorView: View): Bitmap? {
    return runCatching {
        Bitmap.createBitmap(decorView.width, decorView.height, Bitmap.Config.ARGB_8888).also { bitmap ->
            decorView.draw(Canvas(bitmap))
        }
    }.onFailure { throwable ->
        Logger.e(APP_SCREENSHOT_TAG, "DecorView draw fallback failed", throwable)
    }.getOrNull()
}

fun cropAppScreenshotBitmap(
    bitmap: Bitmap,
    cropRect: AppScreenshotCropRect
): Bitmap? {
    return runCatching {
        Bitmap.createBitmap(
            bitmap,
            cropRect.left,
            cropRect.top,
            cropRect.width,
            cropRect.height
        )
    }.onFailure { throwable ->
        Logger.e(APP_SCREENSHOT_TAG, "Failed to crop app screenshot", throwable)
    }.getOrNull()
}

internal suspend fun saveAppScreenshotBitmapToGallery(
    context: Context,
    bitmap: Bitmap,
    fileName: String = buildAppScreenshotFileName()
): Boolean = withContext(Dispatchers.IO) {
    val resolver = context.contentResolver
    val values = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
        put(MediaStore.Images.Media.MIME_TYPE, "image/png")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(MediaStore.Images.Media.IS_PENDING, 1)
            put(MediaStore.Images.Media.RELATIVE_PATH, APP_SCREENSHOT_RELATIVE_PATH)
        }
    }
    val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        ?: return@withContext false

    try {
        val wrote = resolver.openOutputStream(uri)?.use { output ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
        } ?: false
        if (!wrote) {
            resolver.delete(uri, null, null)
            return@withContext false
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
        }
        true
    } catch (e: Exception) {
        Logger.e(APP_SCREENSHOT_TAG, "Failed to save app screenshot", e)
        resolver.delete(uri, null, null)
        false
    }
}
