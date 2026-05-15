package com.fancy.skill_tree.core.ui.render

import com.fancy.skill_tree.core.domain.entity.SkillNodeEntity
import com.fancy.skill_tree.feature.tree.NodePosition
import com.fancy.skill_tree.feature.tree.TreeNode

/**
 * 树布局结果缓存
 * 只在节点数据变化时重新计算，避免 Canvas 每帧重复计算布局
 */
class TreeLayoutCache {

    private var cachedNodeHash: Int? = null
    private var cachedTreeNodes: List<TreeNode>? = null
    private var cachedPositions: List<NodePosition>? = null
    private var cachedCanvasWidth: Float? = null
    private var cachedCanvasHeight: Float? = null

    /**
     * 获取或计算树节点结构
     * 如果节点数据未变化则使用缓存
     *
     * @param nodes 节点实体列表
     * @param compute 实际计算函数
     * @return 树节点列表
     */
    fun getOrComputeTreeNodes(
        nodes: List<SkillNodeEntity>,
        compute: (List<SkillNodeEntity>) -> List<TreeNode>
    ): List<TreeNode> {
        val currentHash = nodes.map { it.id to it.updatedAt }.hashCode()
        if (cachedNodeHash == currentHash && cachedTreeNodes != null) {
            return cachedTreeNodes ?: compute(nodes)
        }
        val result = compute(nodes)
        cachedNodeHash = currentHash
        cachedTreeNodes = result
        cachedPositions = null
        return result
    }

    /**
     * 获取或计算节点位置
     * 如果节点数据和画布尺寸未变化则使用缓存
     *
     * @param treeNodes 树节点列表
     * @param canvasWidth 画布宽度
     * @param canvasHeight 画布高度
     * @param compute 实际计算函数
     * @return 节点位置列表
     */
    fun getOrComputePositions(
        treeNodes: List<TreeNode>,
        canvasWidth: Float,
        canvasHeight: Float,
        compute: (List<TreeNode>, Float, Float) -> List<NodePosition>
    ): List<NodePosition> {
        if (cachedPositions != null
            && cachedCanvasWidth == canvasWidth
            && cachedCanvasHeight == canvasHeight
        ) {
            return cachedPositions ?: compute(treeNodes, canvasWidth, canvasHeight)
        }
        val result = compute(treeNodes, canvasWidth, canvasHeight)
        cachedPositions = result
        cachedCanvasWidth = canvasWidth
        cachedCanvasHeight = canvasHeight
        return result
    }

    /**
     * 使缓存失效
     * 在节点数据变化时调用
     */
    fun invalidate() {
        cachedNodeHash = null
        cachedTreeNodes = null
        cachedPositions = null
        cachedCanvasWidth = null
        cachedCanvasHeight = null
    }
}
