# 收尾打磨提示词

## 项目背景

Skill-Tree 是一个 Kotlin / Jetpack Compose / Hilt / Room 技术栈的 Android 应用，采用 Clean Architecture + MVVM。项目已完成所有核心功能和 P1 修复，当前完成度约 95%。本次收尾打磨包含 5 个 P2 级任务，按优先级排序。

---

## Task 1: 残留硬编码中文修复 + 冗余常量清理

### 1.1 SkillTreeScreen.kt 第 273 行

**当前代码**:
```kotlin
text = uiState.errorMessage ?: "未知错误",
```

**修改为**:
```kotlin
text = uiState.errorMessage ?: stringResource(R.string.common_unknown_error),
```

说明：此处已在 Composable 上下文中，`stringResource` 可直接使用。`R.string.common_unknown_error` 已在 `values/strings.xml` 中定义（英文: "Unknown error"，中文: "未知错误"）。

### 1.2 SkillTreeScreen.kt 第 1004 行

**当前代码**:
```kotlin
viewModel.showExportError(e.message ?: "未知错误")
```

**修改为**:
```kotlin
viewModel.showExportError(e.message ?: context.getString(R.string.common_unknown_error))
```

说明：此处在 `exportMarkdown` 函数中，不是 Composable 上下文，需使用 `context.getString()` 获取国际化字符串。需确认 `exportMarkdown` 函数签名中 `context` 参数是否可用，如果不可用需要添加 `context: Context` 参数。

### 1.3 AnimationConfig.kt — 移除未使用的 ZOOM_BOUNCE_DURATION 常量

**当前代码**:
```kotlin
const val ZOOM_BOUNCE_DURATION = 200
```

**修改为**: 删除该行。实际回弹动画使用 `ZOOM_BOUNCE_SPRING`（SpringSpec），此常量在代码中未被任何地方引用。

---

## Task 2: fallbackToDestructiveMigration 替换为正式 Migration 策略

### 问题描述

当前 `DatabaseModule.kt` 第 40 行使用 `fallbackToDestructiveMigration()`，这意味着数据库版本升级时会**删除所有用户数据**。生产环境发布前必须替换为正式的 Migration 策略。

### 当前代码

**DatabaseModule.kt** (第 33-41 行):
```kotlin
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

**AppDatabase.kt**:
```kotlin
@Database(
    entities = [
        SkillNodeEntity::class,
        NodeLinkEntity::class,
        TagEntity::class,
        NodeTagCrossRef::class,
        AttachmentEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun skillNodeDao(): SkillNodeDao
    abstract fun nodeLinkDao(): NodeLinkDao
    abstract fun tagDao(): TagDao
    abstract fun nodeTagDao(): NodeTagDao
    abstract fun attachmentDao(): AttachmentDao

    companion object {
        const val DATABASE_NAME = "skill_tree.db"
    }
}
```

### 实现方案

1. 在 `core/data/database/` 目录下创建 `MigrationManager.kt`：

```kotlin
package com.fancy.skill_tree.core.data.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * 数据库迁移管理器
 * 集中管理所有版本的数据库迁移脚本
 */
object MigrationManager {

    /**
     * 所有已注册的迁移脚本
     * 每次数据库版本升级时，在此列表中添加对应的 Migration
     */
    val ALL_MIGRATIONS: List<Migration>
        get() = listOf(
            // 示例：从版本 1 迁移到版本 2
            // MIGRATION_1_2,
        )

    /**
     * 数据库迁移示例（版本 1 → 2）
     * 当需要升级数据库时，取消注释并修改 SQL 语句
     *
     * @see MIGRATION_1_2
     */
    // val MIGRATION_1_2 = object : Migration(1, 2) {
    //     override fun migrate(db: SupportSQLiteDatabase) {
    //         // 示例：添加新列
    //         // db.execSQL("ALTER TABLE skill_node ADD COLUMN newColumn TEXT DEFAULT ''")
    //     }
    // }
}
```

2. 修改 `DatabaseModule.kt`：

```kotlin
return Room.databaseBuilder(
    context,
    AppDatabase::class.java,
    AppDatabase.DATABASE_NAME
)
    .setJournalMode(JournalMode.WRITE_AHEAD_LOGGING)
    .setQueryExecutor(Executors.newFixedThreadPool(4))
    .addMigrations(*MigrationManager.ALL_MIGRATIONS.toTypedArray())
    // 仅在 Debug 构建中允许破坏性迁移，方便开发
    .fallbackToDestructiveMigrationFrom(*IntRange(1, AppDatabase.DATABASE_VERSION - 1).toList().toIntArray())
    .build()
```

注意：由于当前数据库版本为 1，不存在旧版本迁移场景，所以 `ALL_MIGRATIONS` 列表为空。关键改动是：
- 移除 `.fallbackToDestructiveMigration()`（无条件破坏性迁移）
- 添加 `.addMigrations()` 注册迁移脚本
- 可选：使用 `fallbackToDestructiveMigrationFrom()` 仅对特定旧版本允许破坏性迁移

3. 在 `AppDatabase` 中添加 `DATABASE_VERSION` 常量供 `MigrationManager` 引用：

```kotlin
companion object {
    const val DATABASE_NAME = "skill_tree.db"
    const val DATABASE_VERSION = 1
}
```

### 实现要求

1. 创建 `MigrationManager.kt`，包含 KDoc 注释
2. 修改 `DatabaseModule.kt`，移除 `fallbackToDestructiveMigration()`，添加 `addMigrations()`
3. 在 `AppDatabase` 中添加 `DATABASE_VERSION` 常量
4. `exportSchema = true` 保持不变（已配置），确保 schema JSON 文件正常导出
5. 修改完成后运行 `./gradlew assembleDebug` 确认编译通过

---

## Task 3: 4 个缺失成就条件检查实现

### 问题描述

`CheckAchievementsUseCase` 中有 4 个成就缺少条件检查实现，代码第 57-58 行注释"暂时跳过，后续补充"：

| 成就 | ID | 缺失条件 |
|------|-----|---------|
| `TEN_AI_CONFIRMS` | ten_ai_confirms | 确认 10 条 AI 推荐链接 |
| `TEN_VOICE` | ten_voice | 使用语音输入 10 次 |
| `TWENTY_IMAGES` | twenty_images | 插入 20 张图片 |
| `DAILY_STREAK_7` | daily_7 | 连续 7 天创建/编辑节点 |

### 当前代码

**CheckAchievementsUseCase.kt** (第 49-58 行):
```kotlin
checkAndUnlock(Achievement.FIRST_NODE, nodeCount >= 1, newAchievements)
checkAndUnlock(Achievement.TEN_NODES, nodeCount >= 10, newAchievements)
checkAndUnlock(Achievement.FIFTY_NODES, nodeCount >= 50, newAchievements)
checkAndUnlock(Achievement.TWENTY_LINKS, linkCount >= 20, newAchievements)
checkAndUnlock(Achievement.FIVE_LEVEL5, level5Count >= 5, newAchievements)
checkAndUnlock(Achievement.TREE_DEPTH_5, maxDepth >= 5, newAchievements)

// 以下成就需要额外实现：语音输入计数、AI 确认计数、图片计数、连续天数
// 暂时跳过，后续补充
```

**AchievementManager.kt** 已有方法：
- `updateLastActivityDate(dateString: String)` — 更新最后活动日期
- `getLastActivityDate(): String?` — 获取最后活动日期

**NodeLinkDao** 已有查询：
- `getAiSuggestedLinkCount(): Flow<Int>` — AI 推荐链接数量

**AttachmentDao** 已有查询：
- `getAttachmentsByMimeType(mimePrefix: String): Flow<List<AttachmentEntity>>` — 按 MIME 类型查询附件

### 实现方案

#### 3.1 TEN_AI_CONFIRMS — 确认 10 条 AI 推荐链接

在 `SkillTreeRepository` 中添加方法：
```kotlin
suspend fun getConfirmedAiLinkCount(): Int
```

在 `SkillTreeRepositoryImpl` 中实现：
```kotlin
override suspend fun getConfirmedAiLinkCount(): Int {
    return safeDbCall {
        nodeLinkDao.getLinksByTypeAndConfirmation("AI_SUGGESTED", true).first().size
    }.getOrDefault(0)
}
```

需要在 `NodeLinkDao` 中添加查询：
```kotlin
@Query("SELECT COUNT(*) FROM node_link WHERE linkType = 'AI_SUGGESTED' AND confirmed = 1")
fun getConfirmedAiLinkCount(): Flow<Int>
```

在 `CheckAchievementsUseCase` 中：
```kotlin
val confirmedAiLinks = repository.getConfirmedAiLinkCount()
checkAndUnlock(Achievement.TEN_AI_CONFIRMS, confirmedAiLinks >= 10, newAchievements)
```

#### 3.2 TEN_VOICE — 使用语音输入 10 次

在 `AchievementManager` 中添加计数器：
```kotlin
private const val KEY_VOICE_INPUT_COUNT = "voice_input_count"

fun incrementVoiceInputCount() {
    val count = prefs.getInt(KEY_VOICE_INPUT_COUNT, 0)
    prefs.edit().putInt(KEY_VOICE_INPUT_COUNT, count + 1).apply()
}

fun getVoiceInputCount(): Int = prefs.getInt(KEY_VOICE_INPUT_COUNT, 0)
```

在 `CheckAchievementsUseCase` 中：
```kotlin
val voiceCount = achievementManager.getVoiceInputCount()
checkAndUnlock(Achievement.TEN_VOICE, voiceCount >= 10, newAchievements)
```

注意：当前项目没有语音输入功能，此计数器为预留接口。在 `NodeDetailScreen` 编辑内容时，如果未来添加语音输入，需调用 `achievementManager.incrementVoiceInputCount()`。

#### 3.3 TWENTY_IMAGES — 插入 20 张图片

在 `SkillTreeRepository` 中添加方法：
```kotlin
suspend fun getImageAttachmentCount(): Int
```

在 `SkillTreeRepositoryImpl` 中实现：
```kotlin
override suspend fun getImageAttachmentCount(): Int {
    return safeDbCall {
        attachmentDao.getAttachmentsByMimeType("image/").first().size
    }.getOrDefault(0)
}
```

在 `CheckAchievementsUseCase` 中：
```kotlin
val imageCount = repository.getImageAttachmentCount()
checkAndUnlock(Achievement.TWENTY_IMAGES, imageCount >= 20, newAchievements)
```

#### 3.4 DAILY_STREAK_7 — 连续 7 天创建/编辑节点

在 `AchievementManager` 中添加连续天数计算：
```kotlin
private const val KEY_DAILY_STREAK = "daily_streak"

fun updateDailyStreak(): Int {
    val today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
    val lastDate = prefs.getString(KEY_LAST_ACTIVITY_DATE, null)
    
    val streak = if (lastDate == null) {
        1
    } else {
        val lastActivity = LocalDate.parse(lastDate, DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        val daysDiff = ChronoUnit.DAYS.between(lastActivity, LocalDate.now())
        when {
            daysDiff == 0L -> prefs.getInt(KEY_DAILY_STREAK, 1)
            daysDiff == 1L -> (prefs.getInt(KEY_DAILY_STREAK, 0) + 1)
            else -> 1
        }
    }
    
    prefs.edit()
        .putString(KEY_LAST_ACTIVITY_DATE, today)
        .putInt(KEY_DAILY_STREAK, streak)
        .apply()
    
    return streak
}

fun getDailyStreak(): Int = prefs.getInt(KEY_DAILY_STREAK, 0)
```

在 `CheckAchievementsUseCase` 中：
```kotlin
val dailyStreak = achievementManager.updateDailyStreak()
checkAndUnlock(Achievement.DAILY_STREAK_7, dailyStreak >= 7, newAchievements)
```

### 实现要求

1. 在 `NodeLinkDao` 中添加 `getConfirmedAiLinkCount()` 查询
2. 在 `SkillTreeRepository` / `SkillTreeRepositoryImpl` 中添加 `getConfirmedAiLinkCount()` 和 `getImageAttachmentCount()` 方法
3. 在 `AchievementManager` 中添加语音输入计数器和连续天数计算
4. 在 `CheckAchievementsUseCase` 中补全 4 个成就的条件检查
5. 所有新增方法必须有 KDoc 注释
6. 修改完成后运行 `./gradlew assembleDebug` 确认编译通过

---

## Task 4: AccessibilityOverlay 渲染数量限制

### 问题描述

当前 `AccessibilityOverlay.kt` 使用 `treeNodes.forEach` 遍历所有节点创建 Composable，没有数量上限。当节点数 >100 时，可能产生大量 Composable 导致性能问题。

### 当前代码

**AccessibilityOverlay.kt** (第 54-83 行):
```kotlin
Box(modifier = Modifier) {
    treeNodes.forEach { node ->
        val pos = positionMap[node.entity.id] ?: return@forEach
        if (visibleNodeIds.isNotEmpty() && node.entity.id !in visibleNodeIds) return@forEach
        // ... 计算 screenX, screenY, scaledWidth, scaledHeight
        if (scaledWidth <= 0f || scaledHeight <= 0f) return@forEach
        Box(modifier = Modifier
            .offset { ... }
            .size(...)
            .semantics { ... }
            .clickable { ... }
        )
    }
}
```

### 实现方案

添加最大渲染数量常量和截断逻辑：

```kotlin
private const val MAX_ACCESSIBILITY_NODES = 50

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

    // 优先渲染可见节点，超出限制时截断
    val nodesToRender = if (visibleNodeIds.isNotEmpty()) {
        treeNodes.filter { it.entity.id in visibleNodeIds }.take(MAX_ACCESSIBILITY_NODES)
    } else {
        treeNodes.take(MAX_ACCESSIBILITY_NODES)
    }

    Box(modifier = Modifier) {
        nodesToRender.forEach { node ->
            // ... 原有渲染逻辑不变
        }
    }
}
```

### 实现要求

1. 添加 `MAX_ACCESSIBILITY_NODES = 50` 常量
2. 修改 `treeNodes.forEach` 为 `nodesToRender.forEach`
3. 优先渲染 `visibleNodeIds` 中的节点
4. 超出 50 个时截断，避免 Composable 过多
5. 修改完成后运行 `./gradlew assembleDebug` 确认编译通过

---

## Task 5: SplashScreen API 集成

### 问题描述

项目尚未集成 AndroidX SplashScreen API。Android 12+ 要求所有应用提供 SplashScreen，旧版本也通过兼容库支持。

### 实现方案

1. 在 `gradle/libs.versions.toml` 中添加依赖：
```toml
[versions]
core-splashscreen = "1.0.1"

[libraries]
core-splashscreen = { group = "androidx.core", name = "core-splashscreen", version.ref = "core-splashscreen" }
```

2. 在 `app/build.gradle` 中添加依赖：
```kotlin
implementation(libs.core.splashscreen)
```

3. 修改 `MainActivity.kt`，在 `onCreate` 中安装 SplashScreen：
```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    val splashScreen = installSplashScreen()
    super.onCreate(savedInstanceState)
    // ... 原有代码
}
```

4. 在 `res/values/styles.xml` 或 `res/values/themes.xml` 中配置 SplashScreen 主题：
```xml
<style name="Theme.SkillTree.Splash" parent="Theme.SplashScreen">
    <item name="windowSplashScreenBackground">@color/backgroundDark</item>
    <item name="windowSplashScreenAnimatedIcon">@drawable/ic_launcher_foreground</item>
    <item name="postSplashScreenTheme">@style/Theme.SkillTree</item>
</style>
```

5. 在 `AndroidManifest.xml` 中将 MainActivity 的 theme 改为 SplashScreen 主题：
```xml
<activity
    android:name=".MainActivity"
    android:theme="@style/Theme.SkillTree.Splash"
    ...>
```

### 实现要求

1. 使用 `androidx.core:core-splashscreen:1.0.1` 兼容库
2. SplashScreen 背景色使用 `backgroundDark`（与暗色主题一致）
3. 使用 `ic_launcher_foreground` 作为启动图标
4. `postSplashScreenTheme` 指向应用正常主题
5. 修改完成后运行 `./gradlew assembleDebug` 确认编译通过

---

## 验证清单

完成所有修改后，请依次验证：

1. `./gradlew assembleDebug` 编译通过
2. `./gradlew test` 全部测试通过
3. 全项目搜索 `"未知错误"` — 确认无残留硬编码中文
4. 全项目搜索 `fallbackToDestructiveMigration()` — 确认已替换
5. `CheckAchievementsUseCase` 中 10/10 成就均有条件检查
6. `AccessibilityOverlay` 有 `MAX_ACCESSIBILITY_NODES` 限制
7. `MainActivity` 使用 `installSplashScreen()`
