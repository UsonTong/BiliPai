package com.android.purebilibili.feature.video.ui.overlay

import android.graphics.Color as AndroidColor
import android.view.ViewGroup
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.android.purebilibili.feature.live.LiveDanmakuItem
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.bytedance.danmaku.render.engine.control.DanmakuController
import com.bytedance.danmaku.render.engine.data.DanmakuData
import com.bytedance.danmaku.render.engine.utils.LAYER_TYPE_BOTTOM_CENTER
import com.bytedance.danmaku.render.engine.utils.LAYER_TYPE_SCROLL
import com.bytedance.danmaku.render.engine.utils.LAYER_TYPE_TOP_CENTER
import com.bytedance.danmaku.render.engine.DanmakuView
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/**
 * 直播弹幕图层
 * 使用 ByteDance DanmakuRenderEngine 渲染
 * 
 * 修复记录:
 * - 使用 mutableStateOf 替代 object 管理状态
 * - 添加 isActive 检查防止协程泄漏
 * - 添加 try-catch 防止崩溃
 */
@Composable
fun LiveDanmakuOverlay(
    danmakuFlow: SharedFlow<LiveDanmakuItem>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    // 使用稳定的状态管理
    var controller by remember { mutableStateOf<DanmakuController?>(null) }
    var startTime by remember { mutableStateOf(0L) }
    var isStarted by remember { mutableStateOf(false) }
    val danmakuList = remember { mutableListOf<DanmakuData>() }

    AndroidView(
        factory = { ctx ->
            DanmakuView(ctx).apply {
                try {
                    // 设置透明背景
                    setBackgroundColor(AndroidColor.TRANSPARENT)
                    
                    // 设置布局参数
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    
                    // 保存引用
                    controller = this.controller
                    startTime = System.currentTimeMillis()
                    
                    android.util.Log.d("LiveDanmakuOverlay", "🟢 DanmakuView created, starting controller")
                    
                    // 启动渲染引擎
                    this.controller.start(0)
                    isStarted = true
                } catch (e: Exception) {
                    android.util.Log.e("LiveDanmakuOverlay", "❌ DanmakuView init failed: ${e.message}")
                }
            }
        },
        modifier = modifier.fillMaxSize(),
        update = { view ->
            try {
                // 确保控制器正在运行
                val ctrl = controller
                if (ctrl != null && !isStarted) {
                    android.util.Log.d("LiveDanmakuOverlay", "🟡 Controller not started, starting...")
                    val currentTime = System.currentTimeMillis() - startTime
                    ctrl.start(currentTime)
                    isStarted = true
                }
            } catch (e: Exception) {
                android.util.Log.e("LiveDanmakuOverlay", "❌ Update failed: ${e.message}")
            }
        }
    )

    // 持续驱动播放时间更新 - 每帧调用 start() 来推进渲染
    LaunchedEffect(Unit) {
        while (isActive) { // 使用 isActive 检查，协程取消时自动退出
            try {
                val ctrl = controller
                if (ctrl != null && isStarted) {
                    val currentTime = System.currentTimeMillis() - startTime
                    // 定期调用 start() 更新播放进度
                    ctrl.start(currentTime)
                }
            } catch (e: Exception) {
                android.util.Log.e("LiveDanmakuOverlay", "❌ Render loop error: ${e.message}")
            }
            delay(50) // ~20fps 足够流畅
        }
    }
    
    // 监听弹幕流
    LaunchedEffect(danmakuFlow) {
        danmakuFlow.collect { item ->
            try {
                val ctrl = controller ?: return@collect
                if (!isStarted) return@collect
                
                android.util.Log.d("LiveDanmakuOverlay", "🔴 Received: ${item.text}")
                
                // 计算当前相对时间
                val currentTime = System.currentTimeMillis() - startTime
                val danmakuData = createDanmakuData(item, currentTime, context, ctrl)
                
                // 添加到列表 (同步操作，避免并发问题)
                synchronized(danmakuList) {
                    // 移除过期弹幕 (20秒前)
                    danmakuList.removeAll { it.showAtTime < currentTime - 20_000 }
                    danmakuList.add(danmakuData)
                    // 排序
                    danmakuList.sortBy { it.showAtTime }
                    
                    android.util.Log.d("LiveDanmakuOverlay", "🔴 setData: size=${danmakuList.size}, time=$currentTime")
                    
                    // 更新数据
                    ctrl.setData(danmakuList.toList(), currentTime)
                }
                ctrl.invalidateView()
            } catch (e: Exception) {
                android.util.Log.e("LiveDanmakuOverlay", "❌ Danmaku collect error: ${e.message}")
            }
        }
    }
    
    // 清理
    DisposableEffect(Unit) {
        onDispose {
            android.util.Log.d("LiveDanmakuOverlay", "🔴 Disposing DanmakuView")
            try {
                controller?.stop()
                synchronized(danmakuList) {
                    danmakuList.clear()
                }
                isStarted = false
                controller = null
            } catch (e: Exception) {
                android.util.Log.e("LiveDanmakuOverlay", "❌ Dispose error: ${e.message}")
            }
        }
    }
}


private fun createDanmakuData(
    item: LiveDanmakuItem, 
    currentTime: Long, 
    context: android.content.Context,
    controller: DanmakuController?
): DanmakuData {
    val textSize = 42f
    val layerType = when (item.mode) {
        4 -> LAYER_TYPE_BOTTOM_CENTER
        5 -> LAYER_TYPE_TOP_CENTER
        else -> LAYER_TYPE_SCROLL
    }
    
    val textColor = if (item.color == 0) {
        AndroidColor.WHITE
    } else {
        (0xFF000000 or item.color.toLong()).toInt()
    }

    return com.android.purebilibili.feature.video.danmaku.createBitmapDanmaku(
        context = context,
        text = item.text,
        textColor = textColor,
        textSize = textSize,
        layerType = layerType,
        showAtTime = currentTime + 50L,
        onUpdate = {
            // 当图片加载完成后刷新视图
            controller?.invalidateView()
        }
    )
}
