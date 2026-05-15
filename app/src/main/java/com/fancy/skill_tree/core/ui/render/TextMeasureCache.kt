package com.fancy.skill_tree.core.ui.render

import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle

/**
 * 文本缓存管理器
 * 缓存已测量的 TextLayoutResult，避免每帧重复测量文本布局
 * 使用 LinkedHashMap 实现 LRU 策略（accessOrder = true）
 *
 * @param maxSize 缓存最大条目数，默认 500
 */
class TextMeasureCache(private val maxSize: Int = 500) {

    private val cache = object : LinkedHashMap<String, TextLayoutResult>(maxSize, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, TextLayoutResult>): Boolean {
            return size > maxSize
        }
    }

    /**
     * 获取或测量文本布局
     * 如果缓存中存在对应 key 则直接返回，否则使用 measurer 测量并缓存
     *
     * @param key 缓存键，应包含影响渲染结果的所有变量
     * @param text 待测量文本
     * @param style 文本样式
     * @param measurer 文本测量器
     * @return 文本布局结果
     */
    fun getOrMeasure(
        key: String,
        text: String,
        style: TextStyle,
        measurer: TextMeasurer
    ): TextLayoutResult {
        return cache.getOrPut(key) {
            measurer.measure(text = text, style = style)
        }
    }

    /**
     * 清除所有缓存
     * 在节点数据变化时调用
     */
    fun clear() = cache.clear()
}
