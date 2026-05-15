# 性能优化方案

> **关联 PRD**: 第 1.5 节（F1.3 视口裁剪）、第 7.5 节（非功能性需求）
> **当前状态**: 无性能优化措施，所有节点全量渲染
> **目标**: 实现视口裁剪、内存管理、渲染优化，支持 1000+ 节点流畅运行

---

## 1. 性能指标目标

| 指标 | 当前 | 目标 (Phase 1) | 目标 (Phase 3) |
|------|------|---------------|---------------|
| 技能树渲染帧率 (100 节点) | ~50fps | ≥ 60fps | ≥ 60fps |
| 技能树渲染帧率 (500 节点) | < 30fps | ≥ 45fps | ≥ 60fps |
| 冷启动时间 | ~800ms | < 500ms | < 300ms |
| 节点详情加载时间 | ~50ms | < 50ms | < 30ms |
| 内存占用 (100 节点) | ~80MB | < 100MB | < 80MB |
| 内存占用 (1000 节点) | N/A | < 200MB | < 150MB |

---

## 2. 视口裁剪（Viewport Culling）

### 2.1 算法设计

```
┌─────────────────────────────────────┐
│          屏幕可视区域                 │
│  ┌─────────────────────────────┐    │
│  │                             │    │
│  │     ✅ 渲染区                │    │
│  │                             │    │
│  │    ┌───┐    ┌───┐          │    │
│  │    │ A │────│ B │          │    │
│  │    └─┬─┘    └───┘          │    │
│  │      │                      │    │
│  │   ┌──┴──┐                  │    │
│  │   │  C  │                  │    │
│  │   └─────┘                  │    │
│  └─────────────────────────────┘    │
│                                     │
│  ❌ 不渲染区（裁剪掉）               │
│  ┌───┐                              │
│  │ D │  ← 在可视区域外，跳过渲染     │
│  └───┘                              │
└─────────────────────────────────────┘
```

### 2.2 实现代码

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
    private val margin: Float = 200f // 扩展边距，避免边缘节点闪烁
) {
    /**
     * 计算世界坐标系下的可视区域
     * 考虑缩放和平移变换的逆变换
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
}

/**
 * 在 Canvas 渲染中使用视口裁剪
 */
private fun DrawScope.drawNodesWithCulling(
    treeNodes: List<TreeNode>,
    positions: List<NodePosition>,
    textMeasurer: TextMeasurer,
    scale: Float,
    offsetX: Float,
    offsetY: Float
) {
    val culler = ViewportCuller(size.width, size.height, scale, offsetX, offsetY)
    val visiblePositions = culler.filterVisibleNodes(positions, NODE_WIDTH, NODE_HEIGHT)
    val visibleNodeIds = visiblePositions.map { it.nodeId }.toSet()

    // 只绘制可见节点的连线
    drawConnectionsFiltered(treeNodes, positions, visibleNodeIds)

    // 只绘制可见节点
    val visibleNodes = treeNodes.filter { it.entity.id in visibleNodeIds }
    // ... 绘制节点 ...
}
```

---

## 3. 树布局算法优化

### 3.1 当前问题

当前使用简化的层次布局算法，存在以下问题：
- 同层节点间距不均匀
- 深层次节点会挤在一起
- 每次 Canvas 重绘都重新计算布局

### 3.2 改进方案：缓存布局结果

```kotlin
/**
 * 树布局结果缓存
 * 只在节点数据变化时重新计算，避免 Canvas 每帧重复计算
 */
class TreeLayoutCache {
    private var cachedNodeMap: Map<String, SkillNodeEntity>? = null
    private var cachedPositions: List<NodePosition>? = null

    /**
     * 获取布局位置，如果节点数据未变化则使用缓存
     */
    fun getLayout(
        nodes: List<SkillNodeEntity>,
        canvasWidth: Float,
        canvasHeight: Float
    ): List<NodePosition> {
        val currentMap = nodes.associateBy { it.id }

        if (cachedNodeMap == currentMap && cachedPositions != null) {
            return cachedPositions!!
        }

        val treeNodes = buildTreeNodes(nodes)
        val positions = calculateNodePositions(treeNodes, canvasWidth, canvasHeight)

        cachedNodeMap = currentMap
        cachedPositions = positions

        return positions
    }

    fun invalidate() {
        cachedNodeMap = null
        cachedPositions = null
    }
}
```

### 3.3 Reingold-Tilford 算法（Phase 1 后期可选）

```
简化版 Reingold-Tilford 算法实现要点:

1. 后序遍历计算每个子树的轮廓（左轮廓 + 右轮廓）
2. 相邻子树之间计算最小间距（避免重叠）
3. 父节点定位在子节点中心
4. 使用偏移量（modifier）累加子树位置调整

伪代码:
function layout(node, depth):
    if node has no children:
        node.x = 0
        node.y = depth * nodeSpacing
        return node

    for child in node.children:
        layout(child, depth + 1)

    // 两两调整子树间距
    for i in 1..children.size-1:
        leftSubtree = children[i-1]
        rightSubtree = children[i]
        minSeparation = calculateMinSeparation(leftSubtree, rightSubtree)
        shiftRightSubtree(rightSubtree, minSeparation)

    // 父节点居中
    node.x = (firstChild.x + lastChild.x) / 2
    node.y = depth * nodeSpacing
    return node
```

---

## 4. 内存管理优化

### 4.1 图片加载优化

```kotlin
/**
 * Coil 图片加载配置
 * 限制内存缓存大小，避免 OOM
 */
@Module
@InstallIn(SingletonComponent::class)
object ImageLoaderModule {
    @Provides
    @Singleton
    fun provideImageLoader(@ApplicationContext context: Context): ImageLoader {
        return ImageLoader.Builder(context)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .memoryCache {
                MemoryCache.Builder(context)
                    .maxSizePercent(0.15) // 最多占用 15% 可用内存
                    .build()
            }
            .diskCachePolicy(CachePolicy.ENABLED)
            .diskCache {
                DiskCache.Builder()
                    .directory(context.cacheDir.resolve("image_cache"))
                    .maxSizeBytes(50 * 1024 * 1024) // 50MB 磁盘缓存
                    .build()
            }
            .crossfade(true)
            .build()
    }
}
```

### 4.2 大列表虚拟化

当节点列表用于搜索结果显示时（非 Canvas 场景），使用 LazyColumn 虚拟化：

```kotlin
@Composable
fun SearchResultsList(nodes: List<SkillNodeEntity>, onNodeClick: (String) -> Unit) {
    LazyColumn {
        items(nodes, key = { it.id }) { node ->
            SearchResultItem(node = node, onClick = { onNodeClick(node.id) })
        }
    }
}
```

### 4.3 文本渲染优化

```kotlin
/**
 * 文本缓存管理器
 * 缓存已测量的 TextLayoutResult，避免重复测量
 */
class TextMeasureCache(private val maxSize: Int = 500) {
    private val cache = LinkedHashMap<String, TextLayoutResult>(maxSize, 0.75f, true)

    fun getOrMeasure(
        key: String,
        text: String,
        style: TextStyle,
        measurer: TextMeasurer
    ): TextLayoutResult {
        return cache.getOrPut(key) {
            measurer.measure(text = text, style = style)
        }
    }

    fun clear() = cache.clear()
}
```

---

## 5. Canvas 渲染优化

### 5.1 减少不必要的重绘

```kotlin
@Composable
fun SkillTreeScreen(...) {
    // ✅ 使用 remember + derivedStateOf 减少重组
    val treeNodes by remember {
        derivedStateOf { buildTreeNodes(displayNodes) }
    }

    // ✅ 布局缓存
    val layoutCache = remember { TreeLayoutCache() }

    // ✅ 文本测量缓存
    val textCache = remember { TextMeasureCache() }

    // ✅ 只在节点数据变化时 invalidate 缓存
    LaunchedEffect(displayNodes) {
        layoutCache.invalidate()
        textCache.clear()
    }
}
```

### 5.2 连线渲染优化

```kotlin
/**
 * 预计算并缓存贝塞尔曲线 Path
 * 只在节点位置变化时重新计算
 */
class ConnectionPathCache(private val maxSize: Int = 1000) {
    private val cache = LruCache<String, Path>(maxSize)

    fun getOrCreatePath(
        key: String,
        startX: Float, startY: Float,
        endX: Float, endY: Float
    ): Path {
        return cache.get(key) ?: run {
            Path().apply {
                moveTo(startX, startY)
                cubicTo(
                    startX, (startY + endY) / 2,
                    endX, (startY + endY) / 2,
                    endX, endY
                )
            }.also { cache.put(key, it) }
        }
    }
}
```

---

## 6. 数据库查询优化

### 6.1 索引优化

```sql
-- 在 Room Entity 中已定义的索引
-- skill_node: parentId (已存在)
-- node_link: sourceId, targetId (已存在)
-- node_tag: nodeId, tagId (已存在)
-- attachment: nodeId (已存在)

-- 新增建议索引（高频查询）
CREATE INDEX IF NOT EXISTS idx_skill_node_type ON skill_node(nodeType);
CREATE INDEX IF NOT EXISTS idx_skill_node_created ON skill_node(createdAt);
CREATE INDEX IF NOT EXISTS idx_skill_node_title ON skill_node(title);
```

### 6.2 分页加载

```kotlin
@Dao
interface SkillNodeDao {
    // ✅ 现有全量查询
    @Query("SELECT * FROM skill_node ORDER BY sortOrder ASC")
    fun getAllNodes(): Flow<List<SkillNodeEntity>>

    // ✅ 新增分页查询（节点数 > 200 时使用）
    @Query("SELECT * FROM skill_node ORDER BY sortOrder ASC LIMIT :limit OFFSET :offset")
    fun getNodesPaged(limit: Int, offset: Int): Flow<List<SkillNodeEntity>>
}
```

### 6.3 数据库连接池配置

```kotlin
@Provides
@Singleton
fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
    return Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.DATABASE_NAME)
        .setJournalMode(JournalMode.WRITE_AHEAD_LOGGING) // WAL 模式提升并发读
        .setQueryExecutor(Executors.newFixedThreadPool(4)) // 查询线程池
        .build()
}
```

---

## 7. 启动优化

### 7.1 延迟初始化

```kotlin
@HiltAndroidApp
class SkillTreeApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // 延迟初始化非关键组件
        CoroutineScope(Dispatchers.Default).launch {
            delay(1000) // 等主界面渲染完成
            initializeNonCriticalComponents()
        }
    }

    private suspend fun initializeNonCriticalComponents() {
        // 预加载 Markwon 实例
        // 预热图片缓存
        // 检查数据库备份
    }
}
```

### 7.2 Splash Screen 优化

使用 Android 12+ SplashScreen API，确保冷启动体验：

```kotlin
// MainActivity.kt
override fun onCreate(savedInstanceState: Bundle?) {
    installSplashScreen() // 必须在 super.onCreate 之前调用
    super.onCreate(savedInstanceState)
    // ...
}
```

---

## 8. 性能监控

```kotlin
/**
 * 简单的性能监控工具
 * 在 debug 构建中启用，release 中移除
 */
object PerformanceMonitor {
    private val frameTimes = mutableListOf<Long>()

    fun recordFrameTime(durationMs: Long) {
        if (BuildConfig.DEBUG) {
            frameTimes.add(durationMs)
            if (frameTimes.size > 100) frameTimes.removeAt(0)
        }
    }

    fun getAverageFrameTime(): Long {
        if (frameTimes.isEmpty()) return 0
        return frameTimes.average().toLong()
    }

    fun getFps(): Float {
        val avg = getAverageFrameTime()
        if (avg == 0L) return 60f
        return 1000f / avg
    }
}
```

---

## 9. 实施步骤

| 步骤 | 内容 | 优先级 |
|------|------|--------|
| 1 | 实现 `ViewportCuller` 视口裁剪类 | P0 |
| 2 | 实现 `TreeLayoutCache` 布局缓存 | P0 |
| 3 | 实现 `TextMeasureCache` 文本测量缓存 | P1 |
| 4 | 实现 `ConnectionPathCache` 连线路径缓存 | P1 |
| 5 | 配置 Coil 内存/磁盘缓存限制 | P1 |
| 6 | 添加数据库查询索引 | P1 |
| 7 | 配置 WAL 模式和查询线程池 | P2 |
| 8 | 实现启动延迟初始化 | P2 |
| 9 | 集成 SplashScreen API | P2 |
| 10 | 添加 `PerformanceMonitor` debug 监控 | P2 |
| 11 | 使用 Android Studio Profiler 验证 | P0 |