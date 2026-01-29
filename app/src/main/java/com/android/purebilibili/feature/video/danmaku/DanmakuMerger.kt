package com.android.purebilibili.feature.video.danmaku

import com.bytedance.danmaku.render.engine.data.DanmakuData
import com.bytedance.danmaku.render.engine.render.draw.text.TextData
import android.util.Log

/**
 * 弹幕合并工具
 *
 * 用于合并短时间内出现的重复弹幕，减少屏幕遮挡
 */
object DanmakuMerger {
    private const val TAG = "DanmakuMerger"

    /**
     * 合并重复弹幕
     *
     * @param list 原始弹幕列表 (必须已按时间排序)
     * @param intervalMs 合并的时间窗口 (毫秒)，默认 500ms
     * @return 合并后的弹幕列表
     */
    /**
     * 合并重复弹幕
     *
     * @param list 原始弹幕列表 (必须已按时间排序)
     * @param intervalMs 合并的时间窗口 (毫秒)，默认 500ms
     * @return Pair(标准弹幕列表, 高级合并弹幕列表)
     *         标准弹幕继续在原层渲染，合并后的重复弹幕转换为高级弹幕在屏幕中央显示
     */
    fun merge(list: List<DanmakuData>, intervalMs: Long = 500): Pair<List<DanmakuData>, List<AdvancedDanmakuData>> {
        if (list.isEmpty()) return Pair(list, emptyList())

        val startTime = System.currentTimeMillis()
        val standardList = mutableListOf<DanmakuData>()
        val advancedMergedList = mutableListOf<AdvancedDanmakuData>()
        
        // 1. 分离 TextData 和其他数据
        val textDanmakus = mutableListOf<TextData>()
        
        list.forEach { 
            if (it is TextData) textDanmakus.add(it)
            else standardList.add(it) // 非文本弹幕直接保留在标准列表
        }
        
        // 2. 对 TextData 按内容分组
        val groupedByContent = textDanmakus.groupBy { it.text }
        
        // 3. 统计每组的数量并排序，找出"Top High Energy"
        // [API 利用] 结合 weight (权重) 进行评分。
        // 评分 = 数量 + (最大权重 * 0.5)
        // 这样高优先级的弹幕更容易被选为高能弹幕
        val topRepetitiveGroups = groupedByContent.entries
            .filter { it.value.size >= 3 }
            .sortedByDescending { entry ->
                val count = entry.value.size
                var maxWeight = 0
                
                // 检查是否包含 WeightedTextData 并获取最大权重
                if (entry.value.isNotEmpty()) {
                    entry.value.forEach { item ->
                        if (item is WeightedTextData) {
                            if (item.weight > maxWeight) maxWeight = item.weight
                        }
                    }
                }
                
                // 综合评分公式
                count + (maxWeight * 0.5)
            }
            .take(2) // 只取前 2 名
            .map { it.key }
            .toSet()
            
        var mergedCount = 0
        
        groupedByContent.forEach { (text, items) ->
            if (items.size == 1) {
                // 没有重复的，保持为标准弹幕
                standardList.add(items[0])
            } else {
                // 是高能弹幕组吗？
                val isHighEnergy = topRepetitiveGroups.contains(text)
                
                // 对同一内容的弹幕进行时间聚类
                var currentBatch = mutableListOf<TextData>()
                
                for (item in items) {
                    if (currentBatch.isEmpty()) {
                        currentBatch.add(item)
                    } else {
                        val lastItem = currentBatch.last()
                        if (item.showAtTime - lastItem.showAtTime <= intervalMs) {
                            currentBatch.add(item)
                        } else {
                            // 结算上一批
                            processBatch(currentBatch, standardList, advancedMergedList, isHighEnergy)
                            if (currentBatch.size > 1) mergedCount += (currentBatch.size - 1)
                            
                            // 开启新的一批
                            currentBatch = mutableListOf()
                            currentBatch.add(item)
                        }
                    }
                }
                
                // 结算最后一批
                if (currentBatch.isNotEmpty()) {
                    processBatch(currentBatch, standardList, advancedMergedList, isHighEnergy)
                    if (currentBatch.size > 1) mergedCount += (currentBatch.size - 1)
                }
            }
        }
        
        // 3. 重新按时间排序
        standardList.sortBy { it.showAtTime }
        advancedMergedList.sortBy { it.startTimeMs }
        
        Log.d(TAG, "Merged result: ${standardList.size} standard, ${advancedMergedList.size} high-energy. Reduced $mergedCount items.")
        
        return Pair(standardList, advancedMergedList)
    }
    
    /**
     * 处理一批重复弹幕
     */
    private fun processBatch(
        batch: List<TextData>, 
        standardOut: MutableList<DanmakuData>, 
        advancedOut: MutableList<AdvancedDanmakuData>,
        isHighEnergy: Boolean
    ) {
        if (batch.isEmpty()) return
        
        if (batch.size == 1) {
            standardOut.add(batch[0])
        } else {
            if (isHighEnergy) {
                // [变更] 只有 Top 2 的刷屏才升级为高级弹幕（屏幕中央）
                val base = batch[0]
                val count = batch.size
                
                // 计算时间跨度，至少给 1.5 秒的动画时间，让计数效果更明显
                val timeSpan = batch.last().showAtTime - batch.first().showAtTime
                val accumulationTime = timeSpan.coerceAtLeast(1500L)
                
                val advanced = AdvancedDanmakuData(
                    id = "merged_${base.hashCode()}_${System.nanoTime()}",
                    content = base.text ?: "", // 内容不带 xN，由 Overlay 动态生成
                    startTimeMs = base.showAtTime,
                    durationMs = accumulationTime + 3000L, // 动画结束后停留 3 秒
                    startX = 0.5f,
                    startY = 0.5f, 
                    endX = 0.5f,
                    endY = 0.5f,
                    fontSize = 45f, 
                    color = 0xFFD700, 
                    alpha = 1.0f,
                    motionType = "Static",
                    maxCount = count,
                    accumulationDurationMs = accumulationTime 
                )
                advancedOut.add(advanced)
            } else {
                // [变更] 普通的重复弹幕（非 Top 2），只是普通的合并，不占领屏幕中央
                // 继续以标准弹幕形式存在，但带上 xN 标记
                standardOut.add(combineBatch(batch))
            }
        }
    }
    /**
     * 将一批 TextData 合并为一个
     */
    private fun combineBatch(batch: List<TextData>): TextData {
        if (batch.size == 1) return batch[0]
        
        // 取第一个作为基准
        val base = batch[0]
        val count = batch.size
        
        // 创建一个新的 TextData (浅拷贝属性)
        // 注意：TextData 是 Java 类，使用 apply 设置属性
        val mergedText = TextData()
        mergedText.text = "${base.text} x$count"
        mergedText.showAtTime = base.showAtTime
        mergedText.textColor = base.textColor
        mergedText.textSize = base.textSize
        mergedText.layerType = base.layerType
        mergedText.typeface = base.typeface
            
        return mergedText
    }
}
