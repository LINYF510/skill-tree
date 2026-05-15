# 动画系统实现方案

> **关联 PRD**: 第 4.4 节（动画清单）
> **当前状态**: 零动画实现
> **目标**: 实现 PRD 中列出的所有动画效果

---

## 1. 动画清单（来自 PRD）

| 动画名称 | 触发时机 | 持续时间 | 缓动函数 |
|---------|---------|---------|---------|
| 节点创建 | 新建节点 | 400ms | overshoot |
| 节点生长 | 技能树首次加载 | 800ms | easeOut |
| 节点解锁 | 节点从不可用变为可用 | 300ms | easeInOut |
| 节点高亮脉冲 | 搜索匹配结果 | 持续 2s × 2 次 | easeInOut |
| 连线生长 | 创建新链接 | 500ms | easeOut |
| 粒子特效 | 节点升级/成就解锁 | 1000ms | easeOut |
| 画布缩放回弹 | 双指缩放结束 | 200ms | spring |
| 成就通知入场 | 成就解锁 | 400ms | overshoot |

---

## 2. 动画基础设施

### 2.1 动画工具类

```kotlin
package com.fancy.skill_tree.core.ui.animation

import androidx.compose.animation.core.*
import androidx.compose.ui.geometry.Offset
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * 动画配置常量
 */
object AnimationConfig {
    val NODE_CREATE = tween<Float>(durationMillis = 400, easing = FastOutSlowInEasing)
    val NODE_GROW = tween<Float>(durationMillis = 800, easing = FastOutSlowInEasing)
    val NODE_UNLOCK = tween<Float>(durationMillis = 300, easing = FastOutSlowInEasing)
    val LINK_GROW = tween<Float>(durationMillis = 500, easing = LinearEasing)
    val PARTICLE = tween<Float>(durationMillis = 1000, easing = LinearEasing)
    val SPRING_BOUNCE = spring<Float>(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)

    // 重复高亮脉冲
    val HIGHLIGHT_PULSE = repeatable<Float>(
        iterations = 2,
        animation = tween(1000, easing = FastOutSlowInEasing),
        repeatMode = RepeatMode.Reverse
    )
}
```

### 2.2 动画状态管理器

```kotlin
/**
 * 节点动画状态
 */
data class NodeAnimationState(
    val id: String,
    val isCreating: Boolean = false,
    val isHighlighted: Boolean = false,
    val creationProgress: Float = 0f,   // 0..1
    val highlightProgress: Float = 0f    // 0..1
)

/**
 * 全局动画管理器
 */
class AnimationManager {
    private val nodeAnimations = mutableMapOf<String, NodeAnimationState>()

    fun startCreateAnimation(nodeId: String) {
        nodeAnimations[nodeId] = NodeAnimationState(nodeId, isCreating = true)
    }

    fun startHighlightAnimation(nodeId: String) {
        nodeAnimations[nodeId] = nodeAnimations[nodeId]?.copy(isHighlighted = true) ?: NodeAnimationState(nodeId, isHighlighted = true)
    }

    fun removeAnimation(nodeId: String) {
        nodeAnimations.remove(nodeId)
    }

    fun getNodeAnimation(nodeId: String): NodeAnimationState? = nodeAnimations[nodeId]

    fun clearAll() = nodeAnimations.clear()
}
```

---

## 3. 各动画具体实现

### 3.1 节点创建动画（缩放弹出）

```kotlin
@Composable
fun AnimatedNodeCreation(
    nodeId: String,
    content: @Composable (Float) -> Unit
) {
    val scale = remember { Animatable(0f) }

    LaunchedEffect(nodeId) {
        scale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            )
        )
    }

    Box(
        modifier = Modifier.graphicsLayer {
            scaleX = scale.value
            scaleY = scale.value
            alpha = scale.value.coerceIn(0f, 1f)
        }
    ) {
        content(scale.value)
    }
}
```

### 3.2 树生长动画（从根到叶逐层展开）

```kotlin
@Composable
fun AnimatedTreeGrowth(
    treeNodes: List<TreeNode>,
    positionMap: Map<String, NodePosition>,
    onAnimationComplete: () -> Unit = {}
) {
    val maxDepth = treeNodes.maxOfOrNull { it.depth } ?: 0

    // 每层延迟 200ms
    val layerDelay = 200L

    LaunchedEffect(treeNodes) {
        delay(maxDepth * layerDelay + 800)
        onAnimationComplete()
    }

    // 根据深度分组渲染
    for (depth in 0..maxDepth) {
        val nodesAtDepth = treeNodes.filter { it.depth == depth }
        val layerProgress by animateFloatAsState(
            targetValue = 1f,
            animationSpec = tween(400, delayMillis = (depth * layerDelay).toInt())
        )

        nodesAtDepth.forEach { node ->
            val pos = positionMap[node.entity.id] ?: return@forEach

            Box(
                modifier = Modifier
                    .offset { IntOffset(pos.x.toInt(), pos.y.toInt()) }
                    .graphicsLayer {
                        alpha = layerProgress
                        scaleX = 0.5f + 0.5f * layerProgress
                        scaleY = 0.5f + 0.5f * layerProgress
                    }
            ) {
                // 节点内容
            }
        }
    }
}
```

### 3.3 搜索高亮脉冲

```kotlin
@Composable
fun SearchHighlightPulse(
    isHighlighted: Boolean,
    content: @Composable () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    Box(
        modifier = Modifier.graphicsLayer {
            scaleX = if (isHighlighted) pulseScale else 1f
            scaleY = if (isHighlighted) pulseScale else 1f
        }
    ) {
        content()

        if (isHighlighted) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    color = PrimaryBlue.copy(alpha = pulseAlpha),
                    radius = size.minDimension / 2 + 4f,
                    center = center
                )
            }
        }
    }
}
```

### 3.4 连线生长动画

```kotlin
/**
 * 在 Canvas 中绘制生长中的连线
 */
private fun DrawScope.drawGrowingConnection(
    startX: Float, startY: Float,
    endX: Float, endY: Float,
    progress: Float, // 0..1
    color: Color,
    strokeWidth: Float = 3f
) {
    val currentEndX = startX + (endX - startX) * progress
    val currentEndY = startY + (endY - startY) * progress

    val midX = (startX + currentEndX) / 2
    val midY = (startY + currentEndY) / 2

    val path = Path().apply {
        moveTo(startX, startY)
        cubicTo(midX, startY, midY, currentEndY, currentEndX, currentEndY)
    }

    drawPath(path = path, color = color, style = Stroke(width = strokeWidth))
}
```

### 3.5 成就粒子特效

```kotlin
data class Particle(
    var x: Float,
    var y: Float,
    var velocityX: Float,
    var velocityY: Float,
    var alpha: Float,
    val color: Color,
    val radius: Float
)

/**
 * 粒子系统用于成就解锁特效
 */
class ParticleSystem(
    private val centerX: Float,
    private val centerY: Float,
    particleCount: Int = 30
) {
    val particles: List<Particle>

    init {
        val colors = listOf(Color(0xFFFFD700), Color(0xFF58A6FF), Color(0xFF3FB950), Color(0xFFF0883E))
        particles = (0 until particleCount).map {
            val angle = (2 * PI * it / particleCount).toFloat()
            val speed = (100f + Math.random().toFloat() * 200f)
            Particle(
                x = centerX,
                y = centerY,
                velocityX = cos(angle) * speed,
                velocityY = sin(angle) * speed,
                alpha = 1f,
                color = colors.random(),
                radius = 3f + Math.random().toFloat() * 4f
            )
        }
    }

    /**
     * 更新粒子位置和透明度
     * @param deltaTime 时间增量（秒）
     * @param gravity 重力加速度
     * @return 是否所有粒子都已消失
     */
    fun update(deltaTime: Float, gravity: Float = 100f): Boolean {
        var allDead = true
        particles.forEach { p ->
            p.x += p.velocityX * deltaTime
            p.y += p.velocityY * deltaTime + 0.5f * gravity * deltaTime * deltaTime
            p.velocityY += gravity * deltaTime
            p.alpha -= 0.8f * deltaTime
            if (p.alpha > 0f) allDead = false
        }
        return allDead
    }
}

// 在 Canvas 中绘制粒子
private fun DrawScope.drawParticles(particleSystem: ParticleSystem) {
    particleSystem.particles.forEach { p ->
        if (p.alpha > 0) {
            drawCircle(
                color = p.color.copy(alpha = p.alpha),
                radius = p.radius,
                center = Offset(p.x, p.y)
            )
        }
    }
}
```

### 3.6 画布缩放回弹

```kotlin
@Composable
fun CanvasZoomBounce(
    currentScale: Float,
    onScaleChange: (Float) -> Unit
) {
    val animatedScale = remember { Animatable(currentScale) }

    LaunchedEffect(currentScale) {
        // 限制缩放范围后带弹簧效果回弹
        val clampedScale = currentScale.coerceIn(0.3f, 3.0f)
        if (clampedScale != currentScale) {
            animatedScale.animateTo(
                targetValue = clampedScale,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
            onScaleChange(clampedScale)
        } else {
            animatedScale.snapTo(currentScale)
        }
    }
}
```

---

## 4. 在 SkillTreeScreen 中集成动画

```kotlin
@Composable
fun SkillTreeScreen(...) {
    val animationManager = remember { AnimationManager() }
    var newlyCreatedNodeId by remember { mutableStateOf<String?>(null) }

    // 节点创建时触发动画
    fun onCreateNode(title: String, nodeType: String, parentId: String?) {
        viewModel.createNode(title, nodeType, parentId) { newNodeId ->
            newlyCreatedNodeId = newNodeId
            animationManager.startCreateAnimation(newNodeId)

            // 400ms 后清除动画状态
            viewModelScope.launch {
                delay(500)
                animationManager.removeAnimation(newNodeId)
                newlyCreatedNodeId = null
            }
        }
    }

    // 首次加载时触发树生长动画
    var isFirstLoad by remember { mutableStateOf(true) }
    LaunchedEffect(displayNodes) {
        if (isFirstLoad && displayNodes.isNotEmpty()) {
            isFirstLoad = false
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            // 使用动画状态绘制节点
            treeNodes.forEach { node ->
                val animState = animationManager.getNodeAnimation(node.entity.id)
                val scale = if (animState?.isCreating == true) {
                    // 创建动画的缩放
                    0f // 由 Compose 层处理
                } else 1f

                // 绘制节点...
            }
        }

        // 粒子特效（成就解锁时触发）
        if (showParticleEffect) {
            var particleSystem by remember { mutableStateOf<ParticleSystem?>(null) }
            LaunchedEffect(Unit) {
                particleSystem = ParticleSystem(centerX, centerY)
            }
            // ... 粒子动画循环 ...
        }
    }
}
```

---

## 5. 动画性能优化

| 策略 | 说明 |
|------|------|
| 动画层隔离 | 动画节点使用 `graphicsLayer` 隔离，不触发重组 |
| 动画数量限制 | 同时播放的动画不超过 10 个 |
| 低端设备降级 | 检测设备性能，低端设备禁用粒子特效 |
| 动画暂停 | 画布拖动时暂停非关键动画 |

```kotlin
/**
 * 检测是否为低端设备
 */
object DevicePerformance {
    fun isLowEnd(): Boolean {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        // 内存低于 3GB 视为低端设备
        return memoryInfo.totalMem < 3L * 1024 * 1024 * 1024
    }
}
```

---

## 6. 实施步骤

| 步骤 | 内容 | 优先级 |
|------|------|--------|
| 1 | 创建 `AnimationConfig` 动画配置常量 | P0 |
| 2 | 实现节点创建动画（缩放弹出） | P0 |
| 3 | 实现树生长动画（逐层展开） | P0 |
| 4 | 实现搜索高亮脉冲动画 | P1 |
| 5 | 实现连线生长动画 | P1 |
| 6 | 实现画布缩放回弹动画 | P1 |
| 7 | 实现成就粒子特效 | P2 |
| 8 | 实现成就通知入场动画 | P2 |
| 9 | 添加低端设备性能降级 | P2 |
| 10 | 全面测试动画性能 | P0 |