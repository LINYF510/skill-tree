package com.fancy.skill_tree.core.ui.animation

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec

/**
 * 动画配置常量
 * 集中管理所有动画的时长、缓动函数等参数
 */
object AnimationConfig {

    const val NODE_CREATE_DURATION = 400
    const val NODE_GROW_DURATION = 800
    const val NODE_GROW_LAYER_DELAY = 200
    const val LINK_GROW_DURATION = 500
    const val ACHIEVEMENT_ENTER_DURATION = 400
    const val SEARCH_PULSE_DURATION = 600
    const val SELECTED_PULSE_DURATION = 600
    const val PAGE_TRANSITION_DURATION = 400
    const val ACHIEVEMENT_DISPLAY_DURATION = 4000L

    /**
     * 缩放回弹弹簧动画参数
     * 阻尼比 MediumBouncy 提供适度回弹，刚度 Medium 保证流畅感
     */
    val ZOOM_BOUNCE_SPRING = SpringSpec<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMedium
    )
}

/**
 * 动画参数数据类
 * 在 Canvas 绘制时传递的动画状态值，用于驱动各动画效果
 *
 * @param newlyCreatedNodeId 新创建节点的 ID，用于触发创建动画
 * @param creationScale 创建动画的缩放进度 (0..1)
 * @param growthProgress 树生长动画的整体进度，值等于当前展开到的深度
 * @param searchPulseAlpha 搜索高亮脉冲的透明度
 * @param selectedPulseAlpha 选中节点脉冲的透明度
 * @param selectedNodeId 当前选中节点的 ID
 * @param isSearchActive 是否处于搜索模式
 */
data class AnimationParams(
    val newlyCreatedNodeId: String? = null,
    val creationScale: Float = 1f,
    val growthProgress: Float = 100f,
    val searchPulseAlpha: Float = 0f,
    val selectedPulseAlpha: Float = 0f,
    val selectedNodeId: String? = null,
    val isSearchActive: Boolean = false
)
