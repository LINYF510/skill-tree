package com.fancy.skill_tree.core.ui.accessibility

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.fancy.skill_tree.R
import com.fancy.skill_tree.feature.tree.NodePosition
import com.fancy.skill_tree.feature.tree.TreeNode

/**
 * Canvas 无障碍语义代理层
 * 在 Canvas 上方叠加透明可点击区域，使 TalkBack 能感知节点
 *
 * @param treeNodes 树节点列表
 * @param nodePositions 节点位置列表
 * @param visibleNodeIds 可见节点 ID 集合，为空时显示全部
 * @param scale 画布缩放比例
 * @param offsetX 画布 X 偏移量
 * @param offsetY 画布 Y 偏移量
 * @param nodeWidth 节点宽度（未缩放）
 * @param nodeHeight 节点高度（未缩放）
 * @param onNodeClick 节点点击回调
 * @param onNodeLongClick 节点长按回调
 */
@Composable
fun AccessibilityOverlay(
    treeNodes: List<TreeNode>,
    nodePositions: List<NodePosition>,
    visibleNodeIds: Set<String>,
    scale: Float,
    offsetX: Float,
    offsetY: Float,
    nodeWidth: Float,
    nodeHeight: Float,
    onNodeClick: (String) -> Unit,
    onNodeLongClick: (String) -> Unit
) {
    val positionMap = nodePositions.associateBy { it.nodeId }
    val density = LocalDensity.current
    val context = LocalContext.current

    Box(modifier = Modifier) {
        treeNodes.forEach { node ->
            val pos = positionMap[node.entity.id] ?: return@forEach

            if (visibleNodeIds.isNotEmpty() && node.entity.id !in visibleNodeIds) return@forEach

            val screenX = pos.x * scale + offsetX - (nodeWidth * scale) / 2
            val screenY = pos.y * scale + offsetY - (nodeHeight * scale) / 2
            val scaledWidth = nodeWidth * scale
            val scaledHeight = nodeHeight * scale

            if (scaledWidth <= 0f || scaledHeight <= 0f) return@forEach

            Box(
                modifier = Modifier
                    .offset { IntOffset(screenX.toInt(), screenY.toInt()) }
                    .size(with(density) { scaledWidth.toDp() }, with(density) { scaledHeight.toDp() })
                    .semantics(mergeDescendants = true) {
                        contentDescription = buildNodeDescription(node, context)
                        role = Role.Button
                        stateDescription = buildNodeStateDescription(node, context)
                    }
                    .clickable(
                        onClick = { onNodeClick(node.entity.id) },
                        role = Role.Button,
                        onClickLabel = context.getString(R.string.a11y_node_click_detail, node.entity.title)
                    )
            )
        }
    }
}

/**
 * 构建节点的无障碍描述
 * 包含节点类型、标题、等级、子节点数、内容状态
 *
 * @param node 树节点
 * @param context 上下文，用于获取字符串资源
 * @return 无障碍描述文本
 */
private fun buildNodeDescription(node: TreeNode, context: android.content.Context): String {
    return buildString {
        append(if (node.entity.nodeType == "ABILITY") context.getString(R.string.a11y_node_type_ability) else context.getString(R.string.a11y_node_type_resource))
        append(node.entity.title)
        append(context.getString(R.string.a11y_node_level, node.entity.level))
        if (node.children.isNotEmpty()) {
            append(context.getString(R.string.a11y_node_children, node.children.size))
        }
        if (!node.entity.content.isNullOrBlank()) {
            append(context.getString(R.string.a11y_node_has_content))
        }
    }
}

/**
 * 构建节点的状态描述
 *
 * @param node 树节点
 * @param context 上下文，用于获取字符串资源
 * @return 状态描述文本
 */
private fun buildNodeStateDescription(node: TreeNode, context: android.content.Context): String {
    return buildString {
        append(context.getString(R.string.a11y_state_click_detail))
        if (node.children.isNotEmpty()) {
            append(context.getString(R.string.a11y_state_long_press_menu))
        }
    }
}
