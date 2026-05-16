# 深层硬编码中文 i18n 修复提示词

## 项目背景

Skill-Tree 是一个 Kotlin / Jetpack Compose / Hilt / Room 技术栈的 Android 应用。第一轮 i18n 修复了 85 处 UI 文本硬编码，但还有 77 处深层硬编码中文未处理，包括异常消息、成就名称、等级名称、UseCase 验证消息等。这些在英文模式下仍会显示中文。

---

## 修复策略

深层硬编码与 UI 文本不同，很多不在 Composable 上下文中，无法直接使用 `stringResource`。需要按类型分别处理：

| 类型 | 位置 | 处理方式 |
|------|------|---------|
| 异常消息 | DomainException / DataException | 保留中文 message（仅开发日志用），ErrorStateManager 映射时已使用 @StringRes |
| 成就标题/描述 | Achievement 枚举 | 改为 @StringRes 引用 |
| 等级名称 | NodeLevelConfig | 改为 @StringRes 引用 |
| UseCase 验证消息 | CreateNodeUseCase 等 | 改为 @StringRes 引用 |
| UI 无障碍/对话框 | 多个 Screen 文件 | 改为 stringResource |
| 示例数据 | 3 处重复 | 改为 stringResource |
| 导出 Markdown | ExportToMarkdownUseCase | 改为 Context.getString |

---

## Part A: Achievement 枚举改造

### 当前代码（NodeLevelConfig.kt 第 52-69 行）

```kotlin
enum class Achievement(
    val id: String,
    val emoji: String,
    val title: String,
    val description: String,
    val reward: String
) {
    FIRST_NODE("first_node", "🌱", "初出茅庐", "创建第一个节点", "解锁自定义图标"),
    TEN_NODES("ten_nodes", "🌿", "枝繁叶茂", "拥有 10 个节点", "解锁自定义主题色"),
    ...
}
```

### 修改方案

将 `title`、`description`、`reward` 从 `String` 改为 `@StringRes Int`：

```kotlin
enum class Achievement(
    val id: String,
    val emoji: String,
    @StringRes val titleResId: Int,
    @StringRes val descriptionResId: Int,
    @StringRes val rewardResId: Int
) {
    FIRST_NODE("first_node", "🌱", R.string.achievement_first_node_title, R.string.achievement_first_node_desc, R.string.achievement_first_node_reward),
    TEN_NODES("ten_nodes", "🌿", R.string.achievement_ten_nodes_title, R.string.achievement_ten_nodes_desc, R.string.achievement_ten_nodes_reward),
    FIFTY_NODES("fifty_nodes", "🌳", R.string.achievement_fifty_nodes_title, R.string.achievement_fifty_nodes_desc, R.string.achievement_fifty_nodes_reward),
    TWENTY_LINKS("twenty_links", "🔗", R.string.achievement_twenty_links_title, R.string.achievement_twenty_links_desc, R.string.achievement_twenty_links_reward),
    TEN_AI_CONFIRMS("ten_ai_confirms", "🤖", R.string.achievement_ten_ai_confirms_title, R.string.achievement_ten_ai_confirms_desc, R.string.achievement_ten_ai_confirms_reward),
    FIVE_LEVEL5("five_level5", "✍️", R.string.achievement_five_level5_title, R.string.achievement_five_level5_desc, R.string.achievement_five_level5_reward),
    TEN_VOICE("ten_voice", "🎤", R.string.achievement_ten_voice_title, R.string.achievement_ten_voice_desc, R.string.achievement_ten_voice_reward),
    TWENTY_IMAGES("twenty_images", "📸", R.string.achievement_twenty_images_title, R.string.achievement_twenty_images_desc, R.string.achievement_twenty_images_reward),
    DAILY_STREAK_7("daily_7", "🔥", R.string.achievement_daily_streak_title, R.string.achievement_daily_streak_desc, R.string.achievement_daily_streak_reward),
    TREE_DEPTH_5("depth_5", "🏔️", R.string.achievement_tree_depth5_title, R.string.achievement_tree_depth5_desc, R.string.achievement_tree_depth5_reward)
}
```

### 新增 string resource

**values/strings.xml**:
```xml
<!-- Achievement Titles -->
<string name="achievement_first_node_title">First Step</string>
<string name="achievement_ten_nodes_title">Branching Out</string>
<string name="achievement_fifty_nodes_title">Skill Master</string>
<string name="achievement_twenty_links_title">Link Master</string>
<string name="achievement_ten_ai_confirms_title">AI Pioneer</string>
<string name="achievement_five_level5_title">Content King</string>
<string name="achievement_ten_voice_title">Voice Recorder</string>
<string name="achievement_twenty_images_title">Visual Storyteller</string>
<string name="achievement_daily_streak_title">Consistent</string>
<string name="achievement_tree_depth5_title">Deep Explorer</string>

<!-- Achievement Descriptions -->
<string name="achievement_first_node_desc">Create your first node</string>
<string name="achievement_ten_nodes_desc">Own 10 nodes</string>
<string name="achievement_fifty_nodes_desc">Own 50 nodes</string>
<string name="achievement_twenty_links_desc">Create 20 links</string>
<string name="achievement_ten_ai_confirms_desc">Confirm 10 AI-suggested links</string>
<string name="achievement_five_level5_desc">Own 5 Lv.5 nodes</string>
<string name="achievement_ten_voice_desc">Use voice input 10 times</string>
<string name="achievement_twenty_images_desc">Insert 20 images</string>
<string name="achievement_daily_streak_desc">Create or edit nodes 7 days in a row</string>
<string name="achievement_tree_depth5_desc">Reach tree depth of 5 levels</string>

<!-- Achievement Rewards -->
<string name="achievement_first_node_reward">Custom icon</string>
<string name="achievement_ten_nodes_reward">Custom theme color</string>
<string name="achievement_fifty_nodes_reward">Special trunk style</string>
<string name="achievement_twenty_links_reward">Special link style</string>
<string name="achievement_ten_ai_confirms_reward">AI glow effect</string>
<string name="achievement_five_level5_reward">Gold theme</string>
<string name="achievement_ten_voice_reward">Voice wave animation</string>
<string name="achievement_twenty_images_reward">Photo wall mode</string>
<string name="achievement_daily_streak_reward">Streak badge</string>
<string name="achievement_tree_depth5_reward">Deep Explorer title</string>
```

**values-zh/strings.xml**:
```xml
<string name="achievement_first_node_title">初出茅庐</string>
<string name="achievement_ten_nodes_title">枝繁叶茂</string>
<string name="achievement_fifty_nodes_title">技能大师</string>
<string name="achievement_twenty_links_title">链接达人</string>
<string name="achievement_ten_ai_confirms_title">AI 先驱</string>
<string name="achievement_five_level5_title">内容王者</string>
<string name="achievement_ten_voice_title">语音记录者</string>
<string name="achievement_twenty_images_title">图文并茂</string>
<string name="achievement_daily_streak_title">连续记录</string>
<string name="achievement_tree_depth5_title">深不可测</string>

<string name="achievement_first_node_desc">创建第一个节点</string>
<string name="achievement_ten_nodes_desc">拥有 10 个节点</string>
<string name="achievement_fifty_nodes_desc">拥有 50 个节点</string>
<string name="achievement_twenty_links_desc">创建 20 条链接</string>
<string name="achievement_ten_ai_confirms_desc">确认 10 条 AI 推荐链接</string>
<string name="achievement_five_level5_desc">拥有 5 个 Lv.5 节点</string>
<string name="achievement_ten_voice_desc">使用语音输入 10 次</string>
<string name="achievement_twenty_images_desc">插入 20 张图片</string>
<string name="achievement_daily_streak_desc">连续 7 天创建/编辑节点</string>
<string name="achievement_tree_depth5_desc">技能树深度达到 5 层</string>

<string name="achievement_first_node_reward">解锁自定义图标</string>
<string name="achievement_ten_nodes_reward">解锁自定义主题色</string>
<string name="achievement_fifty_nodes_reward">解锁特殊树干样式</string>
<string name="achievement_twenty_links_reward">解锁特殊连线样式</string>
<string name="achievement_ten_ai_confirms_reward">解锁 AI 专属光效</string>
<string name="achievement_five_level5_reward">解锁金色主题</string>
<string name="achievement_ten_voice_reward">解锁语音波形动画</string>
<string name="achievement_twenty_images_reward">解锁图片墙展示模式</string>
<string name="achievement_daily_streak_reward">解锁连续天数徽章</string>
<string name="achievement_tree_depth5_reward">解锁深层探索者称号</string>
```

### 调用方修改

所有引用 `achievement.title` / `achievement.description` / `achievement.reward` 的地方需改为 `stringResource(achievement.titleResId)` 或 `context.getString(achievement.titleResId)`。

涉及文件：
- `StatisticsScreen.kt` — 成就列表展示
- `SkillTreeScreen.kt` — 成就解锁弹窗（第 1343、1360 行）
- `CheckAchievementsUseCaseTest.kt` — 测试中引用 achievement.title 的地方

---

## Part B: NodeLevelConfig 等级名称改造

### 当前代码（NodeLevelConfig.kt 第 21-25 行）

```kotlin
5 -> NodeLevelConfig(5, "👑", "大师", 0x80FFD700, 20f, 1.3f, 3f)
4 -> NodeLevelConfig(4, "⭐", "专家", 0x8058A6FF, 15f, 1.15f, 2.5f)
3 -> NodeLevelConfig(3, "🌳", "熟练", 0x403FB950, 10f, 1.0f, 2f)
2 -> NodeLevelConfig(2, "🌿", "入门", 0x208B949E, 5f, 0.9f, 1.5f)
else -> NodeLevelConfig(1, "🌱", "新手", 0x00000000, 0f, 0.8f, 1f)
```

### 修改方案

将 `label: String` 改为 `@StringRes labelResId: Int`：

```kotlin
data class NodeLevelConfig(
    val level: Int,
    val emoji: String,
    @StringRes val labelResId: Int,
    val glowColor: Long,
    val glowRadius: Float,
    val sizeMultiplier: Float,
    val borderWidth: Float
)

companion object {
    fun forLevel(level: Int): NodeLevelConfig = when (level) {
        5 -> NodeLevelConfig(5, "👑", R.string.level_master, 0x80FFD700, 20f, 1.3f, 3f)
        4 -> NodeLevelConfig(4, "⭐", R.string.level_expert, 0x8058A6FF, 15f, 1.15f, 2.5f)
        3 -> NodeLevelConfig(3, "🌳", R.string.level_proficient, 0x403FB950, 10f, 1.0f, 2f)
        2 -> NodeLevelConfig(2, "🌿", R.string.level_beginner, 0x208B949E, 5f, 0.9f, 1.5f)
        else -> NodeLevelConfig(1, "🌱", R.string.level_novice, 0x00000000, 0f, 0.8f, 1f)
    }
}
```

### 新增 string resource

**values/strings.xml**:
```xml
<string name="level_master">Master</string>
<string name="level_expert">Expert</string>
<string name="level_proficient">Proficient</string>
<string name="level_beginner">Beginner</string>
<string name="level_novice">Novice</string>
```

**values-zh/strings.xml**:
```xml
<string name="level_master">大师</string>
<string name="level_expert">专家</string>
<string name="level_proficient">熟练</string>
<string name="level_beginner">入门</string>
<string name="level_novice">新手</string>
```

### 调用方修改

搜索所有引用 `levelConfig.label` 或 `.label` 的地方，改为 `context.getString(levelConfig.labelResId)` 或 `stringResource(levelConfig.labelResId)`。

---

## Part C: DomainException / DataException 异常消息

### 分析

`DomainException` 和 `DataException` 的 `message` 字段主要用于：
1. 开发日志 / 调试
2. `ErrorStateManager.mapDomainException()` 映射为 `UiError`

**关键发现**：`ErrorStateManager` 已经将每种 `DomainException` 映射为 `@StringRes Int` 的 `UiError`，**不会直接将 message 显示给用户**。因此异常 message 中的中文不会出现在 UI 上。

### 修改方案

**不修改 DomainException / DataException 的 message 字段**。原因：
1. 异常 message 是开发调试信息，不面向用户
2. ErrorStateManager 已通过 `@StringRes` 映射，用户看到的是 strings.xml 中的文本
3. 修改 message 为英文会影响现有测试的断言

但需要确认 `ErrorStateManager.mapDomainException()` 确实覆盖了所有 10 种 `DomainException` 和 4 种 `DataException`。如果有遗漏，需要补充映射。

---

## Part D: UseCase 验证消息

### 当前代码

1. **CreateNodeUseCase.kt 第 35 行**:
   ```kotlin
   return Outcome.Error(DomainException.ValidationError("title", "标题不能为空"))
   ```

2. **CreateTagUseCase.kt 第 27 行**:
   ```kotlin
   return Outcome.Error(DomainException.ValidationError("name", "标签名称不能为空"))
   ```

### 分析

`ValidationError` 的 `field` 和 `reason` 参数被传入 `DomainException.message`，但 `ErrorStateManager` 映射时统一使用 `R.string.error_validation`，不直接显示 reason 字段。

### 修改方案

将 reason 改为英文（因为是开发调试信息），不影响用户界面：

```kotlin
// CreateNodeUseCase.kt
return Outcome.Error(DomainException.ValidationError("title", "Title cannot be empty"))

// CreateTagUseCase.kt
return Outcome.Error(DomainException.ValidationError("name", "Tag name cannot be empty"))
```

---

## Part E: UI 无障碍/对话框残留硬编码

### 需要修复的文件

1. **NodeDetailScreen.kt**:
   - 第 298 行: `contentDescription = "删除操作"` → `stringResource(R.string.cd_delete_action)`
   - 第 481 行: `accessibilityLabel("节点内容")` → `.accessibilityLabel(stringResource(R.string.a11y_node_content))`
   - 第 522 行: `accessibilityLabel("Markdown 内容")` → `.accessibilityLabel(stringResource(R.string.a11y_markdown_content))`

2. **StatisticsScreen.kt**:
   - 第 242 行: `"已解锁"/"未解锁"` → `stringResource(R.string.a11y_unlocked)/stringResource(R.string.a11y_locked)`
   - 第 339 行: `"能力"/"物质"/"总计"` → `stringResource(R.string.stats_ability_label)/stringResource(R.string.stats_resource_label)/stringResource(R.string.stats_total_label)`

3. **TagChip.kt**:
   - 第 62 行: `"标签："/"点击移除"` → `stringResource(R.string.a11y_tag_label, tag.name)/stringResource(R.string.a11y_tag_tap_remove)`

4. **SettingsScreen.kt**:
   - 第 129 行: `"危险操作"` → `stringResource(R.string.settings_danger_zone)`

5. **AddAttachmentDialog.kt**:
   - 第 48 行: `"添加附件"` → `stringResource(R.string.attachment_add_title)`（已存在）
   - 第 55-57 行: `"拍照"/"从相册选择"/"选择文件"` → `stringResource(R.string.attachment_take_photo)/stringResource(R.string.attachment_from_gallery)/stringResource(R.string.attachment_from_file)`（均已存在）
   - 第 64 行: `"取消"` → `stringResource(R.string.common_cancel)`（已存在）

6. **AccessibilityExt.kt**:
   - 第 54-55 行: 默认参数 `"已展开"/"已折叠"` → 改为 `@StringRes` 参数或移除默认值
   - 第 76-77 行: 默认参数 `"已开启"/"已关闭"` → 同上

### AccessibilityExt 默认参数处理

当前 `accessibilityExpandable` 和 `accessibilityToggle` 的默认参数是中文，在英文环境下会出问题。修改方案：

```kotlin
fun Modifier.accessibilityExpandable(
    isExpanded: Boolean,
    label: String,
    expandedLabel: String? = null,
    collapsedLabel: String? = null
): Modifier {
    return this.semantics(mergeDescendants = true) {
        contentDescription = label
        stateDescription = if (isExpanded) expandedLabel ?: label else collapsedLabel ?: label
    }
}

fun Modifier.accessibilityToggle(
    isChecked: Boolean,
    label: String,
    checkedLabel: String? = null,
    uncheckedLabel: String? = null
): Modifier {
    return this.semantics(mergeDescendants = true) {
        contentDescription = label
        role = Role.Switch
        stateDescription = if (isChecked) checkedLabel ?: label else uncheckedLabel ?: label
    }
}
```

调用方需显式传入 expandedLabel/collapsedLabel/checkedLabel/uncheckedLabel。

### 新增 string resource

**values/strings.xml**:
```xml
<string name="cd_delete_action">Delete</string>
<string name="a11y_node_content">Node content</string>
<string name="a11y_markdown_content">Markdown content</string>
<string name="a11y_unlocked">Unlocked</string>
<string name="a11y_locked">Locked</string>
<string name="stats_total_label">Total</string>
<string name="a11y_tag_label">Tag: %s</string>
<string name="a11y_tag_tap_remove">Tap to remove</string>
<string name="settings_danger_zone">Danger Zone</string>
<string name="a11y_expanded">Expanded</string>
<string name="a11y_collapsed">Collapsed</string>
<string name="a11y_checked">On</string>
<string name="a11y_unchecked">Off</string>
```

**values-zh/strings.xml**:
```xml
<string name="cd_delete_action">删除操作</string>
<string name="a11y_node_content">节点内容</string>
<string name="a11y_markdown_content">Markdown 内容</string>
<string name="a11y_unlocked">已解锁</string>
<string name="a11y_locked">未解锁</string>
<string name="stats_total_label">总计</string>
<string name="a11y_tag_label">标签：%s</string>
<string name="a11y_tag_tap_remove">点击移除</string>
<string name="settings_danger_zone">危险操作</string>
<string name="a11y_expanded">已展开</string>
<string name="a11y_collapsed">已折叠</string>
<string name="a11y_checked">已开启</string>
<string name="a11y_unchecked">已关闭</string>
```

---

## Part F: 导出 Markdown 标题

### 当前代码（ExportToMarkdownUseCase.kt 第 30、34 行）

```kotlin
if (nodes.isEmpty()) return "# 技能树\n\n暂无节点数据\n"
sb.appendLine("# 技能树")
```

### 修改方案

由于 `ExportToMarkdownUseCase` 不是 Composable，需要传入 `Context`：

1. 修改 `invoke()` 签名添加 `context: Context` 参数
2. 使用 `context.getString()` 获取国际化字符串

```kotlin
suspend operator fun invoke(context: Context): Outcome<String> {
    return try {
        val nodes = repository.getAllNodes().first()
        val markdown = exportToMarkdown(nodes, context)
        Outcome.Success(markdown)
    } catch (e: Exception) {
        Outcome.Error(DomainException.StorageError(e))
    }
}

private fun exportToMarkdown(nodes: List<SkillNodeEntity>, context: Context): String {
    if (nodes.isEmpty()) return "# ${context.getString(R.string.export_tree_title)}\n\n${context.getString(R.string.export_no_data)}\n"

    val rootNodes = nodes.filter { it.parentId == null }
    val sb = StringBuilder()
    sb.appendLine("# ${context.getString(R.string.export_tree_title)}")
    ...
}
```

### 新增 string resource

```xml
<string name="export_tree_title">Skill Tree</string>
<string name="export_no_data">No node data yet</string>
```

中文：
```xml
<string name="export_tree_title">技能树</string>
<string name="export_no_data">暂无节点数据</string>
```

### 调用方修改

`SettingsViewModel.exportMarkdown()` 和 `SkillTreeViewModel` 中调用 `exportToMarkdownUseCase()` 的地方需传入 `context`。

---

## Part G: 示例数据硬编码

### 涉及文件（3 处重复）

1. `LoadSampleDataUseCase.kt` — `"技能树"/"编程"/"Python"/"Kotlin"/"Web开发"/"设计"/"数据分析"` + content 中的中文
2. `SettingsViewModel.kt` — 同上，7 次 `createNodeUseCase` 调用
3. `SkillTreeViewModel.kt` — 同上

### 修改方案

**LoadSampleDataUseCase.kt**：添加 `context: Context` 参数，使用 `context.getString()`

**SettingsViewModel.kt**：改为调用 `LoadSampleDataUseCase`（消除重复），传入 `context`

**SkillTreeViewModel.kt**：同上

### 新增 string resource

```xml
<!-- Sample Data -->
<string name="sample_root_title">Skill Tree</string>
<string name="sample_programming_title">Programming</string>
<string name="sample_python_title">Python</string>
<string name="sample_kotlin_title">Kotlin</string>
<string name="sample_web_title">Web Development</string>
<string name="sample_design_title">Design</string>
<string name="sample_data_analysis_title">Data Analysis</string>
<string name="sample_python_content">## Python Learning Path\n\n- Basic Syntax\n- Data Structures\n- OOP\n- Async Programming</string>
<string name="sample_kotlin_content">## Kotlin Learning Path\n\n- Coroutines\n- Compose\n- KMP</string>
<string name="sample_design_content">## Design Resources\n\n- Figma\n- Color Theory\n- Typography</string>
```

中文：
```xml
<string name="sample_root_title">技能树</string>
<string name="sample_programming_title">编程</string>
<string name="sample_python_title">Python</string>
<string name="sample_kotlin_title">Kotlin</string>
<string name="sample_web_title">Web开发</string>
<string name="sample_design_title">设计</string>
<string name="sample_data_analysis_title">数据分析</string>
<string name="sample_python_content">## Python 学习路线\n\n- 基础语法\n- 数据结构\n- 面向对象编程\n- 异步编程</string>
<string name="sample_kotlin_content">## Kotlin 学习路线\n\n- 协程\n- Compose\n- KMP</string>
<string name="sample_design_content">## 设计资源\n\n- Figma\n- 色彩理论\n- 排版原则</string>
```

---

## Part H: 其他零散硬编码

1. **NodeDetailViewModel.kt 第 68 行**: `"nodeId 不能为空"` — 这是 `IllegalArgumentException` 的 message，仅开发时可见，改为英文：
   ```kotlin
   ?: throw IllegalArgumentException("nodeId must not be null")
   ```

2. **AttachmentFileManager.kt 第 38 行**: `"无法打开文件: $sourceUri"` — `IOException` message，仅开发日志，改为英文：
   ```kotlin
   throw IOException("Unable to open file: $sourceUri")
   ```

---

## 验证清单

1. `./gradlew assembleDebug` 编译通过
2. `./gradlew test` 全部测试通过
3. 全项目搜索源码中的硬编码中文（排除注释和 strings.xml）— 确认仅剩 DomainException/DataException 的调试 message
4. 切换到英文模式，检查统计页面成就标题/描述/奖励显示英文
5. 切换到英文模式，检查节点等级名称显示英文
6. 切换到英文模式，检查导出 Markdown 标题显示英文
7. 切换到英文模式，检查示例数据标题显示英文
