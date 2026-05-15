# 国际化（i18n）方案

> **关联 PRD**: 无（PRD 中缺失此项）
> **当前状态**: 零实现，所有文本硬编码为中文
> **目标**: 建立 i18n 基础架构，MVP 阶段支持中/英双语

---

## 1. 策略

| 维度 | 决策 |
|------|------|
| MVP 支持语言 | 中文（简体）+ 英文 |
| 字符串管理 | Android 标准 `strings.xml` + Compose `stringResource` |
| 切换方式 | 应用内切换（不依赖系统语言） |
| 默认语言 | 跟随系统，系统不支持时回退到英文 |
| RTL 支持 | 暂不支持（中英文均为 LTR） |

---

## 2. 资源文件结构

```
app/src/main/res/
├── values/
│   └── strings.xml           # 默认语言（英文）
├── values-zh/
│   └── strings.xml           # 简体中文
└── values-zh-rTW/
    └── strings.xml           # 繁体中文（未来扩展）
```

---

## 3. strings.xml 定义

### 3.1 默认（英文）— `values/strings.xml`

```xml
<resources>
    <string name="app_name">Skill-Tree</string>

    <!-- 导航 -->
    <string name="nav_skill_tree">Skill Tree</string>
    <string name="nav_statistics">Statistics</string>
    <string name="nav_settings">Settings</string>

    <!-- 技能树主页 -->
    <string name="tree_search_hint">Search nodes...</string>
    <string name="tree_empty_title">Welcome to Skill-Tree</string>
    <string name="tree_empty_description">Start building your skill knowledge graph</string>
    <string name="tree_load_sample">Load Sample Data</string>
    <string name="tree_create_first">Create First Node</string>
    <string name="tree_export_success">Exported to %s</string>
    <string name="tree_export_failed">Export failed: %s</string>
    <string name="tree_drag_hint">Tap target node to place, tap empty to cancel</string>
    <string name="tree_no_results">No matching nodes found</string>

    <!-- 节点详情 -->
    <string name="node_detail_title">Node Detail</string>
    <string name="node_edit">Edit</string>
    <string name="node_view">View</string>
    <string name="node_save">Save</string>
    <string name="node_cancel">Cancel</string>
    <string name="node_delete">Delete Node</string>
    <string name="node_delete_confirm">Are you sure you want to delete this node? This action cannot be undone.</string>
    <string name="node_type_ability">Ability</string>
    <string name="node_type_resource">Resource</string>
    <string name="node_content_placeholder">Enter Markdown content...</string>
    <string name="node_content_empty">No content. Tap edit to add.</string>
    <string name="node_tags">Tags</string>
    <string name="node_tags_empty">No tags. Tap + to add.</string>
    <string name="node_links">Linked Nodes</string>
    <string name="node_links_empty">No linked nodes</string>
    <string name="node_ai_suggestions">AI Suggestions</string>

    <!-- 创建节点 -->
    <string name="create_node_title">Create Node</string>
    <string name="create_node_label">Node Title</string>
    <string name="create_node_type">Node Type</string>
    <string name="create_node_parent">Parent Node (optional)</string>
    <string name="create_node_no_parent">None (as root node)</string>
    <string name="create_node_button">Create</string>

    <!-- 标签 -->
    <string name="tag_add_title">Add Tags</string>
    <string name="tag_search_hint">Search or create tag...</string>
    <string name="tag_create_hint">Create tag \"%s\"</string>
    <string name="tag_existing">Existing Tags:</string>
    <string name="tag_done">Done</string>

    <!-- 设置 -->
    <string name="settings_title">Settings</string>
    <string name="settings_data_management">Data Management</string>
    <string name="settings_export_markdown">Export Markdown</string>
    <string name="settings_export_desc">Export all nodes as Markdown files</string>
    <string name="settings_load_sample">Load Sample Data</string>
    <string name="settings_load_sample_desc">Load a pre-built sample skill tree</string>
    <string name="settings_clear_data">Clear All Data</string>
    <string name="settings_clear_data_desc">Delete all nodes, tags and links</string>
    <string name="settings_clear_confirm">This will delete all nodes, tags and links. This action cannot be undone. Continue?</string>
    <string name="settings_clear_confirm_btn">Confirm Delete</string>
    <string name="settings_appearance">Appearance</string>
    <string name="settings_theme">Theme</string>
    <string name="settings_theme_dark">Dark Mode</string>
    <string name="settings_theme_light">Light Mode</string>
    <string name="settings_theme_system">Follow System</string>
    <string name="settings_content_management">Content Management</string>
    <string name="settings_tag_management">Tag Management</string>
    <string name="settings_link_management">Link Management</string>
    <string name="settings_about">About</string>
    <string name="settings_restart_onboarding">Restart Guide</string>
    <string name="settings_about_app">About Skill-Tree</string>

    <!-- 通用 -->
    <string name="common_ok">OK</string>
    <string name="common_cancel">Cancel</string>
    <string name="common_confirm">Confirm</string>
    <string name="common_retry">Retry</string>
    <string name="common_skip">Skip</string>
    <string name="common_back">Back</string>
    <string name="common_loading">Loading...</string>
    <string name="common_error">Error</string>
    <string name="common_unknown_error">Unknown error</string>
</resources>
```

### 3.2 简体中文 — `values-zh/strings.xml`

```xml
<resources>
    <string name="app_name">技能树</string>

    <string name="nav_skill_tree">技能树</string>
    <string name="nav_statistics">统计</string>
    <string name="nav_settings">设置</string>

    <string name="tree_search_hint">搜索节点...</string>
    <string name="tree_empty_title">欢迎来到技能树</string>
    <string name="tree_empty_description">开始构建你的技能知识图谱</string>
    <string name="tree_load_sample">加载示例数据</string>
    <string name="tree_create_first">创建第一个节点</string>
    <string name="tree_export_success">已导出到 %s</string>
    <string name="tree_export_failed">导出失败: %s</string>
    <string name="tree_drag_hint">点击目标节点放置，点击空白取消</string>
    <string name="tree_no_results">未找到匹配的节点</string>

    <string name="node_detail_title">节点详情</string>
    <string name="node_edit">编辑</string>
    <string name="node_view">查看</string>
    <string name="node_save">保存</string>
    <string name="node_delete">删除节点</string>
    <string name="node_delete_confirm">确定要删除此节点吗？此操作无法撤销。</string>
    <string name="node_type_ability">能力</string>
    <string name="node_type_resource">物质</string>
    <string name="node_content_placeholder">输入 Markdown 内容...</string>
    <string name="node_content_empty">暂无内容，点击编辑按钮添加</string>
    <string name="node_tags">标签</string>
    <string name="node_tags_empty">暂无标签，点击 + 添加</string>
    <string name="node_links">关联节点</string>
    <string name="node_links_empty">暂无关联节点</string>
    <string name="node_ai_suggestions">AI 推荐链接</string>

    <string name="create_node_title">新建节点</string>
    <string name="create_node_label">节点标题</string>
    <string name="create_node_type">节点类型</string>
    <string name="create_node_parent">所属父节点（可选）</string>
    <string name="create_node_no_parent">无（作为根节点）</string>
    <string name="create_node_button">创建</string>

    <string name="tag_add_title">添加标签</string>
    <string name="tag_search_hint">搜索或创建标签...</string>
    <string name="tag_create_hint">创建标签 \"%s\"</string>
    <string name="tag_existing">已有标签：</string>
    <string name="tag_done">完成</string>

    <string name="settings_title">设置</string>
    <string name="settings_data_management">数据管理</string>
    <string name="settings_export_markdown">导出 Markdown</string>
    <string name="settings_export_desc">将所有节点导出为 Markdown 文件</string>
    <string name="settings_load_sample">加载示例数据</string>
    <string name="settings_load_sample_desc">加载一棵预置的示例技能树</string>
    <string name="settings_clear_data">清除所有数据</string>
    <string name="settings_clear_data_desc">删除所有节点、标签和链接</string>
    <string name="settings_clear_confirm">此操作将删除所有节点、标签和链接，且无法恢复。确定要继续吗？</string>
    <string name="settings_clear_confirm_btn">确认清除</string>
    <string name="settings_appearance">外观</string>
    <string name="settings_theme">主题</string>
    <string name="settings_theme_dark">暗色模式</string>
    <string name="settings_theme_light">亮色模式</string>
    <string name="settings_theme_system">跟随系统</string>
    <string name="settings_content_management">内容管理</string>
    <string name="settings_tag_management">标签管理</string>
    <string name="settings_link_management">链接管理</string>
    <string name="settings_about">关于</string>
    <string name="settings_restart_onboarding">重新引导</string>
    <string name="settings_about_app">关于技能树</string>

    <string name="common_ok">确定</string>
    <string name="common_cancel">取消</string>
    <string name="common_confirm">确认</string>
    <string name="common_retry">重试</string>
    <string name="common_skip">跳过</string>
    <string name="common_back">返回</string>
    <string name="common_loading">加载中...</string>
    <string name="common_error">错误</string>
    <string name="common_unknown_error">未知错误</string>
</resources>
```

---

## 4. Composable 中使用

### 4.1 基本用法

```kotlin
@Composable
fun MyComponent() {
    // 使用 stringResource 获取本地化文本
    Text(text = stringResource(R.string.tree_empty_title))
}

@Composable
fun ExportButton() {
    val context = LocalContext.current
    Button(onClick = {
        val path = "/sdcard/export.md"
        Toast.makeText(context, context.getString(R.string.tree_export_success, path), Toast.LENGTH_LONG).show()
    }) {
        Text(stringResource(R.string.settings_export_markdown))
    }
}
```

### 4.2 带参数的字符串

```kotlin
// strings.xml:
// <string name="tag_create_hint">Create tag \"%s\"</string>

Text(text = stringResource(R.string.tag_create_hint, "Python"))
// 输出: Create tag "Python"
```

### 4.3 数量字符串（Plurals）

```xml
<!-- values/strings.xml -->
<plurals name="node_count">
    <item quantity="one">%d node</item>
    <item quantity="other">%d nodes</item>
</plurals>

<!-- values-zh/strings.xml -->
<plurals name="node_count">
    <item quantity="other">%d 个节点</item>
</plurals>
```

```kotlin
Text(text = pluralStringResource(R.plurals.node_count, count = 5, 5))
// 英文: 5 nodes
// 中文: 5 个节点
```

---

## 5. 语言切换实现

```kotlin
package com.fancy.skill_tree.core.data.preferences

/**
 * 语言管理器
 */
@Singleton
class LocaleManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences("locale", Context.MODE_PRIVATE)

    val currentLocale: Locale
        get() {
            val languageTag = prefs.getString(KEY_LANGUAGE, null)
            return if (languageTag != null) {
                Locale.forLanguageTag(languageTag)
            } else {
                Locale.getDefault()
            }
        }

    fun setLocale(languageTag: String) {
        prefs.edit().putString(KEY_LANGUAGE, languageTag).apply()
    }

    /**
     * 应用语言设置到 Context
     */
    fun applyLocale(baseContext: Context): Context {
        val locale = currentLocale
        Locale.setDefault(locale)

        val config = Configuration(baseContext.resources.configuration)
        config.setLocale(locale)
        return baseContext.createConfigurationContext(config)
    }

    companion object {
        private const val KEY_LANGUAGE = "app_language"
    }
}
```

### 在 Application 中应用

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

---

## 6. 代码迁移策略

### 6.1 迁移优先级

| 优先级 | 范围 | 说明 |
|--------|------|------|
| P0 | 所有用户可见的 UI 文本 | 按钮、标题、提示、错误消息 |
| P1 | Toast / Snackbar 消息 | 操作反馈 |
| P2 | KDoc / 代码注释 | 保持中文（开发者面向） |
| P3 | 日志输出 | 保持英文（标准化） |

### 6.2 迁移检查清单

- [ ] 将所有硬编码中文文本提取到 `strings.xml`
- [ ] 将所有 Composable 中的 `text = "xxx"` 替换为 `text = stringResource(R.string.xxx)`
- [ ] 将所有 Toast/AlertDialog 文本替换为 stringResource
- [ ] 将 DomainException 的错误消息也进行国际化
- [ ] 验证中英文切换后 UI 布局正常

---

## 7. 实施步骤

| 步骤 | 内容 |
|------|------|
| 1 | 创建 `values/strings.xml`（英文默认） |
| 2 | 创建 `values-zh/strings.xml`（简体中文） |
| 3 | 创建 `LocaleManager` 语言管理器 |
| 4 | 在 `SkillTreeApplication` 中集成语言切换 |
| 5 | 迁移 `SkillTreeScreen` 中的硬编码文本 |
| 6 | 迁移 `NodeDetailScreen` 中的硬编码文本 |
| 7 | 迁移 `CreateNodeDialog` 中的硬编码文本 |
| 8 | 迁移 `SettingsScreen` 中的硬编码文本 |
| 9 | 迁移 Toast/AlertDialog 中的文本 |
| 10 | 编译验证中英文切换 |