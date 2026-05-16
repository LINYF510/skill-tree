# 提示词：粒子特效 + 节点解锁动画

## 背景

项目当前已实现 7 种动画效果（节点创建弹簧、树生长逐层展开、选中脉冲、搜索高亮呼吸、连线PathMeasure绘制、成就解锁滑入+缩放弹出、页面转场），但 PRD 中规划的**粒子特效**和**节点解锁动画**尚未实现。

### 现有架构

- **AnimationConfig**：集中管理动画常量（时长、弹簧参数等）
- **AnimationParams**：Canvas 绘制时传递的动画状态数据类
- **SkillTreeScreen.kt**：Canvas 中通过 `drawNodes()`/`drawConnections()` 绘制节点和连线，动画通过 `Animatable`/`infiniteTransition` 驱动
- **NodeLevelConfig**：5 个等级（Novice→Master），每个等级有 emoji/glowColor/glowRadius/sizeMultiplier/borderWidth

### 粒子系统现状

PRD `12-animation-system.md` 中规划了 `ParticleSystem` 和 `Particle` 数据类（P2 优先级），但**尚未实现**，项目中无任何粒子相关代码。

## 目标

实现两种视觉效果：
1. **粒子特效**：节点创建/升级时从节点中心向外发射彩色粒子
2. **节点解锁动画**：节点等级提升时的光效+缩放动画

## 实施步骤

### Part A: Particle 数据类和 ParticleSystem

**新建文件**: `app/src/main/java/com/fancy/skill_tree/core/ui/animation/ParticleSystem.kt`

```kotlin
/**
 * 单个粒子
 */
data class Particle(
    var x: Float,
    var y: Float,
    var velocityX: Float,
    var velocityY: Float,
    var alpha: Float,
    val color: Color,
    val radius: Float,
    val life: Float,
    val maxLife: Float
)

/**
 * 粒子系统
 * 管理粒子的创建、更新和生命周期
 */
class ParticleSystem(
    private val centerX: Float,
    private val centerY: Float,
    private val particleCount: Int = 30,
    private val colors: List<Color> = emptyList()
) {
    val particles: MutableList<Particle> = mutableListOf()
    var isAlive: Boolean = true
        private set

    /**
     * 初始化粒子，从中心点向外随机发射
     */
    fun emit() { ... }

    /**
     * 更新所有粒子的位置和透明度
     * @param deltaTime 帧间隔时间（秒）
     * @param gravity 重力加速度
     * @return 是否还有存活粒子
     */
    fun update(deltaTime: Float, gravity: Float = 100f): Boolean { ... }
}
```

设计要点：
- 粒子从 `(centerX, centerY)` 向四周随机方向发射
- 速度范围：50~200 px/s，角度随机 0~2π
- 初始 alpha=1.0，随 life 衰减到 0
- radius 范围：2~6 px
- life 范围：0.5~1.5 秒
- colors 为空时使用主题 primary 色
- `update()` 返回 `true` 表示仍有存活粒子，`false` 表示全部消亡
- `isAlive` 在所有粒子消亡后设为 `false`

### Part B: AnimationConfig 新增常量

**文件**: `app/src/main/java/com/fancy/skill_tree/core/ui/animation/AnimationConfig.kt`

新增：
```kotlin
const val PARTICLE_COUNT = 30
const val PARTICLE_GRAVITY = 100f
const val NODE_UNLOCK_DURATION = 600
const val NODE_UNLOCK_GLOW_MAX_RADIUS = 30f
```

### Part C: AnimationParams 扩展

**文件**: `app/src/main/java/com/fancy/skill_tree/core/ui/animation/AnimationConfig.kt`

在 `AnimationParams` data class 中新增字段：
```kotlin
data class AnimationParams(
    val newlyCreatedNodeId: String? = null,
    val creationScale: Float = 1f,
    val growthProgress: Float = 100f,
    val searchPulseAlpha: Float = 0f,
    val selectedPulseAlpha: Float = 0f,
    val selectedNodeId: String? = null,
    val isSearchActive: Boolean = false,
    val unlockingNodeId: String? = null,
    val unlockProgress: Float = 0f,
    val particleSystems: Map<String, ParticleSystem> = emptyMap()
)
```

- `unlockingNodeId`：正在执行解锁动画的节点 ID
- `unlockProgress`：解锁动画进度 (0..1)
- `particleSystems`：节点 ID → ParticleSystem 的映射，用于 Canvas 绘制粒子

### Part D: Canvas 绘制粒子

**文件**: `app/src/main/java/com/fancy/skill_tree/feature/tree/SkillTreeScreen.kt`

1. 新增 `drawParticles` 扩展函数：
```kotlin
/**
 * 绘制粒子特效
 */
private fun DrawScope.drawParticles(
    particleSystems: Map<String, ParticleSystem>,
    positionMap: Map<String, NodePosition>,
    themeColors: ThemeColors
) {
    particleSystems.forEach { (nodeId, system) ->
        system.particles.forEach { particle ->
            if (particle.alpha > 0.01f) {
                drawCircle(
                    color = particle.color.copy(alpha = particle.alpha),
                    radius = particle.radius,
                    center = Offset(particle.x, particle.y)
                )
            }
        }
    }
}
```

2. 在 Canvas 的 `withTransform` 块中，在 `drawConnections` 和 `drawNodes` 之后调用 `drawParticles`。

### Part E: Canvas 绘制节点解锁光效

**文件**: `app/src/main/java/com/fancy/skill_tree/feature/tree/SkillTreeScreen.kt`

在 `drawNodes` 的 `drawTreeNodes` 函数中，在节点主体绘制之前，新增解锁光效：
```kotlin
// 节点解锁光效
if (node.entity.id == animationParams.unlockingNodeId && animationParams.unlockProgress > 0f) {
    val glowRadius = AnimationConfig.NODE_UNLOCK_GLOW_MAX_RADIUS * animationParams.unlockProgress
    val glowAlpha = (1f - animationParams.unlockProgress) * 0.6f
    drawCircle(
        color = nodeColor.copy(alpha = glowAlpha),
        radius = scaledWidth / 2 + glowRadius,
        center = Offset(drawX, drawY)
    )
}
```

### Part F: SkillTreeScreen 中驱动粒子动画

**文件**: `app/src/main/java/com/fancy/skill_tree/feature/tree/SkillTreeScreen.kt`

1. 新增粒子系统状态：
```kotlin
val particleSystems = remember { mutableStateMapOf<String, ParticleSystem>() }
```

2. 在节点创建时触发粒子发射：
```kotlin
LaunchedEffect(uiState.nodes) {
    val currentNodeIds = uiState.nodes.map { it.id }.toSet()
    val newIds = currentNodeIds - previousNodeIds
    if (hasPlayedGrowthAnimation && previousNodeIds.isNotEmpty() && newIds.isNotEmpty()) {
        newlyCreatedNodeId = newIds.first()
        // 在节点位置发射粒子
        val newNodePos = layoutCache.getOrComputePositions(...).find { it.nodeId in newIds }
        if (newNodePos != null) {
            val system = ParticleSystem(
                centerX = newNodePos.x,
                centerY = newNodePos.y,
                particleCount = AnimationConfig.PARTICLE_COUNT,
                colors = listOf(colors.primary, colors.ability, colors.resource)
            )
            system.emit()
            particleSystems[newNodePos.nodeId] = system
        }
        // ... 原有创建动画逻辑
    }
}
```

3. 使用 `LaunchedEffect` + `frameNanos` 驱动粒子更新循环：
```kotlin
LaunchedEffect(particleSystems.toMap()) {
    if (particleSystems.isEmpty()) return@LaunchedEffect
    while (true) {
        val frameTime = withFrameNanos { it } / 1_000_000_000f
        val deadKeys = mutableListOf<String>()
        particleSystems.forEach { (key, system) ->
            if (!system.update(deltaTime = 1f / 60f)) {
                deadKeys.add(key)
            }
        }
        deadKeys.forEach { particleSystems.remove(it) }
        if (particleSystems.isEmpty()) break
    }
}
```

### Part G: 节点升级解锁动画

**文件**: `app/src/main/java/com/fancy/skill_tree/feature/tree/SkillTreeScreen.kt`

1. 检测节点等级变化：
```kotlin
val previousNodeLevels = remember { mutableStateMapOf<String, Int>() }

LaunchedEffect(uiState.nodes) {
    uiState.nodes.forEach { node ->
        val prevLevel = previousNodeLevels[node.id]
        if (prevLevel != null && node.level > prevLevel) {
            // 触发解锁动画
            unlockingNodeId = node.id
            unlockProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(AnimationConfig.NODE_UNLOCK_DURATION)
            )
            // 同时发射粒子
            val nodePos = layoutCache.getOrComputePositions(...).find { it.nodeId == node.id }
            if (nodePos != null) {
                val levelConfig = NodeLevelConfig.forLevel(node.level)
                val system = ParticleSystem(
                    centerX = nodePos.x,
                    centerY = nodePos.y,
                    particleCount = AnimationConfig.PARTICLE_COUNT * 2,
                    colors = listOf(Color(levelConfig.glowColor), colors.primary)
                )
                system.emit()
                particleSystems[node.id] = system
            }
            unlockingNodeId = null
            unlockProgress.snapTo(0f)
        }
        previousNodeLevels[node.id] = node.level
    }
}
```

2. 新增动画状态：
```kotlin
val unlockingNodeId = remember { mutableStateOf<String?>(null) }
val unlockProgress = remember { Animatable(0f) }
```

### Part H: 低端设备降级

**文件**: `app/src/main/java/com/fancy/skill_tree/core/ui/animation/AnimationConfig.kt`

新增：
```kotlin
var areParticlesEnabled: Boolean = true
```

在 `SkillTreeScreen` 中，通过 `PerformanceMonitor` 检测帧率，如果连续 10 帧低于 30fps，自动禁用粒子：
```kotlin
LaunchedEffect(performanceMonitor.averageFrameTime) {
    if (performanceMonitor.averageFrameTime > 33.0 && AnimationConfig.areParticlesEnabled) {
        AnimationConfig.areParticlesEnabled = false
    }
}
```

在粒子发射前检查 `AnimationConfig.areParticlesEnabled`，禁用时跳过。

## 约束

1. **不引入新的第三方依赖**——粒子系统纯 Canvas 实现
2. **性能优先**——粒子数量默认 30 个，单次动画最多 60 个（升级时 2x），低端设备自动降级
3. **粒子生命周期管理**——所有粒子消亡后自动清理 ParticleSystem，避免内存泄漏
4. **所有 public 函数必须有 KDoc 注释**
5. **禁止使用 !! 非空断言**
6. **粒子颜色使用 ThemeColors 中的颜色**，不硬编码色值
7. **新增字符串资源必须同时添加英文和中文版本**（如有 UI 文本）

## 测试要求

1. `ParticleSystemTest`：测试粒子发射、更新、生命周期、消亡判定
2. `AnimationParamsTest`：测试新增字段的默认值
3. 验证粒子系统在所有粒子消亡后 `isAlive == false`
4. 验证低端设备降级逻辑

## 验证

1. `./gradlew assembleDebug` 编译通过
2. `./gradlew test` 全部测试通过
3. 创建新节点时可见粒子从节点中心向外发射
4. 节点升级时可见光效扩散 + 粒子发射
5. 粒子在 1~2 秒内自然消亡，无残留
