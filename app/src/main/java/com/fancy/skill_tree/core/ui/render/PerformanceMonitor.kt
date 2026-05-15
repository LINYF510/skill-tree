package com.fancy.skill_tree.core.ui.render

/**
 * 简单的性能监控工具
 * 通过 isDebugEnabled 控制是否记录帧时间，默认由外部注入 debug 状态
 */
object PerformanceMonitor {

    private val frameTimes = mutableListOf<Long>()

    /**
     * 是否启用监控
     * 由调用方根据构建类型设置
     */
    var isDebugEnabled: Boolean = false

    /**
     * 记录单帧渲染时间
     *
     * @param durationMs 帧渲染耗时（毫秒）
     */
    fun recordFrameTime(durationMs: Long) {
        if (isDebugEnabled) {
            frameTimes.add(durationMs)
            if (frameTimes.size > 100) frameTimes.removeAt(0)
        }
    }

    /**
     * 获取平均帧时间
     *
     * @return 最近 100 帧的平均耗时（毫秒），无数据时返回 0
     */
    fun getAverageFrameTime(): Long {
        if (frameTimes.isEmpty()) return 0
        return frameTimes.average().toLong()
    }

    /**
     * 获取当前估算 FPS
     *
     * @return 估算帧率，无数据时返回 60f
     */
    fun getFps(): Float {
        val avg = getAverageFrameTime()
        if (avg == 0L) return 60f
        return 1000f / avg
    }

    /**
     * 清除所有记录
     */
    fun clear() = frameTimes.clear()
}
