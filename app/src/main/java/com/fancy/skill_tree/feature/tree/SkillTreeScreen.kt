package com.fancy.skill_tree.feature.tree

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fancy.skill_tree.core.domain.entity.SkillNodeEntity
import com.fancy.skill_tree.ui.theme.AbilityGreen
import com.fancy.skill_tree.ui.theme.BackgroundDark
import com.fancy.skill_tree.ui.theme.LinkOrange
import com.fancy.skill_tree.ui.theme.ResourcePurple
import com.fancy.skill_tree.ui.theme.SurfaceDark
import com.fancy.skill_tree.ui.theme.TextPrimary
import com.fancy.skill_tree.ui.theme.TextSecondary
import java.util.UUID
import kotlin.math.abs

private const val NODE_WIDTH = 140f
private const val NODE_HEIGHT = 70f
private const val HORIZONTAL_SPACING = 60f
private const val VERTICAL_SPACING = 100f
private const val CORNER_RADIUS = 16f
private const val MIN_SCALE = 0.3f
private const val MAX_SCALE = 3.0f

private val NODE_TYPE_ABILITY = "ABILITY"
private val NODE_TYPE_RESOURCE = "RESOURCE"

data class TreeNode(
    val entity: SkillNodeEntity,
    var x: Float = 0f,
    var y: Float = 0f,
    val children: MutableList<TreeNode> = mutableListOf()
)

data class NodePosition(
    val nodeId: String,
    val x: Float,
    val y: Float
)

@Composable
fun SkillTreeScreen(
    modifier: Modifier = Modifier,
    viewModel: SkillTreeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        when {
            uiState.isLoading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.primary
                )
            }
            uiState.errorMessage != null -> {
                Text(
                    text = uiState.errorMessage ?: "未知错误",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            else -> {
                val displayNodes = if (uiState.nodes.isEmpty()) {
                    getHardcodedTestNodes()
                } else {
                    uiState.nodes
                }

                val treeNodes = remember(displayNodes) {
                    buildTreeNodes(displayNodes)
                }

                val textMeasurer = rememberTextMeasurer()

                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                scale = (scale * zoom).coerceIn(MIN_SCALE, MAX_SCALE)
                                offsetX += pan.x
                                offsetY += pan.y
                            }
                        }
                ) {
                    val nodePositions = calculateNodePositions(treeNodes, size.width, size.height)

                    withTransform({
                        translate(offsetX, offsetY)
                        scale(scale, scale, Offset.Zero)
                    }) {
                        drawConnections(treeNodes, nodePositions)
                        drawNodes(treeNodes, nodePositions, textMeasurer)
                    }
                }
            }
        }
    }
}

private fun getHardcodedTestNodes(): List<SkillNodeEntity> {
    val rootId = "root"
    val programmingId = "programming"
    val pythonId = "python"
    val webId = "web"
    val kotlinId = "kotlin"
    val dataAnalysisId = "data-analysis"

    return listOf(
        SkillNodeEntity(
            id = rootId,
            parentId = null,
            title = "技能树",
            nodeType = NODE_TYPE_ABILITY
        ),
        SkillNodeEntity(
            id = programmingId,
            parentId = rootId,
            title = "编程",
            nodeType = NODE_TYPE_ABILITY
        ),
        SkillNodeEntity(
            id = pythonId,
            parentId = programmingId,
            title = "Python",
            nodeType = NODE_TYPE_ABILITY
        ),
        SkillNodeEntity(
            id = webId,
            parentId = programmingId,
            title = "Web开发",
            nodeType = NODE_TYPE_ABILITY
        ),
        SkillNodeEntity(
            id = kotlinId,
            parentId = programmingId,
            title = "Kotlin",
            nodeType = NODE_TYPE_ABILITY
        ),
        SkillNodeEntity(
            id = dataAnalysisId,
            parentId = pythonId,
            title = "数据分析",
            nodeType = NODE_TYPE_ABILITY
        )
    )
}

private fun buildTreeNodes(nodes: List<SkillNodeEntity>): List<TreeNode> {
    val nodeMap = nodes.associate { it.id to TreeNode(it) }
    val rootNodes = mutableListOf<TreeNode>()

    nodes.forEach { entity ->
        val treeNode = nodeMap[entity.id]!!
        if (entity.parentId == null) {
            rootNodes.add(treeNode)
        } else {
            nodeMap[entity.parentId]?.children?.add(treeNode)
        }
    }

    return rootNodes
}

private fun calculateNodePositions(treeNodes: List<TreeNode>, canvasWidth: Float, canvasHeight: Float): List<NodePosition> {
    val positions = mutableListOf<NodePosition>()
    var currentX = 0f
    var currentY = 100f

    fun layoutTree(node: TreeNode, depth: Int, startX: Float, endX: Float): Float {
        val subtreeWidth = calculateSubtreeWidth(node)

        if (endX - startX <= subtreeWidth) {
            currentX = (startX + endX) / 2
        }

        val nodeX = currentX
        val nodeY = currentY + depth * (NODE_HEIGHT + VERTICAL_SPACING)

        node.x = nodeX
        node.y = nodeY
        positions.add(NodePosition(node.entity.id, nodeX, nodeY))

        currentX = startX

        if (node.children.isNotEmpty()) {
            val childSpacing = (endX - startX) / node.children.size
            node.children.forEachIndexed { index, child ->
                val childStartX = startX + index * childSpacing
                val childEndX = if (index == node.children.size - 1) {
                    endX
                } else {
                    childStartX + childSpacing
                }
                layoutTree(child, depth + 1, childStartX, childEndX)
            }
        }

        return nodeX
    }

    treeNodes.forEachIndexed { index, root ->
        val totalWidth = canvasWidth
        val rootStartX = index * (totalWidth / treeNodes.size)
        val rootEndX = if (index == treeNodes.size - 1) totalWidth else (index + 1) * (totalWidth / treeNodes.size)
        layoutTree(root, 0, rootStartX, rootEndX)
    }

    return positions
}

private fun calculateSubtreeWidth(node: TreeNode): Float {
    if (node.children.isEmpty()) {
        return NODE_WIDTH
    }
    val childrenWidth = node.children.sumOf { calculateSubtreeWidth(it).toDouble() }.toFloat()
    return maxOf(NODE_WIDTH, childrenWidth + (node.children.size - 1) * HORIZONTAL_SPACING)
}

private fun DrawScope.drawConnections(
    treeNodes: List<TreeNode>,
    positions: List<NodePosition>
) {
    val positionMap = positions.associateBy { it.nodeId }

    fun drawNodeConnections(node: TreeNode) {
        node.children.forEach { child ->
            val parentPos = positionMap[node.entity.id]
            val childPos = positionMap[child.entity.id]

            if (parentPos != null && childPos != null) {
                val startX = parentPos.x
                val startY = parentPos.y + NODE_HEIGHT / 2
                val endX = childPos.x
                val endY = childPos.y - NODE_HEIGHT / 2

                val controlPoint1X = startX
                val controlPoint1Y = (startY + endY) / 2
                val controlPoint2X = endX
                val controlPoint2Y = (startY + endY) / 2

                val path = Path().apply {
                    moveTo(startX, startY)
                    cubicTo(
                        controlPoint1X, controlPoint1Y,
                        controlPoint2X, controlPoint2Y,
                        endX, endY
                    )
                }

                drawPath(
                    path = path,
                    color = LinkOrange,
                    style = Stroke(width = 3f)
                )

                drawNodeConnections(child)
            }
        }
    }

    treeNodes.forEach { drawNodeConnections(it) }
}

private fun DrawScope.drawNodes(
    treeNodes: List<TreeNode>,
    positions: List<NodePosition>,
    textMeasurer: TextMeasurer
) {
    val positionMap = positions.associateBy { it.nodeId }

    fun drawTreeNodes(node: TreeNode) {
        val pos = positionMap[node.entity.id] ?: return

        val nodeColor = when (node.entity.nodeType) {
            NODE_TYPE_ABILITY -> AbilityGreen
            NODE_TYPE_RESOURCE -> ResourcePurple
            else -> AbilityGreen
        }

        drawRoundRect(
            color = SurfaceDark,
            topLeft = Offset(pos.x - NODE_WIDTH / 2, pos.y - NODE_HEIGHT / 2),
            size = Size(NODE_WIDTH, NODE_HEIGHT),
            cornerRadius = CornerRadius(CORNER_RADIUS)
        )

        drawRoundRect(
            color = nodeColor.copy(alpha = 0.3f),
            topLeft = Offset(pos.x - NODE_WIDTH / 2, pos.y - NODE_HEIGHT / 2),
            size = Size(NODE_WIDTH, NODE_HEIGHT),
            cornerRadius = CornerRadius(CORNER_RADIUS),
            style = Stroke(width = 2f)
        )

        val titleStyle = TextStyle(
            color = TextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )

        val titleResult = textMeasurer.measure(
            text = node.entity.title,
            style = titleStyle,
            maxLines = 1
        )

        val truncatedTitle = if (titleResult.size.width > NODE_WIDTH - 20) {
            var text = node.entity.title
            while (text.isNotEmpty() && textMeasurer.measure(
                    text = text + "...",
                    style = titleStyle
                ).size.width > NODE_WIDTH - 20
            ) {
                text = text.dropLast(1)
            }
            if (text.isNotEmpty()) text + "..." else node.entity.title.take(6) + "..."
        } else {
            node.entity.title
        }

        drawText(
            textMeasurer = textMeasurer,
            text = truncatedTitle,
            style = titleStyle,
            topLeft = Offset(
                pos.x - titleResult.size.width / 2,
                pos.y - titleResult.size.height / 2
            )
        )

        node.children.forEach { drawTreeNodes(it) }
    }

    treeNodes.forEach { drawTreeNodes(it) }
}
