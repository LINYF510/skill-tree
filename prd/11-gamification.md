# 游戏化系统实现方案（节点等级 + 成就 + 统计面板）

> **关联 PRD**: 第 4.5 节（游戏化元素）
> **当前状态**: 零实现
> **目标**: 实现节点等级自动计算、成就系统、统计面板

---

## 1. 节点等级系统

### 1.1 等级判定规则

| 等级 | 条件 | 视觉效果 | 节点大小倍率 |
|------|------|---------|-------------|
| **Lv.1** 🌱 | 仅有标题 | 小节点，无光效 | 0.8x |
| **Lv.2** 🌿 | 标题 + 描述（> 20 字） | 中等节点，微弱光效 | 0.9x |
| **Lv.3** 🌳 | 标题 + 内容（> 100 字）+ 1 个标签 | 标准节点，稳定光效 | 1.0x |
| **Lv.4** ⭐ | 内容（> 300 字）+ 2 个标签 + 1 个附件 | 较大节点，明亮光效 | 1.15x |
| **Lv.5** 👑 | 内容（> 500 字）+ 3 个标签 + 附件 + 链接 | 最大节点，金色光效 | 1.3x |

### 1.2 等级计算 UseCase

```kotlin
package com.fancy.skill_tree.core.domain.usecase.gamification

import com.fancy.skill_tree.core.data.repository.SkillTreeRepository
import javax.inject.Inject

class CalculateNodeLevelUseCase @Inject constructor(
    private val repository: SkillTreeRepository
) {
    suspend operator fun invoke(nodeId: String): Int {
        val node = repository.getNodeById(nodeId) ?: return 1
        val contentLength = node.content?.length ?: 0
        val tagCount = 0 // TODO: 接入标签统计
        val attachmentCount = 0 // TODO: 接入附件统计
        val linkCount = 0 // TODO: 接入链接统计

        return when {
            contentLength >= 500 && tagCount >= 3 && attachmentCount >= 1 && linkCount >= 1 -> 5
            contentLength >= 300 && tagCount >= 2 && attachmentCount >= 1 -> 4
            contentLength >= 100 && tagCount >= 1 -> 3
            contentLength >= 20 -> 2
            else -> 1
        }
    }
}

data class NodeLevelConfig(
    val level: Int,
    val emoji: String,
    val label: String,
    val glowColor: Long,
    val glowRadius: Float,
    val sizeMultiplier: Float,
    val borderWidth: Float
) {
    companion object {
        fun forLevel(level: Int): NodeLevelConfig = when (level) {
            5 -> NodeLevelConfig(5, "👑", "大师", 0x80FFD700, 20f, 1.3f, 3f)
            4 -> NodeLevelConfig(4, "⭐", "专家", 0x8058A6FF, 15f, 1.15f, 2.5f)
            3 -> NodeLevelConfig(3, "🌳", "熟练", 0x403FB950, 10f, 1.0f, 2f)
            2 -> NodeLevelConfig(2, "🌿", "入门", 0x208B949E, 5f, 0.9f, 1.5f)
            else -> NodeLevelConfig(1, "🌱", "新手", 0x00000000, 0f, 0.8f, 1f)
        }
    }
}
```

### 1.3 Canvas 中的等级光效渲染

```kotlin
private fun DrawScope.drawNodeGlow(position: NodePosition, levelConfig: NodeLevelConfig) {
    if (levelConfig.level <= 1) return

    val glowColor = Color(levelConfig.glowColor)
    val centerX = position.x; val centerY = position.y
    val radius = maxOf(NODE_WIDTH, NODE_HEIGHT) * levelConfig.sizeMultiplier / 2 + levelConfig.glowRadius

    for (i in 2 downTo 0) {
        val alpha = (0.05f * (i + 1)).coerceAtMost(0.15f)
        val r = radius - i * (levelConfig.glowRadius / 3)
        drawCircle(color = glowColor.copy(alpha = alpha), radius = r, center = Offset(centerX, centerY))
    }
}
```

---

## 2. 成就系统

### 2.1 成就定义

```kotlin
enum class Achievement(
    val id: String, val emoji: String, val title: String,
    val description: String, val reward: String
) {
    FIRST_NODE("first_node", "🌱", "初出茅庐", "创建第一个节点", "解锁自定义图标"),
    TEN_NODES("ten_nodes", "🌿", "枝繁叶茂", "拥有 10 个节点", "解锁自定义主题色"),
    FIFTY_NODES("fifty_nodes", "🌳", "技能大师", "拥有 50 个节点", "解锁特殊树干样式"),
    TWENTY_LINKS("twenty_links", "🔗", "链接达人", "创建 20 条跨分支链接", "解锁特殊连线样式"),
    TEN_AI_CONFIRMS("ten_ai_confirms", "🤖", "AI 先驱", "确认 10 条 AI 推荐链接", "解锁 AI 专属光效"),
    FIVE_LEVEL5("five_level5", "✍️", "内容王者", "拥有 5 个 Lv.5 节点", "解锁金色主题"),
    TEN_VOICE("ten_voice", "🎤", "语音记录者", "使用语音输入 10 次", "解锁语音波形动画"),
    TWENTY_IMAGES("twenty_images", "📸", "图文并茂", "插入 20 张图片", "解锁图片墙展示模式"),
    DAILY_STREAK_7("daily_7", "🔥", "连续记录", "连续 7 天创建/编辑节点", "解锁连续天数徽章"),
    TREE_DEPTH_5("depth_5", "🏔️", "深不可测", "技能树深度达到 5 层", "解锁深层探索者称号")
}
```

### 2.2 成就检查 UseCase

```kotlin
@Singleton
class CheckAchievementsUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: SkillTreeRepository
) {
    private val prefs: SharedPreferences
        get() = context.getSharedPreferences("achievements", Context.MODE_PRIVATE)

    suspend operator fun invoke(): List<Achievement> {
        val newAchievements = mutableListOf<Achievement>()
        val nodeCount = repository.getNodeCount().first()
        val linkCount = 0 // TODO
        val aiConfirmCount = 0 // TODO

        checkAndUnlock(Achievement.FIRST_NODE, nodeCount >= 1, newAchievements)
        checkAndUnlock(Achievement.TEN_NODES, nodeCount >= 10, newAchievements)
        checkAndUnlock(Achievement.FIFTY_NODES, nodeCount >= 50, newAchievements)
        checkAndUnlock(Achievement.TWENTY_LINKS, linkCount >= 20, newAchievements)
        checkAndUnlock(Achievement.TEN_AI_CONFIRMS, aiConfirmCount >= 10, newAchievements)

        return newAchievements
    }

    fun isUnlocked(achievement: Achievement): Boolean = prefs.getBoolean(achievement.id, false)
    fun getAllUnlocked(): List<Achievement> = Achievement.entries.filter { isUnlocked(it) }

    private fun checkAndUnlock(a: Achievement, condition: Boolean, newList: MutableList<Achievement>) {
        if (condition && !isUnlocked(a)) {
            prefs.edit().putBoolean(a.id, true).apply()
            newList.add(a)
        }
    }
}
```

### 2.3 成就解锁通知

```kotlin
@Composable
fun AchievementUnlockNotification(achievement: Achievement) {
    var visible by remember { mutableStateOf(true) }

    LaunchedEffect(achievement) { delay(3000); visible = false }

    AnimatedVisibility(visible = visible, enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(), exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut()) {
        Surface(modifier = Modifier.fillMaxWidth().padding(16.dp), shape = RoundedCornerShape(16.dp), color = SurfaceDark, tonalElevation = 8.dp, shadowElevation = 8.dp) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(achievement.emoji, fontSize = 32.sp)
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("🏆 成就解锁！", color = Color(0xFFFFD700), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text(achievement.title, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    Text(achievement.description, color = TextSecondary, fontSize = 13.sp)
                    Text("奖励：${achievement.reward}", color = PrimaryBlue, fontSize = 12.sp)
                }
            }
        }
    }
}
```

---

## 3. 统计面板

### 3.1 统计数据模型

```kotlin
data class StatisticsData(
    val totalNodes: Int = 0,
    val abilityNodes: Int = 0,
    val resourceNodes: Int = 0,
    val totalLinks: Int = 0,
    val manualLinks: Int = 0,
    val aiSuggestedLinks: Int = 0,
    val maxDepth: Int = 0,
    val levelDistribution: Map<Int, Int> = emptyMap(),
    val unlockedAchievements: List<Achievement> = emptyList()
)
```

### 3.2 统计面板 UI 布局

```
┌─────────────────────────────────────┐
│  ← 返回          统计面板           │
├─────────────────────────────────────┤
│  ┌─────────────┐ ┌─────────────┐    │
│  │  总节点      │ │  总链接      │    │
│  │   12        │ │   5         │    │
│  │  ⚔️8  💎4   │ │  🔗3  🤖2   │    │
│  └─────────────┘ └─────────────┘    │
│                                     │
│  📊 节点等级分布                     │
│  ┌─────────────────────────────┐    │
│  │ Lv.1 🌱 ████████ 5          │    │
│  │ Lv.2 🌿 ████ 3              │    │
│  │ Lv.3 🌳 ██ 2                │    │
│  │ Lv.4 ⭐ ██ 1                │    │
│  │ Lv.5 👑 █ 1                 │    │
│  └─────────────────────────────┘    │
│                                     │
│  🏆 成就                            │
│  ┌─────────────────────────────┐    │
│  │ 🌱 初出茅庐 ✅              │    │
│  │ 🌿 枝繁叶茂 ✅              │    │
│  │ 🌳 技能大师 🔒              │    │
│  │ 🔗 链接达人 🔒              │    │
│  └─────────────────────────────┘    │
└─────────────────────────────────────┘
```

### 3.3 统计卡片组件

```kotlin
@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    subtitle: String? = null,
    color: Color = PrimaryBlue
) {
    Surface(modifier = modifier, shape = RoundedCornerShape(12.dp), color = SurfaceDark) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, color = TextSecondary, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, color = color, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            if (subtitle != null) Text(subtitle, color = TextSecondary, fontSize = 12.sp)
        }
    }
}

@Composable
private fun LevelDistributionChart(distribution: Map<Int, Int>, modifier: Modifier = Modifier) {
    val maxCount = distribution.values.maxOrNull() ?: 1
    Surface(modifier = modifier, shape = RoundedCornerShape(12.dp), color = SurfaceDark) {
        Column(modifier = Modifier.padding(16.dp)) {
            for (level in 1..5) {
                val count = distribution[level] ?: 0
                val config = NodeLevelConfig.forLevel(level)
                val barWidth = if (maxCount > 0) (count.toFloat() / maxCount) else 0f
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Lv.$level ${config.emoji}", color = TextSecondary, fontSize = 13.sp, modifier = Modifier.width(80.dp))
                    Box(modifier = Modifier.weight(1f).height(20.dp).clip(RoundedCornerShape(4.dp)).background(SurfaceDark)) {
                        Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(barWidth).clip(RoundedCornerShape(4.dp)).background(Color(config.glowColor)))
                    }
                    Text("$count", color = TextPrimary, fontSize = 13.sp, modifier = Modifier.width(24.dp), textAlign = TextAlign.End)
                }
            }
        }
    }
}
```

---

## 4. 导航集成

在导航栏中接入统计面板：

```kotlin
enum class AppDestinations(val label: String, val icon: Int) {
    HOME("技能树", R.drawable.ic_home),
    STATISTICS("统计", R.drawable.ic_favorite),   // 替换 Favorites
    SETTINGS("设置", R.drawable.ic_account_box),   // 替换 Profile
}
```

---

## 5. 实施步骤

| 步骤 | 内容 |
|------|------|
| 1 | 创建 `NodeLevelConfig` 等级配置数据类 |
| 2 | 创建 `CalculateNodeLevelUseCase` 等级计算 |
| 3 | 实现 Canvas 中的等级光效绘制 |
| 4 | 创建 `Achievement` 枚举定义 |
| 5 | 创建 `CheckAchievementsUseCase` 成就检查 |
| 6 | 创建 `AchievementUnlockNotification` 通知组件 |
| 7 | 在 `SkillTreeApp` 中集成成就检查 |
| 8 | 创建 `StatisticsData` 统计模型 |
| 9 | 创建 `StatisticsViewModel` |
| 10 | 创建 `StatisticsScreen` 统计面板页面 |
| 11 | 在导航栏中接入统计面板 |