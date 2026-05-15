# 国际化 i18n 实现提示词

## 项目背景

Skill-Tree 项目当前所有文本硬编码为中文，需要建立 i18n 基础架构，MVP 阶段支持中/英双语。

**重要**: 请先阅读项目规则 `CLAUDE.md` 和国际化方案 `prd/06-i18n-strategy.md`。

---

## 当前代码现状

- `strings.xml` 仅有 `app_name` 一项
- 所有 UI 文本硬编码在 Kotlin 代码中
- 无 `values-zh/` 目录
- 无 `LocaleManager`
- ErrorStateManager 中的错误消息硬编码中文
- DomainException 的消息硬编码中文

---

## 实现任务

### 任务 1：创建英文默认 strings.xml

**文件**: `app/src/main/res/values/strings.xml`

按照 PRD 06 中第 3.1 节的完整内容创建。包含以下分类：
- 导航（3项）
- 技能树主页（8项）
- 节点详情（12项）
- 创建节点（7项）
- 标签（6项）
- 设置（16项）
- 通用（9项）
- 成就（新增）
- 错误消息（新增，来自 ErrorStateManager 和 DomainException）

**新增成就相关字符串**:
```xml
<string name="achievement_unlocked">Achievement Unlocked!</string>
<string name="achievement_first_node">First Step - Created your first skill node</string>
<string name="achievement_ten_nodes">Skill Collector - Created 10 skill nodes</string>
<string name="achievement_fifty_nodes">Skill Master - Created 50 skill nodes</string>
<string name="achievement_twenty_links">Network Builder - Created 20 links</string>
<string name="achievement_five_level5">Expert - 5 nodes reached level 5</string>
<string name="achievement_tree_depth5">Deep Thinker - Tree depth reached 5 levels</string>
```

**新增错误消息字符串**:
```xml
<string name="error_node_not_found">Node not found</string>
<string name="error_parent_not_found">Parent node not found</string>
<string name="error_circular_reference">Cannot move node - would create circular reference</string>
<string name="error_max_children">Maximum child nodes reached</string>
<string name="error_storage">Storage error - please check device storage</string>
<string name="error_validation">Validation failed - please check your input</string>
<string name="error_invalid_node_type">Invalid node type</string>
<string name="error_tag_exists">Tag already exists</string>
<string name="error_link_exists">Link already exists</string>
<string name="error_self_link">Cannot link node to itself</string>
<string name="error_database">Database error - please retry</string>
<string name="error_file_not_found">File not found</string>
<string name="error_storage_full">Storage space insufficient</string>
<string name="error_io">File read/write error</string>
<string name="error_unknown">Unknown error</string>
```

### 任务 2：创建中文 strings.xml

**文件**: `app/src/main/res/values-zh/strings.xml`

与英文版一一对应，所有文本翻译为简体中文。按照 PRD 06 中第 3.2 节的内容创建。

### 任务 3：创建 LocaleManager

**新增文件**: `app/src/main/java/com/fancy/skill_tree/core/data/preferences/LocaleManager.kt`

```kotlin
@Singleton
class LocaleManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences("locale", Context.MODE_PRIVATE)

    var languageTag: String?
        get() = prefs.getString(KEY_LANGUAGE, null)
        set(value) { prefs.edit().putString(KEY_LANGUAGE, value).apply() }

    fun getCurrentLocale(): Locale {
        val tag = languageTag
        return if (tag != null) Locale.forLanguageTag(tag) else Locale.getDefault()
    }

    fun applyLocale(baseContext: Context): Context {
        val locale = getCurrentLocale()
        Locale.setDefault(locale)
        val config = Configuration(baseContext.resources.configuration)
        config.setLocale(locale)
        return baseContext.createConfigurationContext(config)
    }

    companion object {
        private const val KEY_LANGUAGE = "app_language"
        const val LANG_ZH = "zh"
        const val LANG_EN = "en"
        const val LANG_SYSTEM = null
    }
}
```

### 任务 4：在 SkillTreeApplication 中集成语言切换

**文件**: `app/src/main/java/com/fancy/skill_tree/SkillTreeApplication.kt`

```kotlin
@HiltAndroidApp
class SkillTreeApplication : Application() {

    @Inject lateinit var localeManager: LocaleManager

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base?.let { localeManager.applyLocale(it) })
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        localeManager.applyLocale(this)
    }
}
```

### 任务 5：在 SettingsScreen 中添加语言切换 UI

**文件**: `app/src/main/java/com/fancy/skill_tree/feature/settings/SettingsScreen.kt`

在外观设置区域添加语言选择器：
- 三个选项：中文 / English / 跟随系统
- 选择后保存到 `LocaleManager` 并重建 Activity

```kotlin
// 语言切换后需要重建 Activity 才能生效
fun setLanguage(languageTag: String?) {
    localeManager.languageTag = languageTag
    // 重建 Activity
    (context as? Activity)?.recreate()
}
```

### 任务 6：迁移 SkillTreeScreen 中的硬编码文本

**文件**: `app/src/main/java/com/fancy/skill_tree/feature/tree/SkillTreeScreen.kt`

迁移要点：
- `"搜索节点..."` → `stringResource(R.string.tree_search_hint)`
- `"点击目标节点放置，点击空白取消"` → `stringResource(R.string.tree_drag_hint)`
- `"未找到匹配的节点"` → `stringResource(R.string.tree_no_results)`
- `"欢迎来到技能树"` → `stringResource(R.string.tree_empty_title)`
- `"开始构建你的技能知识图谱"` → `stringResource(R.string.tree_empty_description)`
- `"加载示例数据"` → `stringResource(R.string.tree_load_sample)`
- `"创建第一个节点"` → `stringResource(R.string.tree_create_first)`
- Canvas 绘制中的文字（节点标题、类型标签）不需要国际化（来自数据库）

**注意**: Canvas 中 `drawText` 不能使用 `stringResource`。需要在 Composable 层获取字符串后传入绘制函数。

### 任务 7：迁移 NodeDetailScreen 和 CreateNodeDialog

**文件**: `feature/node/NodeDetailScreen.kt`, `feature/tree/CreateNodeDialog.kt`

迁移要点：
- 所有按钮文字："编辑"、"查看"、"保存"、"取消"、"删除节点" 等
- 所有提示文字："确定要删除此节点吗？"、"暂无内容" 等
- 所有标签文字："标签"、"关联节点"、"附件" 等
- CreateNodeDialog："新建节点"、"节点标题"、"节点类型"、"创建" 等

### 任务 8：迁移 SettingsScreen 和 StatisticsScreen

**文件**: `feature/settings/SettingsScreen.kt`, `feature/statistics/StatisticsScreen.kt`

迁移要点：
- SettingsScreen：所有设置项标题和描述
- StatisticsScreen：统计卡片标题、成就名称

### 任务 9：迁移 ErrorStateManager 和 DomainException 的错误消息

**文件**: `core/ui/error/ErrorStateManager.kt`

ErrorStateManager 中的 `mapDomainException` 和 `mapDataException` 方法返回的 `UiError` 包含硬编码中文标题和消息。需要改为使用 Context 获取字符串资源。

改造方式：
1. ErrorStateManager 不再直接硬编码消息，而是返回 string resource ID
2. 或者 ErrorStateManager 接受 Context 参数，使用 `context.getString()`

```kotlin
// 推荐方案：UiError 使用 string res ID
data class UiError(
    val titleResId: Int,
    val messageResId: Int,
    val messageArgs: Array<Any> = emptyArray(),
    val action: ErrorAction? = null,
    val severity: ErrorSeverity = ErrorSeverity.ERROR
)
```

**注意**: 这需要同步修改 MainActivity 中 Snackbar/Dialog 的消息展示逻辑，从 `uiError.message` 改为 `context.getString(uiError.messageResId, *uiError.messageArgs)`。

### 任务 10：迁移 OnboardingScreen

**文件**: `feature/onboarding/OnboardingScreen.kt`

迁移所有引导页文本。

---

## 实现优先级

1. **P0**: 任务 1-4（strings.xml + LocaleManager + Application 集成）
2. **P0**: 任务 6-8（核心 Screen 文本迁移）
3. **P1**: 任务 5（SettingsScreen 语言切换 UI）
4. **P1**: 任务 9（ErrorStateManager 国际化）
5. **P2**: 任务 10（OnboardingScreen 迁移）

---

## 编码规范提醒

- 默认 `values/strings.xml` 为英文（Android 标准：默认资源为最通用语言）
- `values-zh/strings.xml` 为中文
- Composable 中使用 `stringResource(R.string.xxx)`
- 非 Composable 中使用 `context.getString(R.string.xxx)`
- 带参数的字符串使用 `%s`、`%d` 占位符
- KDoc 注释保持中文（开发者面向）

---

## 实现完成后

1. 运行 `./gradlew assembleDebug` 确保编译通过
2. 运行 `./gradlew test` 确保测试通过
3. 将本次实现内容总结写入 `doc/rep/2026-05-15-国际化i18n.md`
