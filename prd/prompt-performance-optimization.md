# 性能优化实现提示词

## 项目背景

Skill-Tree 项目已完成核心功能、错误处理和测试体系，现在需要实现性能优化。当前 Canvas 每帧全量计算布局和绘制所有节点，节点数超过 200 时帧率会显著下降。

**重要**: 请先阅读项目规则 `CLAUDE.md` 和性能优化方案 `prd/03-performance-optimization.md`。

---

## 当前代码现状

### Canvas 渲染（SkillTreeScreen.kt）

- `buildTreeNodes()` 第 832 行：每帧重建 TreeNode 树结构
- `calculateNodePositions()` 第 854 行：每帧递归计算所有节点位置
- `drawConnections()` 第 909 行：每帧绘制所有连线
- `drawNodes()` 第 976 行：每帧绘制所有节点
- `infiniteTransition` 始终运行（搜索脉冲+选中脉冲），即使无搜索/选中状态
- 无视口裁剪，屏幕外节点也全部绘制

### 数据库（DatabaseModule.kt）

- 无 WAL 模式配置
- 无查询线程池配置
- 无额外索引（仅有 parentId、sourceId/targetId、nodeId/tagId）

### 图片加载

- 使用 Coil AsyncImage 但无自定义 ImageLoader 配置
- 无内存/磁盘缓存限制

### 已有索引

- SkillNodeEntity: `parentId`
- NodeLinkEntity: `sourceId`, `targetId`
- NodeTagCrossRef: `nodeId`, `tagId`
- AttachmentEntity: `nodeId`

---

## 实现任务

### 任务 1：实现 ViewportCuller 视口裁剪（P0）

**新增文件**: `app/src/main/java/com/fancy/skill_tree/core/ui/render/ViewportCuller.kt`

```kotlin
package com.fancy.skill_tree.core.ui.render

import android.graphics.RectF
import com.fancy.skill_tree.feature.tree.NodePosition

/**
 * 视口裁剪管理器
 * 根据当前可视区域过滤需要渲染的节点和连线
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
     */
    private val visibleRect: RectF by lazy {
        val left = (-offsetX - margin) / scale
        val top = (-offsetY - margin) / scale
        val right = (viewportWidth - offsetX + margin) / scale
        val bottom = (viewportHeight - offsetY + margin) / scale
        RectF(left, top, right, bottom)
    }

    /**
     * 判断节点是否在可视区域内
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
     */
    fun getVisibleNodeIds(
        positions: List<NodePosition>,
        nodeWidth: Float,
        nodeHeight: Float
    ): Set<String> {
        return filterVisibleNodes(positions, nodeWidth, nodeHeight).map { it.nodeId }.toSet()
    }
}
```

### 任务 2：实现 TreeLayoutCache 布局缓存（P0）

**新增文件**: `app/src/main/java/com/fancy/skill_tree/core/ui/render/TreeLayoutCache.kt`

```kotlin
package com.fancy.skill_tree.core.ui.render

import com.fancy.skill_tree.core.domain.entity.SkillNodeEntity
import com.fancy.skill_tree.feature.tree.NodePosition
import com.fancy.skill_tree.feature.tree.TreeNode

/**
 * 树布局结果缓存
 * 只在节点数据变化时重新计算，避免 Canvas 每帧重复计算
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
     */
    fun getOrComputeTreeNodes(
        nodes: List<SkillNodeEntity>,
        compute: (List<SkillNodeEntity>) -> List<TreeNode>
    ): List<TreeNode> {
        val currentHash = nodes.map { it.id to it.updatedAt }.hashCode()
        if (cachedNodeHash == currentHash && cachedTreeNodes != null) {
            return cachedTreeNodes!!
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
            return cachedPositions!!
        }
        val result = compute(treeNodes, canvasWidth, canvasHeight)
        cachedPositions = result
        cachedCanvasWidth = canvasWidth
        cachedCanvasHeight = canvasHeight
        return result
    }

    /**
     * 使缓存失效
     */
    fun invalidate() {
        cachedNodeHash = null
        cachedTreeNodes = null
        cachedPositions = null
        cachedCanvasWidth = null
        cachedCanvasHeight = null
    }
}
```

### 任务 3：集成 ViewportCuller 和 TreeLayoutCache 到 SkillTreeScreen（P0）

**修改文件**: `app/src/main/java/com/fancy/skill_tree/feature/tree/SkillTreeScreen.kt`

改造要点：

1. 在 `SkillTreeContent` Composable 中添加缓存实例：

```kotlin
val layoutCache = remember { TreeLayoutCache() }
```

2. 将 `buildTreeNodes` 和 `calculateNodePositions` 调用替换为缓存版本：

```kotlin
// 改造前
val treeNodes = buildTreeNodes(displayNodes)
// ...
val nodePositions = calculateNodePositions(treeNodes, size.width, size.height)

// 改造后
val treeNodes = layoutCache.getOrComputeTreeNodes(displayNodes) { nodes ->
    buildTreeNodes(nodes)
}
// ...
val nodePositions = layoutCache.getOrComputePositions(treeNodes, size.width, size.height) { t, w, h ->
    calculateNodePositions(t, w, h)
}
```

3. 在 Canvas 的 `drawNodes` 和 `drawConnections` 中集成视口裁剪：

```kotlin
// 在 drawScope 内部，绘制前先裁剪
val culler = ViewportCuller(size.width, size.height, scale, offsetX, offsetY)
val visiblePositions = culler.filterVisibleNodes(nodePositions, NODE_WIDTH, NODE_HEIGHT)
val visibleNodeIds = visiblePositions.map { it.nodeId }.toSet()
```

4. `drawConnections` 只绘制两端节点都可见的连线
5. `drawNodes` 只绘制可见节点

6. 节点数据变化时使缓存失效：

```kotlin
LaunchedEffect(displayNodes) {
    layoutCache.invalidate()
}
```

7. `infiniteTransition` 条件控制：将搜索脉冲和选中脉冲的动画只在对应状态激活时运行

```kotlin
// 改造前：始终运行
val searchPulseAlpha by infiniteTransition.animateFloat(
    initialValue = 0.2f, targetValue = 0.8f,
    animationSpec = infiniteRepeatable(...)
)

// 改造后：仅在搜索激活时运行
val isSearchActive = uiState.searchQuery.isNotEmpty()
val searchPulseAlpha by if (isSearchActive) {
    infiniteTransition.animateFloat(...)
} else {
    remember { mutableStateOf(0f) }
}
```

**注意**: 上述 `if` 分支在 Compose 中不能直接用于 `by` 委托。正确做法是始终创建动画，但在绘制时判断是否使用动画值：

```kotlin
val searchPulseAlpha by infiniteTransition.animateFloat(
    initialValue = 0.2f, targetValue = 0.8f,
    animationSpec = infiniteRepeatable(
        animation = tween(SEARCH_PULSE_DURATION, easing = FastOutSlowInEasing),
        repeatMode = RepeatMode.Reverse
    ),
    label = "search_pulse"
)

// 在 AnimationParams 构建时判断
val currentAnimationParams = AnimationParams(
    // ...
    searchPulseAlpha = if (isSearchActive) searchPulseAlpha else 0f,
    isSearchActive = isSearchActive
)
```

### 任务 4：配置 Coil ImageLoader 内存/磁盘缓存（P1）

**新增文件**: `app/src/main/java/com/fancy/skill_tree/di/ImageLoaderModule.kt`

```kotlin
package com.fancy.skill_tree.di

import android.content.Context
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt 图片加载模块
 * 配置 Coil 内存和磁盘缓存限制
 */
@Module
@InstallIn(SingletonComponent::class)
object ImageLoaderModule {

    /**
     * 提供自定义 ImageLoader 实例
     * 限制内存缓存为可用内存的 15%，磁盘缓存为 50MB
     */
    @Provides
    @Singleton
    fun provideImageLoader(@ApplicationContext context: Context): ImageLoader {
        return ImageLoader.Builder(context)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .memoryCache {
                MemoryCache.Builder(context)
                    .maxSizePercent(0.15)
                    .build()
            }
            .diskCachePolicy(CachePolicy.ENABLED)
            .diskCache {
                DiskCache.Builder()
                    .directory(context.cacheDir.resolve("image_cache"))
                    .maxSizeBytes(50L * 1024 * 1024)
                    .build()
            }
            .crossfade(true)
            .build()
    }
}
```

### 任务 5：添加数据库查询索引（P1）

**修改文件**: `app/src/main/java/com/fancy/skill_tree/core/domain/entity/Entities.kt`

在 SkillNodeEntity 的 `@Entity` 注解中添加索引：

```kotlin
// 改造前
@Entity(
    tableName = "skill_node",
    indices = [Index("parentId")]
)

// 改造后
@Entity(
    tableName = "skill_node",
    indices = [
        Index("parentId"),
        Index("nodeType"),
        Index("title"),
        Index("createdAt")
    ]
)
```

**注意**: 添加索引需要增加 AppDatabase 的版本号并添加迁移，或者使用 `fallbackToDestructiveMigration()`（当前已使用）。

### 任务 6：配置数据库 WAL 模式和查询线程池（P2）

**修改文件**: `app/src/main/java/com/fancy/skill_tree/di/DatabaseModule.kt`

```kotlin
// 改造前
return Room.databaseBuilder(
    context,
    AppDatabase::class.java,
    AppDatabase.DATABASE_NAME
)
    .fallbackToDestructiveMigration()
    .build()

// 改造后
return Room.databaseBuilder(
    context,
    AppDatabase::class.java,
    AppDatabase.DATABASE_NAME
)
    .setJournalMode(JournalMode.WRITE_AHEAD_LOGGING)
    .setQueryExecutor(Executors.newFixedThreadPool(4))
    .fallbackToDestructiveMigration()
    .build()
```

添加 `import`:
```kotlin
import androidx.room.RoomDatabase.JournalMode
import java.util.concurrent.Executors
```

### 任务 7：实现 PerformanceMonitor 调试监控（P2）

**新增文件**: `app/src/main/java/com/fancy/skill_tree/core/ui/render/PerformanceMonitor.kt`

```kotlin
package com.fancy.skill_tree.core.ui.render

import com.fancy.skill_tree.BuildConfig

/**
 * 简单的性能监控工具
 * 在 debug 构建中启用，release 中自动跳过
 */
object PerformanceMonitor {
    private val frameTimes = mutableListOf<Long>()

    /**
     * 记录单帧渲染时间
     */
    fun recordFrameTime(durationMs: Long) {
        if (BuildConfig.DEBUG) {
            frameTimes.add(durationMs)
            if (frameTimes.size > 100) frameTimes.removeAt(0)
        }
    }

    /**
     * 获取平均帧时间
     */
    fun getAverageFrameTime(): Long {
        if (frameTimes.isEmpty()) return 0
        return frameTimes.average().toLong()
    }

    /**
     * 获取当前估算 FPS
     */
    fun getFps(): Float {
        val avg = getAverageFrameTime()
        if (avg == 0L) return 60f
        return 1000f / avg
    }

    /**
     * 清除记录
     */
    fun clear() = frameTimes.clear()
}
```

---

## 实现优先级

1. **P0（必须）**: 任务 1-3（ViewportCuller + TreeLayoutCache + SkillTreeScreen 集成）
2. **P1（重要）**: 任务 4-5（Coil 缓存 + 数据库索引）
3. **P2（可选）**: 任务 6-7（WAL 模式 + PerformanceMonitor）

---

## 编码规范提醒

- 所有 public 函数必须有 KDoc 注释
- 禁止使用 `!!` 非空断言
- ViewportCuller 和 TreeLayoutCache 应为无状态或可重置的工具类
- 性能优化不应改变现有功能行为
- Canvas 绘制逻辑修改后需确保搜索高亮、动画效果正常

---

## 实现完成后

1. 运行 `./gradlew assembleDebug` 确保编译通过
2. 运行 `./gradlew test` 确保测试通过
3. 将本次实现内容总结写入 `doc/rep/2026-05-15-性能优化.md`，包含：
   - 实现概述
   - 新增/修改文件列表
   - 关键代码片段
   - 性能优化效果预期
   - 已知问题
