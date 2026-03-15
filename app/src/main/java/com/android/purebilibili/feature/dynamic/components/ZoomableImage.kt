package com.android.purebilibili.feature.dynamic.components

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculateCentroidSize
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateRotation
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.toSize
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.request.ImageRequest
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

private enum class ZoomableImageGestureMode {
    UNDECIDED,
    HORIZONTAL_PAGER,
    IMAGE_INTERACTION,
    VERTICAL_DISMISS
}

internal const val ZOOMABLE_IMAGE_TAG = "zoomable_image"

/**
 * 可缩放的图片组件
 * - 支持双指缩放
 * - 支持双击放大
 * - 支持长图滑动
 * - 自动处理边界限制
 */
@Composable
fun ZoomableImage(
    model: Any?,
    imageLoader: ImageLoader,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    onZoomChange: (Float) -> Unit = {},
    onDisplayRectChange: (Rect?) -> Unit = {},
    onVerticalDismissDragStart: () -> Unit = {},
    onVerticalDismissDrag: (Float) -> Unit = {},
    onVerticalDismissDragEnd: () -> Unit = {},
    onVerticalDismissDragCancel: () -> Unit = {},
    onLongPress: () -> Unit = {},
    onClick: () -> Unit = {}
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    
    // 图片原始尺寸
    var imageSize by remember { mutableStateOf(IntSize.Zero) }
    // 容器尺寸
    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    
    // 是否正在加载
    var isLoading by remember { mutableStateOf(true) }
    var isLongImage by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    fun resolveDisplayedRectOrNull(): Rect? {
        if (containerSize == IntSize.Zero || imageSize == IntSize.Zero) return null

        val fitScale = min(
            containerSize.width.toFloat() / imageSize.width,
            containerSize.height.toFloat() / imageSize.height
        )
        val displayWidth = imageSize.width * fitScale * scale
        val displayHeight = imageSize.height * fitScale * scale
        val centerX = containerSize.width / 2f + offsetX
        val centerY = containerSize.height / 2f + offsetY
        return Rect(
            left = centerX - displayWidth / 2f,
            top = centerY - displayHeight / 2f,
            right = centerX + displayWidth / 2f,
            bottom = centerY + displayHeight / 2f
        )
    }

    LaunchedEffect(containerSize, imageSize, scale, offsetX, offsetY) {
        onDisplayRectChange(resolveDisplayedRectOrNull())
    }
    
    // 双击放大逻辑
    fun onDoubleTap(tapOffset: Offset) {
        if (scale > 1f) {
            // 恢复原大小
            scale = 1f
            offsetX = 0f
            offsetY = 0f
            onZoomChange(1f)
        } else {
            // 放大 2.5 倍
            scale = 2.5f
            
            // 计算偏移量，使点击点居中
            if (containerSize != IntSize.Zero) {
                val centerX = containerSize.width / 2f
                val centerY = containerSize.height / 2f
                
                offsetX = (centerX - tapOffset.x) * (scale - 1)
                offsetY = (centerY - tapOffset.y) * (scale - 1)
                
                // 边界限制
                val fitScale = min(
                    containerSize.width.toFloat() / imageSize.width,
                    containerSize.height.toFloat() / imageSize.height
                )
                val displayWidth = imageSize.width * fitScale * scale
                val displayHeight = imageSize.height * fitScale * scale
                
                val maxOffsetX = max(0f, (displayWidth - containerSize.width) / 2f)
                val maxOffsetY = max(0f, (displayHeight - containerSize.height) / 2f)
                
                offsetX = offsetX.coerceIn(-maxOffsetX, maxOffsetX)
                offsetY = offsetY.coerceIn(-maxOffsetY, maxOffsetY)
            }
            onZoomChange(scale)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { containerSize = it }
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { onDoubleTap(it) },
                    onLongPress = { onLongPress() },
                    onTap = { onClick() }
                )
            }
            .pointerInput(Unit) {
                // 手势监听：缩放 + 拖拽
                awaitEachGesture {
                    var zoom = 1f
                    var pan = Offset.Zero
                    var pastTouchSlop = false
                    val touchSlop = viewConfiguration.touchSlop
                    var isMultiTouch = false
                    var gestureMode = ZoomableImageGestureMode.UNDECIDED
                    var verticalDismissStarted = false
                    var gestureCanceled = false
                    
                    awaitFirstDown(requireUnconsumed = false)
                    
                    do {
                        val event = awaitPointerEvent()
                        val canceled = event.changes.any { it.isConsumed }
                        if (canceled) {
                            gestureCanceled = true
                            break
                        }

                        if (event.changes.size > 1) {
                            isMultiTouch = true
                        }

                        val zoomChange = event.calculateZoom()
                        val panChange = event.calculatePan()

                        if (!pastTouchSlop) {
                            zoom *= zoomChange
                            pan += panChange

                            val centroidSize = event.calculateCentroidSize(useCurrent = false)
                            val zoomMotion = abs(1 - zoom) * centroidSize
                            val panMotion = pan.getDistance()

                            if (zoomMotion > touchSlop ||
                                panMotion > touchSlop
                            ) {
                                pastTouchSlop = true
                                gestureMode = when {
                                    isMultiTouch || !shouldEnableImagePreviewVerticalDismiss(scale) || zoomMotion > panMotion -> {
                                        ZoomableImageGestureMode.IMAGE_INTERACTION
                                    }
                                    abs(pan.y) > abs(pan.x) * 1.12f -> {
                                        ZoomableImageGestureMode.VERTICAL_DISMISS
                                    }
                                    else -> ZoomableImageGestureMode.HORIZONTAL_PAGER
                                }

                                if (gestureMode == ZoomableImageGestureMode.VERTICAL_DISMISS) {
                                    verticalDismissStarted = true
                                    onVerticalDismissDragStart()
                                }
                            }
                        }

                        if (pastTouchSlop) {
                            when (gestureMode) {
                                ZoomableImageGestureMode.VERTICAL_DISMISS -> {
                                    if (panChange.y != 0f) {
                                        onVerticalDismissDrag(panChange.y)
                                    }
                                    event.changes.forEach {
                                        if (it.position != it.previousPosition) {
                                            it.consume()
                                        }
                                    }
                                }
                                ZoomableImageGestureMode.IMAGE_INTERACTION -> {
                                    val centroid = event.calculateCentroid(useCurrent = false)
                                    if (zoomChange != 1f || panChange != Offset.Zero) {
                                        val oldScale = scale
                                        scale = (scale * zoomChange).coerceIn(1f, 5f)

                                        if (oldScale != scale) {
                                            val zoomFactor = scale / oldScale
                                            val dx = (1 - zoomFactor) * (centroid.x - containerSize.width / 2f - offsetX)
                                            val dy = (1 - zoomFactor) * (centroid.y - containerSize.height / 2f - offsetY)
                                            offsetX += dx
                                            offsetY += dy
                                        }

                                        offsetX += panChange.x
                                        offsetY += panChange.y

                                        if (containerSize != IntSize.Zero && imageSize != IntSize.Zero) {
                                            val fitScale = min(
                                                containerSize.width.toFloat() / imageSize.width,
                                                containerSize.height.toFloat() / imageSize.height
                                            )

                                            val displayWidth = imageSize.width * fitScale * scale
                                            val displayHeight = imageSize.height * fitScale * scale

                                            val maxOffsetX = max(0f, (displayWidth - containerSize.width) / 2f)
                                            val maxOffsetY = max(0f, (displayHeight - containerSize.height) / 2f)

                                            offsetX = offsetX.coerceIn(-maxOffsetX, maxOffsetX)
                                            offsetY = offsetY.coerceIn(-maxOffsetY, maxOffsetY)
                                        }

                                        onZoomChange(scale)
                                    }

                                    if (isMultiTouch || scale > 1.01f) {
                                        event.changes.forEach {
                                            if (it.position != it.previousPosition) {
                                                it.consume()
                                            }
                                        }
                                    }
                                }
                                ZoomableImageGestureMode.HORIZONTAL_PAGER,
                                ZoomableImageGestureMode.UNDECIDED -> Unit
                            }
                        }
                    } while (!gestureCanceled && event.changes.any { it.pressed })

                    if (verticalDismissStarted) {
                        if (gestureCanceled) {
                            onVerticalDismissDragCancel()
                        } else {
                            onVerticalDismissDragEnd()
                        }
                    }
                }
            }
    ) {
        AsyncImage(
            model = model,
            contentDescription = contentDescription,
            imageLoader = imageLoader,
            modifier = Modifier
                .fillMaxSize()
                .align(Alignment.Center)
                .testTag(ZOOMABLE_IMAGE_TAG)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offsetX
                    translationY = offsetY
                },
            onSuccess = { state ->
                isLoading = false
                val originalSize = state.painter.intrinsicSize
                if (originalSize.width > 0 && originalSize.height > 0) {
                    imageSize = IntSize(originalSize.width.toInt(), originalSize.height.toInt())
                    
                    // 判断是否为长图 (高宽比 > 3 且高度 > 容器高度)
                    isLongImage = originalSize.height / originalSize.width > 3
                    
                    // 初始长图处理：如果不需要双击就直接看长图细节，可以把 scale 设置为 fillWidth 对应的 scale
                    // 但标准的图片预览通常还是先看全图，再放大
                }
            },
            // 使用 Fit 模式确保初始完整显示
            contentScale = ContentScale.Fit
        )
    }
}
