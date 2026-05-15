package com.fancy.skill_tree.core.ui.render

import android.util.LruCache
import androidx.compose.ui.graphics.Path

/**
 * 预计算并缓存贝塞尔曲线 Path
 * 只在节点位置变化时重新计算，避免每帧重复创建 Path 对象
 * 使用 android.util.LruCache 管理缓存大小
 *
 * @param maxSize 缓存最大条目数，默认 1000
 */
class ConnectionPathCache(private val maxSize: Int = 1000) {

    private val cache = LruCache<String, Path>(maxSize)

    /**
     * 获取或创建贝塞尔曲线路径
     * 如果缓存中存在对应 key 则直接返回，否则创建新路径并缓存
     *
     * @param key 缓存键，应包含起止坐标信息
     * @param startX 起点X坐标
     * @param startY 起点Y坐标
     * @param endX 终点X坐标
     * @param endY 终点Y坐标
     * @return 贝塞尔曲线路径
     */
    fun getOrCreatePath(
        key: String,
        startX: Float, startY: Float,
        endX: Float, endY: Float
    ): Path {
        return cache.get(key) ?: run {
            Path().apply {
                moveTo(startX, startY)
                cubicTo(
                    startX, (startY + endY) / 2,
                    endX, (startY + endY) / 2,
                    endX, endY
                )
            }.also { cache.put(key, it) }
        }
    }

    /**
     * 清除所有缓存
     * 在 TreeLayoutCache 失效时调用
     */
    fun clear() = cache.evictAll()
}
