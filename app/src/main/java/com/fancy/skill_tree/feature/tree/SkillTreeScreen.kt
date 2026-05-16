package com.fancy.skill_tree.feature.tree

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.focusable
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.fancy.skill_tree.R
import com.fancy.skill_tree.core.domain.entity.SkillNodeEntity
import com.fancy.skill_tree.core.domain.entity.TagEntity
import com.fancy.skill_tree.core.domain.model.Achievement
import com.fancy.skill_tree.core.ui.animation.AnimationConfig
import com.fancy.skill_tree.core.ui.animation.AnimationParams
import com.fancy.skill_tree.core.ui.render.TreeLayoutCache
import com.fancy.skill_tree.core.ui.render.TextMeasureCache
import com.fancy.skill_tree.core.ui.render.ConnectionPathCache
import com.fancy.skill_tree.core.ui.render.ViewportCuller
import com.fancy.skill_tree.core.ui.accessibility.AccessibilityOverlay
import com.fancy.skill_tree.core.ui.accessibility.accessibilityButton
import com.fancy.skill_tree.core.ui.accessibility.accessibilityLabel
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import com.fancy.skill_tree.ui.theme.LocalThemeColors
import com.fancy.skill_tree.ui.theme.ThemeColors
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val NODE_WIDTH = 140f
private const val NODE_HEIGHT = 70f
private const val HORIZONTAL_SPACING = 60f
private const val VERTICAL_SPACING = 100f
private const val CORNER_RADIUS = 16f
private const val MIN_SCALE = 0.3f
private const val MAX_SCALE = 3.0f
private const val DRAG_THRESHOLD = 20f

private val NODE_TYPE_ABILITY = "ABILITY"
private val NODE_TYPE_RESOURCE = "RESOURCE"

data class TreeNode(
    val entity: SkillNodeEntity,
    var x: Float = 0f,
    var y: Float = 0f,
    var depth: Int = 0,
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
    onNodeClick: (String) -> Unit = {},
    viewModel: SkillTreeViewModel = hiltViewModel()
) {
    val colors = LocalThemeColors.current
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val newAchievements by viewModel.newAchievements.collectAsState()
    val searchHistory by viewModel.searchHistory.collectAsState()
    val selectedTags by viewModel.selectedTags.collectAsState()
    val allTags by viewModel.allTags.collectAsState()

    var scale by remember { mutableFloatStateOf(1f) }
    val scaleAnimatable = remember { Animatable(1f) }
    var isBounceAnimating by remember { mutableStateOf(false) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var showSearchBar by remember { mutableStateOf(false) }
    var draggedNodeId by remember { mutableStateOf<String?>(null) }
    var dragStartOffset by remember { mutableStateOf<Offset?>(null) }
    var isDragging by remember { mutableStateOf(false) }
    var canvasSize by remember { mutableStateOf(Size.Zero) }

    // === 动画状态 ===
    var hasPlayedGrowthAnimation by remember { mutableStateOf(false) }
    var previousNodeIds by remember { mutableStateOf(emptySet<String>()) }
    var newlyCreatedNodeId by remember { mutableStateOf<String?>(null) }

    val creationScale = remember { Animatable(1f) }
    val growthProgress = remember { Animatable(100f) }

    val infiniteTransition = rememberInfiniteTransition(label = "tree_animations")
    val searchPulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(AnimationConfig.SEARCH_PULSE_DURATION, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "search_pulse"
    )
    val selectedPulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(AnimationConfig.SELECTED_PULSE_DURATION, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "selected_pulse"
    )

    if (showCreateDialog) {
        CreateNodeDialog(
            allNodes = uiState.nodes,
            onDismiss = { showCreateDialog = false },
            onCreateNode = { title, nodeType, parentId ->
                viewModel.createNode(title, nodeType, parentId)
                showCreateDialog = false
            }
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = colors.background,
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                FloatingActionButton(
                    onClick = { exportMarkdown(context, viewModel) },
                    containerColor = colors.surface,
                    contentColor = colors.primary
                ) {
                    Icon(
                        imageVector = Icons.Default.Create,
                        contentDescription = stringResource(R.string.cd_export)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                FloatingActionButton(
                    onClick = { showCreateDialog = true },
                    containerColor = colors.primary,
                    contentColor = colors.textPrimary
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = stringResource(R.string.cd_add_node)
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.background)
                .padding(innerPadding)
        ) {
            // 成就通知
            if (newAchievements.isNotEmpty()) {
                AchievementNotification(
                    achievements = newAchievements,
                    onDismiss = { viewModel.clearNewAchievements() }
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.background)
                .padding(innerPadding)
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
                        text = uiState.errorMessage ?: stringResource(R.string.common_unknown_error),
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                uiState.nodes.isEmpty() -> {
                    EmptyStateGuide(
                        onLoadSampleData = { viewModel.loadSampleData() },
                        onCreateFirstNode = { showCreateDialog = true }
                    )
                }
                else -> {
                    val displayNodes = if (uiState.searchQuery.isBlank() && selectedTags.isEmpty()) uiState.nodes else uiState.filteredNodes

                    val layoutCache = remember { TreeLayoutCache() }
                    val connectionPathCache = remember { ConnectionPathCache() }

                    LaunchedEffect(displayNodes) {
                        layoutCache.invalidate()
                        connectionPathCache.clear()
                    }

                    val treeNodes = remember(displayNodes) {
                        layoutCache.getOrComputeTreeNodes(displayNodes) { nodes ->
                            buildTreeNodes(nodes)
                        }
                    }

                    val maxDepth = remember(treeNodes) {
                        var max = 0
                        fun findMaxDepth(node: TreeNode, depth: Int) {
                            max = maxOf(max, depth)
                            node.children.forEach { findMaxDepth(it, depth + 1) }
                        }
                        treeNodes.forEach { findMaxDepth(it, 0) }
                        max
                    }

                    val textMeasurer = rememberTextMeasurer()
                    val textMeasureCache = remember { TextMeasureCache() }

                    // 树生长动画：首次加载时逐层展开
                    LaunchedEffect(treeNodes.isNotEmpty()) {
                        if (treeNodes.isNotEmpty() && !hasPlayedGrowthAnimation) {
                            hasPlayedGrowthAnimation = true
                            growthProgress.snapTo(0f)
                            growthProgress.animateTo(
                                (maxDepth + 1).toFloat(),
                                animationSpec = tween(
                                    durationMillis = AnimationConfig.NODE_GROW_DURATION + maxDepth * AnimationConfig.NODE_GROW_LAYER_DELAY,
                                    easing = FastOutSlowInEasing
                                )
                            )
                        }
                    }

                    // 节点创建动画：检测新增节点
                    LaunchedEffect(uiState.nodes) {
                        textMeasureCache.clear()
                        val currentNodeIds = uiState.nodes.map { it.id }.toSet()
                        val newIds = currentNodeIds - previousNodeIds
                        if (hasPlayedGrowthAnimation && previousNodeIds.isNotEmpty() && newIds.isNotEmpty()) {
                            newlyCreatedNodeId = newIds.first()
                            creationScale.snapTo(0f)
                            creationScale.animateTo(
                                1f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessMedium
                                )
                            )
                            delay(500)
                            newlyCreatedNodeId = null
                        }
                        previousNodeIds = currentNodeIds
                    }

                    val currentAnimationParams = AnimationParams(
                        newlyCreatedNodeId = newlyCreatedNodeId,
                        creationScale = creationScale.value,
                        growthProgress = growthProgress.value,
                        searchPulseAlpha = if (uiState.searchQuery.isNotBlank()) searchPulseAlpha else 0f,
                        selectedPulseAlpha = if (uiState.selectedNodeId != null) selectedPulseAlpha else 0f,
                        selectedNodeId = uiState.selectedNodeId,
                        isSearchActive = uiState.searchQuery.isNotBlank()
                    )

                    Column(modifier = Modifier.fillMaxSize()) {
                        if (showSearchBar) {
                            SearchBar(
                                query = uiState.searchQuery,
                                searchHistory = searchHistory,
                                allTags = allTags,
                                selectedTags = selectedTags,
                                onQueryChange = { viewModel.search(it) },
                                onHistoryClick = { viewModel.search(it) },
                                onTagClick = { viewModel.toggleTag(it) },
                                onClearHistory = { viewModel.clearSearchHistory() },
                                onClose = {
                                    showSearchBar = false
                                    viewModel.search("")
                                }
                            )
                        }

                        val treeFocusRequester = remember { FocusRequester() }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(treeFocusRequester)
                                .focusable()
                                .onKeyEvent { keyEvent ->
                                    if (keyEvent.type == KeyEventType.KeyDown) {
                                        when (keyEvent.key) {
                                            Key.DirectionUp -> {
                                                viewModel.navigateToParent()
                                                true
                                            }
                                            Key.DirectionDown -> {
                                                viewModel.navigateToFirstChild()
                                                true
                                            }
                                            Key.DirectionLeft -> {
                                                viewModel.navigateToPreviousSibling()
                                                true
                                            }
                                            Key.DirectionRight -> {
                                                viewModel.navigateToNextSibling()
                                                true
                                            }
                                            Key.Enter, Key.Spacebar -> {
                                                viewModel.selectedNodeId.value?.let { onNodeClick(it) }
                                                true
                                            }
                                            else -> false
                                        }
                                    } else false
                                }
                        ) {
                            val nodePositions = remember(treeNodes, canvasSize) {
                                if (canvasSize.width > 0f && canvasSize.height > 0f) {
                                    layoutCache.getOrComputePositions(
                                        treeNodes, canvasSize.width, canvasSize.height
                                    ) { t, w, h ->
                                        calculateNodePositions(t, w, h)
                                    }
                                } else {
                                    emptyList()
                                }
                            }

                            val visibleNodeIds = remember(nodePositions, canvasSize, scale, offsetX, offsetY) {
                                if (canvasSize.width > 0f && canvasSize.height > 0f) {
                                    val culler = ViewportCuller(canvasSize.width, canvasSize.height, scale, offsetX, offsetY)
                                    culler.filterVisibleNodes(nodePositions, NODE_WIDTH, NODE_HEIGHT).map { it.nodeId }.toSet()
                                } else {
                                    emptySet()
                                }
                            }

                            Canvas(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .onSizeChanged { canvasSize = Size(it.width.toFloat(), it.height.toFloat()) }
                                    .pointerInput(draggedNodeId) {
                                        var needsBounce = false
                                        var bounceTarget = 0f

                                        awaitEachGesture {
                                            var previousDistance = 0f
                                            var isTransforming = false

                                            awaitFirstDown(requireUnconsumed = false)

                                            do {
                                                val event = awaitPointerEvent()

                                                if (event.changes.size >= 2 && !isBounceAnimating) {
                                                    isTransforming = true
                                                    val pointer0 = event.changes[0]
                                                    val pointer1 = event.changes[1]

                                                    val currentDistance = (pointer0.position - pointer1.position).getDistance()

                                                    if (previousDistance > 0f) {
                                                        val zoom = currentDistance / previousDistance
                                                        scale *= zoom
                                                    }
                                                    previousDistance = currentDistance

                                                    if (pointer0.previousPosition != Offset.Unspecified && pointer1.previousPosition != Offset.Unspecified) {
                                                        val currentCentroid = (pointer0.position + pointer1.position) / 2f
                                                        val previousCentroid = (pointer0.previousPosition + pointer1.previousPosition) / 2f
                                                        val pan = currentCentroid - previousCentroid
                                                        offsetX += pan.x
                                                        offsetY += pan.y
                                                    }
                                                } else if (event.changes.size == 1 && !isBounceAnimating) {
                                                    val change = event.changes[0]
                                                    if (change.previousPosition != Offset.Unspecified) {
                                                        val pan = change.position - change.previousPosition
                                                        offsetX += pan.x
                                                        offsetY += pan.y
                                                    }
                                                }

                                                event.changes.forEach { it.consume() }
                                            } while (event.changes.any { it.pressed })

                                            if (isTransforming) {
                                                val target = scale.coerceIn(MIN_SCALE, MAX_SCALE)
                                                if (target != scale) {
                                                    needsBounce = true
                                                    bounceTarget = target
                                                }
                                            }
                                        }

                                        if (needsBounce) {
                                            needsBounce = false
                                            isBounceAnimating = true
                                            scaleAnimatable.snapTo(scale)
                                            scaleAnimatable.animateTo(
                                                targetValue = bounceTarget,
                                                animationSpec = AnimationConfig.ZOOM_BOUNCE_SPRING
                                            ) {
                                                scale = value
                                            }
                                            isBounceAnimating = false
                                        }
                                    }
                                    .pointerInput(treeNodes, draggedNodeId) {
                                        detectTapGestures(
                                            onLongPress = { offset ->
                                                val transformedX = (offset.x - offsetX) / scale
                                                val transformedY = (offset.y - offsetY) / scale

                                                val nodePositions = layoutCache.getOrComputePositions(
                                                    treeNodes,
                                                    size.width.toFloat(),
                                                    size.height.toFloat()
                                                ) { t, w, h ->
                                                    calculateNodePositions(t, w, h)
                                                }

                                                nodePositions.forEach { pos ->
                                                    if (transformedX >= pos.x - NODE_WIDTH / 2 &&
                                                        transformedX <= pos.x + NODE_WIDTH / 2 &&
                                                        transformedY >= pos.y - NODE_HEIGHT / 2 &&
                                                        transformedY <= pos.y + NODE_HEIGHT / 2
                                                    ) {
                                                        draggedNodeId = pos.nodeId
                                                        dragStartOffset = offset
                                                        isDragging = true
                                                        return@detectTapGestures
                                                    }
                                                }
                                            },
                                            onTap = { offset ->
                                                if (draggedNodeId != null) {
                                                    val transformedX = (offset.x - offsetX) / scale
                                                    val transformedY = (offset.y - offsetY) / scale

                                                    val nodePositions = layoutCache.getOrComputePositions(
                                                        treeNodes,
                                                        size.width.toFloat(),
                                                        size.height.toFloat()
                                                    ) { t, w, h ->
                                                        calculateNodePositions(t, w, h)
                                                    }

                                                    var targetParentId: String? = null
                                                    nodePositions.forEach { pos ->
                                                        if (transformedX >= pos.x - NODE_WIDTH / 2 &&
                                                            transformedX <= pos.x + NODE_WIDTH / 2 &&
                                                            transformedY >= pos.y - NODE_HEIGHT / 2 &&
                                                            transformedY <= pos.y + NODE_HEIGHT / 2 &&
                                                            pos.nodeId != draggedNodeId
                                                        ) {
                                                            targetParentId = pos.nodeId
                                                        }
                                                    }

                                                    if (targetParentId != null && draggedNodeId != targetParentId) {
                                                        val draggedId = draggedNodeId
                                                        if (draggedId != null) {
                                                            viewModel.moveNode(draggedId, targetParentId)
                                                        }
                                                    }
                                                    draggedNodeId = null
                                                    dragStartOffset = null
                                                    isDragging = false
                                                    return@detectTapGestures
                                                }

                                                val transformedX = (offset.x - offsetX) / scale
                                                val transformedY = (offset.y - offsetY) / scale

                                                val nodePositions = layoutCache.getOrComputePositions(
                                                    treeNodes,
                                                    size.width.toFloat(),
                                                    size.height.toFloat()
                                                ) { t, w, h ->
                                                    calculateNodePositions(t, w, h)
                                                }

                                                nodePositions.forEach { pos ->
                                                    if (transformedX >= pos.x - NODE_WIDTH / 2 &&
                                                        transformedX <= pos.x + NODE_WIDTH / 2 &&
                                                        transformedY >= pos.y - NODE_HEIGHT / 2 &&
                                                        transformedY <= pos.y + NODE_HEIGHT / 2
                                                    ) {
                                                        viewModel.onSelectNode(pos.nodeId)
                                                        onNodeClick(pos.nodeId)
                                                        return@detectTapGestures
                                                    }
                                                }
                                            }
                                        )
                                    }
                            ) {
                                withTransform({
                                    translate(offsetX, offsetY)
                                    scale(scale, scale, Offset.Zero)
                                }) {
                                    drawConnections(treeNodes, nodePositions, currentAnimationParams, visibleNodeIds, connectionPathCache, colors)
                                    drawNodes(
                                        treeNodes,
                                        nodePositions,
                                        textMeasurer,
                                        textMeasureCache,
                                        draggedNodeId,
                                        uiState.searchQuery,
                                        currentAnimationParams,
                                        visibleNodeIds,
                                        colors
                                    )
                                }
                            }

                            AccessibilityOverlay(
                                treeNodes = treeNodes,
                                nodePositions = nodePositions,
                                visibleNodeIds = visibleNodeIds,
                                scale = scale,
                                offsetX = offsetX,
                                offsetY = offsetY,
                                nodeWidth = NODE_WIDTH,
                                nodeHeight = NODE_HEIGHT,
                                onNodeClick = { nodeId ->
                                    viewModel.onSelectNode(nodeId)
                                    onNodeClick(nodeId)
                                },
                                onNodeLongClick = { nodeId ->
                                    viewModel.onSelectNode(nodeId)
                                }
                            )

                            if (isDragging && draggedNodeId != null) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .padding(bottom = 100.dp)
                                        .background(colors.surface.copy(alpha = 0.9f), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 16.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        text = stringResource(R.string.tree_drag_hint),
                                        color = colors.textSecondary,
                                        fontSize = 13.sp
                                    )
                                }
                            }

                            if (uiState.searchQuery.isNotBlank() && uiState.filteredNodes.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.Center)
                                        .background(colors.surface, RoundedCornerShape(12.dp))
                                        .padding(24.dp)
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = "🔍",
                                            fontSize = 40.sp
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text(
                                            text = stringResource(R.string.tree_no_results),
                                            color = colors.textSecondary,
                                            fontSize = 14.sp
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = stringResource(R.string.tree_no_results_hint),
                                            color = colors.textSecondary.copy(alpha = 0.7f),
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(8.dp)
                            ) {
                                FloatingActionButton(
                                    onClick = { showSearchBar = !showSearchBar },
                                    containerColor = colors.surface,
                                    contentColor = if (showSearchBar) colors.primary else colors.textSecondary,
                                    modifier = Modifier.padding(0.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = stringResource(R.string.cd_search)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 将十六进制颜色字符串解析为 Compose Color
 * 支持格式: "#RRGGBB", "#AARRGGBB", "RRGGBB", "AARRGGBB"
 * @param hex 十六进制颜色字符串
 * @return 解析后的 Color，解析失败时返回 TextSecondary
 */
private fun parseColor(hex: String, colors: ThemeColors): Color {
    return try {
        val cleanHex = hex.removePrefix("#")
        val colorValue = when (cleanHex.length) {
            6 -> "FF$cleanHex".toLong(16)
            8 -> cleanHex.toLong(16)
            else -> return colors.textSecondary
        }
        Color(colorValue)
    } catch (_: IllegalArgumentException) {
        colors.textSecondary
    }
}

@Composable
private fun SearchBar(
    query: String,
    searchHistory: List<String>,
    allTags: List<TagEntity>,
    selectedTags: List<String>,
    onQueryChange: (String) -> Unit,
    onHistoryClick: (String) -> Unit,
    onTagClick: (String) -> Unit,
    onClearHistory: () -> Unit,
    onClose: () -> Unit
) {
    val colors = LocalThemeColors.current
    val focusRequester = remember { androidx.compose.ui.focus.FocusRequester() }
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surface.copy(alpha = 0.95f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = stringResource(R.string.cd_close_search),
                    tint = colors.textPrimary
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                placeholder = { Text(stringResource(R.string.tree_search_hint), color = colors.textSecondary) },
                leadingIcon = {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = stringResource(R.string.cd_search_icon),
                        tint = colors.primary
                    )
                },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { onQueryChange("") }) {
                            Icon(
                                Icons.Default.Clear,
                                contentDescription = stringResource(R.string.cd_clear_search),
                                tint = colors.textSecondary
                            )
                        }
                    }
                },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = colors.textPrimary,
                    unfocusedTextColor = colors.textPrimary,
                    focusedBorderColor = colors.primary,
                    unfocusedBorderColor = colors.textSecondary,
                    cursorColor = colors.primary,
                    focusedContainerColor = colors.background,
                    unfocusedContainerColor = colors.background
                ),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            TextButton(onClick = onClose) {
                Text(stringResource(R.string.common_cancel), color = colors.primary)
            }
        }

        // 自动聚焦
        androidx.compose.runtime.LaunchedEffect(Unit) {
            focusRequester.requestFocus()
        }

        // 搜索历史区域（搜索框为空时显示）
        if (query.isEmpty() && searchHistory.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.tree_search_history),
                        color = colors.textSecondary,
                        fontSize = 13.sp,
                        modifier = Modifier.accessibilityLabel(stringResource(R.string.a11y_search_history))
                    )
                    TextButton(onClick = onClearHistory) {
                        Text(
                            text = stringResource(R.string.tree_clear_history),
                            color = colors.primary,
                            fontSize = 13.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(scrollState),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    searchHistory.forEach { historyTerm ->
                        AssistChip(
                            onClick = { onHistoryClick(historyTerm) },
                            label = { Text(historyTerm, color = colors.textPrimary, fontSize = 13.sp) },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = colors.background
                            )
                        )
                    }
                }
            }
        }

        // 标签筛选区域
        if (allTags.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = stringResource(R.string.tree_tag_filter),
                    color = colors.textSecondary,
                    fontSize = 13.sp,
                    modifier = Modifier.accessibilityLabel(stringResource(R.string.a11y_tag_filter))
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    allTags.forEach { tag ->
                        val isSelected = selectedTags.contains(tag.id)
                        val tagColor = tag.color?.let { parseColor(it, colors) } ?: colors.textSecondary

                        FilterChip(
                            selected = isSelected,
                            onClick = { onTagClick(tag.id) },
                            label = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Canvas(modifier = Modifier.width(8.dp).height(8.dp)) {
                                        drawCircle(color = tagColor)
                                    }
                                    Text(
                                        text = tag.name,
                                        color = if (isSelected) colors.textPrimary else colors.textSecondary,
                                        fontSize = 13.sp
                                    )
                                }
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = colors.primary.copy(alpha = 0.2f),
                                containerColor = colors.background
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                borderColor = if (isSelected) colors.primary else colors.textSecondary.copy(alpha = 0.3f),
                                selectedBorderColor = colors.primary,
                                enabled = true,
                                selected = isSelected
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyStateGuide(
    onLoadSampleData: () -> Unit,
    onCreateFirstNode: () -> Unit
) {
    val colors = LocalThemeColors.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "🌳",
            fontSize = 64.sp
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.tree_empty_title),
            color = colors.textPrimary,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.tree_empty_description),
            color = colors.textSecondary,
            fontSize = 16.sp
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onLoadSampleData,
            colors = ButtonDefaults.buttonColors(
                containerColor = colors.primary,
                contentColor = colors.textPrimary
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(stringResource(R.string.tree_load_sample), fontSize = 16.sp)
        }
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedButton(
            onClick = onCreateFirstNode,
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(stringResource(R.string.tree_create_first), color = colors.primary)
        }
    }
}

@Composable
private fun OutlinedButton(
    onClick: () -> Unit,
    shape: RoundedCornerShape,
    content: @Composable () -> Unit
) {
    val colors = LocalThemeColors.current
    Button(
        onClick = onClick,
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = colors.primary
        ),
        shape = shape
    ) {
        content()
    }
}

private fun exportMarkdown(context: android.content.Context, viewModel: SkillTreeViewModel) {
    viewModel.exportToMarkdown { markdown ->
        try {
            val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
            val fileName = "skill_tree_${dateFormat.format(Date())}.md"
            val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(
                android.os.Environment.DIRECTORY_DOWNLOADS
            )
            val skillTreeDir = File(downloadsDir, "skill_tree")
            if (!skillTreeDir.exists()) {
                skillTreeDir.mkdirs()
            }
            val file = File(skillTreeDir, fileName)
            file.writeText(markdown)
            viewModel.showExportSuccess(file.absolutePath)
        } catch (e: Exception) {
            viewModel.showExportError(e.message ?: context.getString(R.string.common_unknown_error))
        }
    }
}

private fun buildTreeNodes(nodes: List<SkillNodeEntity>): List<TreeNode> {
    val nodeMap = nodes.associate { it.id to TreeNode(it) }
    val rootNodes = mutableListOf<TreeNode>()

    nodes.forEach { entity ->
        val treeNode = nodeMap[entity.id] ?: return@forEach
        if (entity.parentId == null) {
            rootNodes.add(treeNode)
        } else {
            nodeMap[entity.parentId]?.children?.add(treeNode)
        }
    }

    fun assignDepth(node: TreeNode, depth: Int) {
        node.depth = depth
        node.children.forEach { assignDepth(it, depth + 1) }
    }
    rootNodes.forEach { assignDepth(it, 0) }

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
    positions: List<NodePosition>,
    animationParams: AnimationParams = AnimationParams(),
    visibleNodeIds: Set<String> = emptySet(),
    connectionPathCache: ConnectionPathCache,
    themeColors: ThemeColors
) {
    val positionMap = positions.associateBy { it.nodeId }
    val pathMeasure = PathMeasure()

    fun drawNodeConnections(node: TreeNode, depth: Int = 0) {
        node.children.forEach { child ->
            val parentPos = positionMap[node.entity.id]
            val childPos = positionMap[child.entity.id]

            if (parentPos != null && childPos != null) {
                val isConnectionVisible = visibleNodeIds.isEmpty() ||
                    (node.entity.id in visibleNodeIds && child.entity.id in visibleNodeIds)

                if (isConnectionVisible) {
                    val connectionProgress = (animationParams.growthProgress - depth - 1).coerceIn(0f, 1f)

                    if (connectionProgress > 0f) {
                        val startX = parentPos.x
                        val startY = parentPos.y + NODE_HEIGHT / 2
                        val endX = childPos.x
                        val endY = childPos.y - NODE_HEIGHT / 2

                        val pathKey = "${startX}_${startY}_${endX}_${endY}"
                        val path = connectionPathCache.getOrCreatePath(pathKey, startX, startY, endX, endY)

                        if (connectionProgress >= 1f) {
                            drawPath(
                                path = path,
                                color = themeColors.link,
                                style = Stroke(width = 3f)
                            )
                        } else {
                            pathMeasure.setPath(path, false)
                            val partialPath = Path()
                            pathMeasure.getSegment(
                                0f,
                                pathMeasure.length * connectionProgress,
                                partialPath,
                                true
                            )
                            drawPath(
                                path = partialPath,
                                color = themeColors.link,
                                style = Stroke(width = 3f)
                            )
                        }
                    }
                }

                drawNodeConnections(child, depth + 1)
            }
        }
    }

    treeNodes.forEach { drawNodeConnections(it, 0) }
}

private fun DrawScope.drawNodes(
    treeNodes: List<TreeNode>,
    positions: List<NodePosition>,
    textMeasurer: TextMeasurer,
    textMeasureCache: TextMeasureCache,
    draggedNodeId: String?,
    searchQuery: String = "",
    animationParams: AnimationParams = AnimationParams(),
    visibleNodeIds: Set<String> = emptySet(),
    themeColors: ThemeColors
) {
    val positionMap = positions.associateBy { it.nodeId }

    fun drawTreeNodes(node: TreeNode, depth: Int = 0) {
        val pos = positionMap[node.entity.id] ?: return

        val isNodeVisible = visibleNodeIds.isEmpty() || node.entity.id in visibleNodeIds
        if (!isNodeVisible) {
            node.children.forEach { drawTreeNodes(it, depth + 1) }
            return
        }

        val nodeColor = when (node.entity.nodeType) {
            NODE_TYPE_ABILITY -> themeColors.ability
            NODE_TYPE_RESOURCE -> themeColors.resource
            else -> themeColors.ability
        }

        val isDragged = node.entity.id == draggedNodeId
        val isCreating = node.entity.id == animationParams.newlyCreatedNodeId
        val isSelected = node.entity.id == animationParams.selectedNodeId
        val isSearchMatch = animationParams.isSearchActive &&
            searchQuery.isNotEmpty() &&
            node.entity.title.contains(searchQuery, ignoreCase = true)

        // 计算生长动画的局部进度
        val growthLocalProgress = (animationParams.growthProgress - depth).coerceIn(0f, 1f)

        // 计算创建动画的缩放
        val creationAnimScale = if (isCreating) animationParams.creationScale else 1f
        val creationAlpha = if (isCreating) animationParams.creationScale.coerceIn(0f, 1f) else 1f

        // 计算生长动画的位置插值（子节点从父节点位置滑出）
        val parentPos = node.entity.parentId?.let { positionMap[it] }
        val drawX = if (parentPos != null && growthLocalProgress < 1f) {
            parentPos.x + (pos.x - parentPos.x) * growthLocalProgress
        } else {
            pos.x
        }
        val drawY = if (parentPos != null && growthLocalProgress < 1f) {
            parentPos.y + (pos.y - parentPos.y) * growthLocalProgress
        } else {
            pos.y
        }

        // 综合缩放和透明度
        val growthScale = 0.5f + 0.5f * growthLocalProgress
        val growthAlpha = growthLocalProgress
        val combinedScale = growthScale * creationAnimScale
        val combinedAlpha = growthAlpha * creationAlpha

        if (combinedAlpha <= 0f) {
            node.children.forEach { drawTreeNodes(it, depth + 1) }
            return
        }

        val scaledWidth = NODE_WIDTH * combinedScale
        val scaledHeight = NODE_HEIGHT * combinedScale

        // 选中节点脉冲光晕
        if (isSelected && combinedAlpha > 0.5f) {
            drawRoundRect(
                color = themeColors.primary.copy(alpha = animationParams.selectedPulseAlpha * combinedAlpha),
                topLeft = Offset(drawX - scaledWidth / 2 - 4, drawY - scaledHeight / 2 - 4),
                size = Size(scaledWidth + 8, scaledHeight + 8),
                cornerRadius = CornerRadius(CORNER_RADIUS * combinedScale + 4),
                style = Stroke(width = 3f)
            )
        }

        // 搜索高亮呼吸边框
        if (isSearchMatch && combinedAlpha > 0.5f) {
            drawRoundRect(
                color = themeColors.primary.copy(alpha = animationParams.searchPulseAlpha * combinedAlpha),
                topLeft = Offset(drawX - scaledWidth / 2 - 2, drawY - scaledHeight / 2 - 2),
                size = Size(scaledWidth + 4, scaledHeight + 4),
                cornerRadius = CornerRadius(CORNER_RADIUS * combinedScale + 2),
                style = Stroke(width = 2f)
            )
        }

        drawRoundRect(
            color = (if (isDragged) themeColors.primary.copy(alpha = 0.3f) else themeColors.surface).copy(alpha = combinedAlpha),
            topLeft = Offset(drawX - scaledWidth / 2, drawY - scaledHeight / 2),
            size = Size(scaledWidth, scaledHeight),
            cornerRadius = CornerRadius(CORNER_RADIUS * combinedScale)
        )

        drawRoundRect(
            color = (if (isDragged) themeColors.primary else nodeColor.copy(alpha = 0.3f)).copy(alpha = combinedAlpha),
            topLeft = Offset(drawX - scaledWidth / 2, drawY - scaledHeight / 2),
            size = Size(scaledWidth, scaledHeight),
            cornerRadius = CornerRadius(CORNER_RADIUS * combinedScale),
            style = Stroke(width = if (isDragged) 3f else 2f)
        )

        // 高亮文本处理
        val annotatedString = if (searchQuery.isNotEmpty()) {
            buildAnnotatedString {
                val lowerTitle = node.entity.title.lowercase()
                val lowerQuery = searchQuery.lowercase()
                var currentIndex = 0

                while (currentIndex < node.entity.title.length) {
                    val matchIndex = lowerTitle.indexOf(lowerQuery, currentIndex)
                    if (matchIndex == -1) {
                        withStyle(SpanStyle(color = themeColors.textPrimary)) {
                            append(node.entity.title.substring(currentIndex))
                        }
                        break
                    }

                    if (matchIndex > currentIndex) {
                        withStyle(SpanStyle(color = themeColors.textPrimary)) {
                            append(node.entity.title.substring(currentIndex, matchIndex))
                        }
                    }

                    withStyle(SpanStyle(color = themeColors.primary, fontWeight = FontWeight.Bold, background = themeColors.primary.copy(alpha = 0.2f))) {
                        append(node.entity.title.substring(matchIndex, matchIndex + searchQuery.length))
                    }

                    currentIndex = matchIndex + searchQuery.length
                }
            }
        } else {
            buildAnnotatedString {
                withStyle(SpanStyle(color = themeColors.textPrimary)) {
                    append(node.entity.title)
                }
            }
        }

        val titleStyle = TextStyle(
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )

        val titleResult = textMeasurer.measure(
            text = annotatedString,
            style = titleStyle,
            maxLines = 1
        )

        // 截断文本（保持完整性）
        val truncatedAnnotatedString = if (titleResult.size.width > NODE_WIDTH - 20) {
            var text = node.entity.title
            while (text.isNotEmpty() && textMeasurer.measure(
                    text = text + "...",
                    style = titleStyle
                ).size.width > NODE_WIDTH - 20
            ) {
                text = text.dropLast(1)
            }
            val finalText = if (text.isNotEmpty()) text + "..." else node.entity.title.take(6) + "..."
            buildAnnotatedString {
                withStyle(SpanStyle(color = themeColors.textPrimary)) {
                    append(finalText)
                }
            }
        } else {
            annotatedString
        }

        // 重新测量截断后的文本
        val finalCacheKey = "${node.entity.id}_${node.entity.title}_${node.entity.level}_final"
        val finalTitleResult = textMeasureCache.getOrMeasure(finalCacheKey, truncatedAnnotatedString.text, titleStyle, textMeasurer)

        drawText(
            textLayoutResult = finalTitleResult,
            topLeft = Offset(
                drawX - finalTitleResult.size.width / 2,
                drawY - finalTitleResult.size.height / 2
            ),
            alpha = combinedAlpha
        )

        node.children.forEach { drawTreeNodes(it, depth + 1) }
    }

    treeNodes.forEach { drawTreeNodes(it) }
}

@Composable
private fun AchievementNotification(
    achievements: List<Achievement>,
    onDismiss: () -> Unit
) {
    val colors = LocalThemeColors.current
    LaunchedEffect(Unit) {
        delay(AnimationConfig.ACHIEVEMENT_DISPLAY_DURATION)
        onDismiss()
    }

    AnimatedVisibility(
        visible = true,
        enter = slideInVertically(
            initialOffsetY = { it / 2 },
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            )
        ) + fadeIn(
            animationSpec = tween(AnimationConfig.ACHIEVEMENT_ENTER_DURATION)
        ) + scaleIn(
            initialScale = 0.5f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            )
        ),
        exit = slideOutVertically(
            targetOffsetY = { it / 2 },
            animationSpec = tween(AnimationConfig.ACHIEVEMENT_ENTER_DURATION)
        ) + fadeOut(
            animationSpec = tween(AnimationConfig.ACHIEVEMENT_ENTER_DURATION)
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.surface, shape = RoundedCornerShape(16.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                achievements.forEach { achievement ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = achievement.emoji,
                            fontSize = 32.sp
                        )
                        Column {
                            Text(
                                text = stringResource(R.string.achievement_unlocked),
                                color = Color(0xFFFFD700),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = stringResource(achievement.titleResId),
                                color = colors.textPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = stringResource(achievement.descriptionResId),
                                color = colors.textSecondary,
                                fontSize = 13.sp
                            )
                            Text(
                                text = stringResource(R.string.achievement_reward, stringResource(achievement.rewardResId)),
                                color = colors.primary,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
