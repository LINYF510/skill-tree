package com.fancy.skill_tree.core.ui.render

import android.graphics.RectF
import com.fancy.skill_tree.feature.tree.NodePosition

/**
 * 视口裁剪管理器
 * 根据当前可视区域过滤需要渲染的节点和连线，避免绘制屏幕外的元素
 *
 * @param viewportWidth 视口宽度（像素）
 * @param viewportHeight 视口高度（像素）
 * @param scale 当前缩放比例
 * @param offsetX X 轴平移偏移量
 * @param offsetY Y 轴平移偏移量
 * @param margin 扩展边距，避免边缘节点闪烁
 */
class ViewportCuller(
    private val viewportWidth: Float,
    private val viewportHeight: Float,
    private val scale: Float,
    private val offsetX: Float,
    private val offsetY: Float,
    private val margin: Float = 200f
) {

    /**
     * 计算世界坐标系下的可视区域
     * 考虑缩放和平移变换的逆变换
     */
    val visibleRect: RectF by lazy {
        val left = (-offsetX - margin) / scale
        val top = (-offsetY - margin) / scale
        val right = (viewportWidth - offsetX + margin) / scale
        val bottom = (viewportHeight - offsetY + margin) / scale
        RectF(left, top, right, bottom)
    }

    /**
     * 判断节点是否在可视区域内
     *
     * @param nodePosition 节点位置
     * @param nodeWidth 节点宽度
     * @param nodeHeight 节点高度
     * @return 是否可见
     */
    fun isNodeVisible(nodePosition: NodePosition, nodeWidth: Float, nodeHeight: Float): Boolean {
        val nodeLeft = nodePosition.x - nodeWidth / 2
        val nodeTop = nodePosition.y - nodeHeight / 2
        val nodeRight = nodePosition.x + nodeWidth / 2
        val nodeBottom = nodePosition.y + nodeHeight / 2

        return RectF.intersects(
            visibleRect,
            RectF(nodeLeft, nodeTop, nodeRight, nodeBottom)
        )
    }

    /**
     * 过滤可见节点
     *
     * @param positions 所有节点位置列表
     * @param nodeWidth 节点宽度
     * @param nodeHeight 节点高度
     * @return 可见节点位置列表
     */
    fun filterVisibleNodes(
        positions: List<NodePosition>,
        nodeWidth: Float,
        nodeHeight: Float
    ): List<NodePosition> {
        return positions.filter { isNodeVisible(it, nodeWidth, nodeHeight) }
    }

    /**
     * 获取可见节点 ID 集合
     *
     * @param positions 所有节点位置列表
     * @param nodeWidth 节点宽度
     * @param nodeHeight 节点高度
     * @return 可见节点 ID 集合
     */
    fun getVisibleNodeIds(
        positions: List<NodePosition>,
        nodeWidth: Float,
        nodeHeight: Float
    ): Set<String> {
        return filterVisibleNodes(positions, nodeWidth, nodeHeight).map { it.nodeId }.toSet()
    }
}
