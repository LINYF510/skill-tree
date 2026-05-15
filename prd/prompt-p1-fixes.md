# P1 问题修复提示词

## 项目背景

Skill-Tree 是一个 Kotlin / Jetpack Compose / Hilt / Room 技术栈的 Android 应用，采用 Clean Architecture + MVVM。项目已完成核心功能、错误处理、测试、性能优化、主题系统、i18n、无障碍等主要模块的实现。

本次修复涵盖 5 个 P1 优先级问题，按逻辑分为三部分：
- **Part A**: 硬编码中文 i18n 补丁（覆盖 10 个文件、85 处硬编码中文）
- **Part B**: Canvas 缩放回弹动画
- **Part C**: TextMeasureCache + ConnectionPathCache 性能缓存

---

## Part A: 硬编码中文 i18n 补丁

### 问题描述

i18n 实现阶段已创建 `values/strings.xml`（英文默认）和 `values-zh/strings.xml`（中文），但代码中仍有 85 处硬编码中文字符串未提取到 strings.xml。这导致：
1. 英文模式下 UI 仍显示中文
2. TalkBack 在英文环境下朗读中文 contentDescription
3. 未来新增语言时无法翻译这些字符串

### 现有 strings.xml 已有资源

`values/strings.xml` 已包含约 80 个 string resource，涵盖导航、树、节点、创建、标签、链接、附件、设置、统计、成就、通用、错误、引导等分类。**新增 string resource 应追加到现有分类之后，保持分类结构一致。**

`values-zh/strings.xml` 需同步添加对应中文翻译。

### 需要修复的文件和具体位置

#### 1. SkillTreeScreen.kt（15 处）

**contentDescription 硬编码（6 处）**:
```
行 222: contentDescription = "导出"          → stringResource(R.string.cd_export)
行 233: contentDescription = "添加节点"      → stringResource(R.string.cd_add_node)
行 619: contentDescription = "搜索"          → stringResource(R.string.cd_search)
行 681: contentDescription = "关闭搜索"      → stringResource(R.string.cd_close_search)
行 699: contentDescription = "搜索图标"      → stringResource(R.string.cd_search_icon)
行 709: contentDescription = "清除搜索"      → stringResource(R.string.cd_clear_search)
```

**accessibilityLabel 硬编码（2 处）**:
```
行 755: Modifier.accessibilityLabel("搜索历史区域")  → .accessibilityLabel(stringResource(R.string.a11y_search_history))
行 798: Modifier.accessibilityLabel("标签筛选区域")  → .accessibilityLabel(stringResource(R.string.a11y_tag_filter))
```

**其他硬编码中文（7 处）**:
```
行 598: text = "试试其他关键词或调整标签筛选"  → stringResource(R.string.tree_no_results_hint)
行 730: Text("取消", ...)                     → stringResource(R.string.common_cancel)  // 已存在
行 752: text = "搜索历史"                      → stringResource(R.string.tree_search_history)
行 759: text = "清除"                          → stringResource(R.string.tree_clear_history)
行 795: text = "标签筛选"                      → stringResource(R.string.tree_tag_filter)
行 1343: text = "🏆 成就解锁！"                → stringResource(R.string.achievement_unlocked)  // 已存在，去掉emoji
行 1360: text = "奖励：${achievement.reward}"  → stringResource(R.string.achievement_reward, achievement.reward)
```

#### 2. OnboardingScreen.kt（22 处）

**FeatureItem 硬编码（9 处）**:
```
行 131: FeatureItem("📝", "随手记录", "3秒完成")        → FeatureItem("📝", stringResource(R.string.onboard_feat_quick_record), stringResource(R.string.onboard_feat_quick_record_desc))
行 132: FeatureItem("🌳", "树状可视化", "一目了然")     → FeatureItem("🌳", stringResource(R.string.onboard_feat_tree_viz), stringResource(R.string.onboard_feat_tree_viz_desc))
行 133: FeatureItem("🔗", "智能关联", "发现新可能")     → FeatureItem("🔗", stringResource(R.string.onboard_feat_smart_link), stringResource(R.string.onboard_feat_smart_link_desc))
行 134: FeatureItem("🔒", "本地存储", "数据安全")       → FeatureItem("🔒", stringResource(R.string.onboard_feat_local_storage), stringResource(R.string.onboard_feat_local_storage_desc))
行 424: FeatureItem("🔍", "搜索", "查找节点")           → FeatureItem("🔍", stringResource(R.string.onboard_feat_search), stringResource(R.string.onboard_feat_search_desc))
行 425: FeatureItem("➕", "创建节点", "添加新能力/物质") → FeatureItem("➕", stringResource(R.string.onboard_feat_create), stringResource(R.string.onboard_feat_create_desc))
行 426: FeatureItem("✏️", "编辑内容", "Markdown笔记")   → FeatureItem("✏️", stringResource(R.string.onboard_feat_edit), stringResource(R.string.onboard_feat_edit_desc))
行 427: FeatureItem("🔗", "关联节点", "建立跨分支链接") → FeatureItem("🔗", stringResource(R.string.onboard_feat_link), stringResource(R.string.onboard_feat_link_desc))
行 428: FeatureItem("📤", "导出", "导出Markdown")       → FeatureItem("📤", stringResource(R.string.onboard_feat_export), stringResource(R.string.onboard_feat_export_desc))
```

**其他硬编码中文（13 处）**:
```
行 158: text = "技能树 Skill-Tree"                                    → stringResource(R.string.onboard_app_name)
行 167: text = "把你的能力和资产变成一棵可生长的技能树"                  → stringResource(R.string.onboard_app_slogan)
行 186: Text("开始探索", ...)                                         → stringResource(R.string.onboard_explore)
行 192: Text("跳过引导", ...)                                         → stringResource(R.string.onboarding_skip)  // 已存在
行 232: Text("创建你的第一个节点", ...)                                → stringResource(R.string.onboard_create_first_node)
行 234: Text("技能树由能力和物质两种节点组成", ...)                     → stringResource(R.string.onboard_node_types_desc)
行 242: Text("例如：Python 编程", ...)                                → stringResource(R.string.onboard_example_ability)
行 283: "💡 提示：能力节点代表你的技能和知识，物质节点代表你拥有的物品和资源" → stringResource(R.string.onboard_type_tip)
行 297: Text("创建节点", ...)                                         → stringResource(R.string.create_node_button)  // 已存在
行 303: Text("稍后创建", ...)                                         → stringResource(R.string.onboard_create_later)
行 312: "⚔️" to "能力"                                                → stringResource(R.string.node_type_ability)  // 已存在
行 313: "💎" to "物质"                                                → stringResource(R.string.node_type_resource)  // 已存在
行 355: Text("这是你的技能树！", ...)                                  → stringResource(R.string.onboard_your_tree)
行 367: Text("📊 示例技能树（可视化）", ...)                           → stringResource(R.string.onboard_sample_tree)
行 379: Text("💡 提示：", ...)                                         → stringResource(R.string.onboard_tip_label)
行 381: Text("• 双指缩放查看全局", ...)                                → stringResource(R.string.onboard_tip_zoom)
行 382: Text("• 单指拖动平移画布", ...)                                → stringResource(R.string.onboard_tip_pan)
行 383: Text("• 点击节点查看详情", ...)                                → stringResource(R.string.onboard_tip_tap)
行 402: Text("保留示例数据", ...)                                      → stringResource(R.string.onboard_keep_sample)
行 412: Text("进入主页", ...)                                          → stringResource(R.string.onboard_enter_home)
行 438: Text("准备就绪！", ...)                                        → stringResource(R.string.onboard_ready)
行 442: Text("你已经掌握了基础操作，以下是更多功能介绍：", ...)          → stringResource(R.string.onboard_ready_desc)
```

#### 3. AccessibilityOverlay.kt（7 处）

**onClickLabel 硬编码（1 处）**:
```
行 76: onClickLabel = "查看 ${node.entity.title} 的详情"  → onClickLabel = context.getString(R.string.a11y_node_click_detail, node.entity.title)
```
注意：AccessibilityOverlay 是 @Composable，可用 `LocalContext.current.getString()` 获取字符串。

**buildNodeDescription 硬编码（4 处）**:
```
行 92: "能力节点：" → context.getString(R.string.a11y_node_type_ability)
行 92: "物质节点：" → context.getString(R.string.a11y_node_type_resource)
行 94: "，等级 ${node.entity.level}" → context.getString(R.string.a11y_node_level, node.entity.level)
行 96: "，${node.children.size} 个子节点" → context.getString(R.string.a11y_node_children, node.children.size)
行 99: "，有详细内容" → context.getString(R.string.a11y_node_has_content)
```

**buildNodeStateDescription 硬编码（2 处）**:
```
行 112: "点击查看详情" → context.getString(R.string.a11y_state_click_detail)
行 114: "，长按显示操作菜单" → context.getString(R.string.a11y_state_long_press_menu)
```

**改造方案**: `buildNodeDescription` 和 `buildNodeStateDescription` 需要改为接收 `Context` 参数，或在 AccessibilityOverlay 中通过 `LocalContext.current` 预先获取字符串再传入。

#### 4. AddLinkDialog.kt（6 处）

```
行 99:  contentDescription = "搜索"        → stringResource(R.string.cd_search)
行 156: contentDescription = "已选择"       → stringResource(R.string.cd_selected)
行 79:  text = "添加关联节点"               → stringResource(R.string.link_add_title)  // 已存在
行 92:  text = "搜索目标节点..."            → stringResource(R.string.link_search_hint)  // 已存在
行 170: Text("取消", ...)                   → stringResource(R.string.common_cancel)  // 已存在
行 178: Text("确认 (${selectedNodeIds.size})") → stringResource(R.string.link_confirm, selectedNodeIds.size)  // 已存在
```

#### 5. AddTagDialog.kt（6 处）

```
行 127: contentDescription = "搜索"              → stringResource(R.string.cd_search)
行 103: text = "添加标签"                         → stringResource(R.string.tag_add_title)  // 已存在
行 120: text = "搜索或创建标签..."                 → stringResource(R.string.tag_search_hint)  // 已存在
行 168: text = "创建标签 \"${searchQuery.text.trim()}\"" → stringResource(R.string.tag_create_hint, searchQuery.text.trim())  // 已存在
行 179: text = "已有标签："                        → stringResource(R.string.tag_existing)  // 已存在
行 202: Text("完成", ...)                          → stringResource(R.string.tag_done)  // 已存在
```

#### 6. AttachmentsSection.kt（8 处）

```
行 70:  contentDescription = "添加附件"           → stringResource(R.string.cd_add_attachment)
行 172: contentDescription = "删除附件"           → stringResource(R.string.cd_delete_attachment)
行 66:  Modifier.accessibilityButton("添加附件")  → .accessibilityButton(stringResource(R.string.a11y_add_attachment))
行 127: .accessibilityButton("附件：${attachment.fileName}，点击查看") → .accessibilityButton(stringResource(R.string.a11y_attachment_view, attachment.fileName))
行 59:  text = "📎 附件 (${attachments.size})"    → stringResource(R.string.attachment_section_title, attachments.size)
行 89:  text = "暂无附件"                          → stringResource(R.string.attachment_empty)
行 95:  Text("添加第一个附件", ...)                 → stringResource(R.string.attachment_add_first)
行 197: "未知大小"                                 → stringResource(R.string.attachment_unknown_size)
```

#### 7. LinkedNodesSection.kt（13 处）

```
行 70:  contentDescription = "添加链接"                     → stringResource(R.string.cd_add_link)
行 175: contentDescription = "删除链接"                     → stringResource(R.string.cd_delete_link)
行 66:  Modifier.accessibilityButton("添加关联节点")        → .accessibilityButton(stringResource(R.string.a11y_add_link))
行 149: .accessibilityButton("关联节点：${node.title}，点击查看详情") → .accessibilityButton(stringResource(R.string.a11y_linked_node_view, node.title))
行 199: .accessibilityLabel("推荐关联：${node.title}")      → .accessibilityLabel(stringResource(R.string.a11y_suggested_link, node.title))
行 59:  text = "🔗 关联节点 (${confirmedLinks.size})"       → stringResource(R.string.link_section_title, confirmedLinks.size)
行 89:  text = "暂无关联节点"                                → stringResource(R.string.link_empty)
行 95:  Text("添加第一个关联", ...)                          → stringResource(R.string.link_add_first)
行 114: text = "🤖 AI 推荐链接 (${suggestedLinks.size})"    → stringResource(R.string.link_ai_suggestions, suggestedLinks.size)
行 167: text = if (linkType == "MANUAL") "手动关联" else "AI 推荐" → if (linkType == "MANUAL") stringResource(R.string.link_type_manual) else stringResource(R.string.link_type_ai)
行 217: text = "AI 推荐"                                    → stringResource(R.string.link_type_ai)
行 223: Text("确认", ...)                                    → stringResource(R.string.common_confirm)  // 已存在
行 226: Text("忽略", ...)                                    → stringResource(R.string.link_ignore)
```

#### 8. NodeTagsSection.kt（3 处）

```
行 59:  contentDescription = "添加标签"      → stringResource(R.string.cd_add_tag)
行 52:  text = "🏷️ 标签"                     → stringResource(R.string.tag_section_title)
行 67:  text = "暂无标签，点击 + 添加"        → stringResource(R.string.tag_empty_hint)
```

#### 9. ImageViewerDialog.kt（2 处）

```
行 55: contentDescription = "查看图片"  → stringResource(R.string.cd_view_image)
行 84: contentDescription = "关闭"      → stringResource(R.string.common_close)  // 已存在
```

#### 10. MainActivity.kt（3 处）

```
行 268: HOME("技能树", R.drawable.ic_home)       → HOME(stringResource(R.string.nav_skill_tree), R.drawable.ic_home)  // 已存在
行 269: STATISTICS("统计", R.drawable.ic_statistics) → STATISTICS(stringResource(R.string.nav_statistics), R.drawable.ic_statistics)  // 已存在
行 270: SETTINGS("设置", R.drawable.ic_settings)     → SETTINGS(stringResource(R.string.nav_settings), R.drawable.ic_settings)  // 已存在
```
注意：MainActivity 的 enum 不能直接使用 `stringResource`。需要改为在 Composable 中使用 `stringResource` 获取 label，或改用 `@StringRes Int` 引用。

### 新增 string resource 清单

以下是需要新增到 `values/strings.xml` 和 `values-zh/strings.xml` 的 string resource：

#### contentDescription 类（cd_ 前缀）

```xml
<!-- Content Descriptions -->
<string name="cd_export">Export</string>
<string name="cd_add_node">Add node</string>
<string name="cd_search">Search</string>
<string name="cd_close_search">Close search</string>
<string name="cd_search_icon">Search icon</string>
<string name="cd_clear_search">Clear search</string>
<string name="cd_selected">Selected</string>
<string name="cd_add_attachment">Add attachment</string>
<string name="cd_delete_attachment">Delete attachment</string>
<string name="cd_add_link">Add link</string>
<string name="cd_delete_link">Delete link</string>
<string name="cd_add_tag">Add tag</string>
<string name="cd_view_image">View image</string>
```

中文：
```xml
<string name="cd_export">导出</string>
<string name="cd_add_node">添加节点</string>
<string name="cd_search">搜索</string>
<string name="cd_close_search">关闭搜索</string>
<string name="cd_search_icon">搜索图标</string>
<string name="cd_clear_search">清除搜索</string>
<string name="cd_selected">已选择</string>
<string name="cd_add_attachment">添加附件</string>
<string name="cd_delete_attachment">删除附件</string>
<string name="cd_add_link">添加链接</string>
<string name="cd_delete_link">删除链接</string>
<string name="cd_add_tag">添加标签</string>
<string name="cd_view_image">查看图片</string>
```

#### 无障碍描述类（a11y_ 前缀）

```xml
<!-- Accessibility Descriptions -->
<string name="a11y_search_history">Search history area</string>
<string name="a11y_tag_filter">Tag filter area</string>
<string name="a11y_add_attachment">Add attachment</string>
<string name="a11y_attachment_view">Attachment: %s, tap to view</string>
<string name="a11y_add_link">Add linked node</string>
<string name="a11y_linked_node_view">Linked node: %s, tap to view details</string>
<string name="a11y_suggested_link">Suggested link: %s</string>
<string name="a11y_node_click_detail">View details of %s</string>
<string name="a11y_node_type_ability">Ability node: </string>
<string name="a11y_node_type_resource">Resource node: </string>
<string name="a11y_node_level">, Level %d</string>
<string name="a11y_node_children">, %d child nodes</string>
<string name="a11y_node_has_content">, has content</string>
<string name="a11y_state_click_detail">Tap to view details</string>
<string name="a11y_state_long_press_menu">, long press for options</string>
```

中文：
```xml
<string name="a11y_search_history">搜索历史区域</string>
<string name="a11y_tag_filter">标签筛选区域</string>
<string name="a11y_add_attachment">添加附件</string>
<string name="a11y_attachment_view">附件：%s，点击查看</string>
<string name="a11y_add_link">添加关联节点</string>
<string name="a11y_linked_node_view">关联节点：%s，点击查看详情</string>
<string name="a11y_suggested_link">推荐关联：%s</string>
<string name="a11y_node_click_detail">查看 %s 的详情</string>
<string name="a11y_node_type_ability">能力节点：</string>
<string name="a11y_node_type_resource">物质节点：</string>
<string name="a11y_node_level">，等级 %d</string>
<string name="a11y_node_children">，%d 个子节点</string>
<string name="a11y_node_has_content">，有详细内容</string>
<string name="a11y_state_click_detail">点击查看详情</string>
<string name="a11y_state_long_press_menu">，长按显示操作菜单</string>
```

#### 引导页（onboard_ 前缀）

```xml
<!-- Onboarding Features -->
<string name="onboard_feat_quick_record">Quick Record</string>
<string name="onboard_feat_quick_record_desc">Done in 3 seconds</string>
<string name="onboard_feat_tree_viz">Tree Visualization</string>
<string name="onboard_feat_tree_viz_desc">Clear at a glance</string>
<string name="onboard_feat_smart_link">Smart Links</string>
<string name="onboard_feat_smart_link_desc">Discover new possibilities</string>
<string name="onboard_feat_local_storage">Local Storage</string>
<string name="onboard_feat_local_storage_desc">Data security</string>
<string name="onboard_feat_search">Search</string>
<string name="onboard_feat_search_desc">Find nodes</string>
<string name="onboard_feat_create">Create Node</string>
<string name="onboard_feat_create_desc">Add abilities/resources</string>
<string name="onboard_feat_edit">Edit Content</string>
<string name="onboard_feat_edit_desc">Markdown notes</string>
<string name="onboard_feat_link">Link Nodes</string>
<string name="onboard_feat_link_desc">Cross-branch connections</string>
<string name="onboard_feat_export">Export</string>
<string name="onboard_feat_export_desc">Export as Markdown</string>

<!-- Onboarding Content -->
<string name="onboard_app_name">Skill-Tree</string>
<string name="onboard_app_slogan">Turn your skills and assets into a growing skill tree</string>
<string name="onboard_explore">Start Exploring</string>
<string name="onboard_create_first_node">Create Your First Node</string>
<string name="onboard_node_types_desc">A skill tree consists of ability and resource nodes</string>
<string name="onboard_example_ability">e.g. Python Programming</string>
<string name="onboard_type_tip">💡 Tip: Ability nodes represent your skills and knowledge, resource nodes represent items and assets you own</string>
<string name="onboard_create_later">Create Later</string>
<string name="onboard_your_tree">This is your skill tree!</string>
<string name="onboard_sample_tree">📊 Sample Skill Tree (Visualization)</string>
<string name="onboard_tip_label">💡 Tips:</string>
<string name="onboard_tip_zoom">• Pinch to zoom for overview</string>
<string name="onboard_tip_pan">• Drag to pan the canvas</string>
<string name="onboard_tip_tap">• Tap a node for details</string>
<string name="onboard_keep_sample">Keep Sample Data</string>
<string name="onboard_enter_home">Enter Home</string>
<string name="onboard_ready">You\'re All Set!</string>
<string name="onboard_ready_desc">You\'ve mastered the basics. Here are more features to explore:</string>
```

中文：
```xml
<string name="onboard_feat_quick_record">随手记录</string>
<string name="onboard_feat_quick_record_desc">3秒完成</string>
<string name="onboard_feat_tree_viz">树状可视化</string>
<string name="onboard_feat_tree_viz_desc">一目了然</string>
<string name="onboard_feat_smart_link">智能关联</string>
<string name="onboard_feat_smart_link_desc">发现新可能</string>
<string name="onboard_feat_local_storage">本地存储</string>
<string name="onboard_feat_local_storage_desc">数据安全</string>
<string name="onboard_feat_search">搜索</string>
<string name="onboard_feat_search_desc">查找节点</string>
<string name="onboard_feat_create">创建节点</string>
<string name="onboard_feat_create_desc">添加新能力/物质</string>
<string name="onboard_feat_edit">编辑内容</string>
<string name="onboard_feat_edit_desc">Markdown笔记</string>
<string name="onboard_feat_link">关联节点</string>
<string name="onboard_feat_link_desc">建立跨分支链接</string>
<string name="onboard_feat_export">导出</string>
<string name="onboard_feat_export_desc">导出Markdown</string>

<string name="onboard_app_name">技能树 Skill-Tree</string>
<string name="onboard_app_slogan">把你的能力和资产变成一棵可生长的技能树</string>
<string name="onboard_explore">开始探索</string>
<string name="onboard_create_first_node">创建你的第一个节点</string>
<string name="onboard_node_types_desc">技能树由能力和物质两种节点组成</string>
<string name="onboard_example_ability">例如：Python 编程</string>
<string name="onboard_type_tip">💡 提示：能力节点代表你的技能和知识，物质节点代表你拥有的物品和资源</string>
<string name="onboard_create_later">稍后创建</string>
<string name="onboard_your_tree">这是你的技能树！</string>
<string name="onboard_sample_tree">📊 示例技能树（可视化）</string>
<string name="onboard_tip_label">💡 提示：</string>
<string name="onboard_tip_zoom">• 双指缩放查看全局</string>
<string name="onboard_tip_pan">• 单指拖动平移画布</string>
<string name="onboard_tip_tap">• 点击节点查看详情</string>
<string name="onboard_keep_sample">保留示例数据</string>
<string name="onboard_enter_home">进入主页</string>
<string name="onboard_ready">准备就绪！</string>
<string name="onboard_ready_desc">你已经掌握了基础操作，以下是更多功能介绍：</string>
```

#### 树页面补充

```xml
<!-- Skill Tree - Additional -->
<string name="tree_no_results_hint">Try different keywords or adjust tag filters</string>
<string name="tree_search_history">Search History</string>
<string name="tree_clear_history">Clear</string>
<string name="tree_tag_filter">Tag Filter</string>
```

中文：
```xml
<string name="tree_no_results_hint">试试其他关键词或调整标签筛选</string>
<string name="tree_search_history">搜索历史</string>
<string name="tree_clear_history">清除</string>
<string name="tree_tag_filter">标签筛选</string>
```

#### 附件补充

```xml
<!-- Attachments - Additional -->
<string name="attachment_section_title">📎 Attachments (%d)</string>
<string name="attachment_empty">No attachments</string>
<string name="attachment_add_first">Add first attachment</string>
<string name="attachment_unknown_size">Unknown size</string>
```

中文：
```xml
<string name="attachment_section_title">📎 附件 (%d)</string>
<string name="attachment_empty">暂无附件</string>
<string name="attachment_add_first">添加第一个附件</string>
<string name="attachment_unknown_size">未知大小</string>
```

#### 关联节点补充

```xml
<!-- Links - Additional -->
<string name="link_section_title">🔗 Linked Nodes (%d)</string>
<string name="link_empty">No linked nodes</string>
<string name="link_add_first">Add first link</string>
<string name="link_ai_suggestions">🤖 AI Suggestions (%d)</string>
<string name="link_type_manual">Manual link</string>
<string name="link_type_ai">AI Suggested</string>
<string name="link_ignore">Ignore</string>
```

中文：
```xml
<string name="link_section_title">🔗 关联节点 (%d)</string>
<string name="link_empty">暂无关联节点</string>
<string name="link_add_first">添加第一个关联</string>
<string name="link_ai_suggestions">🤖 AI 推荐链接 (%d)</string>
<string name="link_type_manual">手动关联</string>
<string name="link_type_ai">AI 推荐</string>
<string name="link_ignore">忽略</string>
```

#### 标签补充

```xml
<!-- Tags - Additional -->
<string name="tag_section_title">🏷️ Tags</string>
<string name="tag_empty_hint">No tags, tap + to add</string>
```

中文：
```xml
<string name="tag_section_title">🏷️ 标签</string>
<string name="tag_empty_hint">暂无标签，点击 + 添加</string>
```

#### 成就补充

```xml
<!-- Achievements - Additional -->
<string name="achievement_reward">Reward: %s</string>
```

中文：
```xml
<string name="achievement_reward">奖励：%s</string>
```

### 实现要求

1. **所有新增 string resource 追加到 `values/strings.xml` 和 `values-zh/strings.xml` 对应分类末尾**
2. **已存在的 string resource 直接复用，不要重复定义**（如 `common_cancel`, `common_close`, `tag_add_title` 等）
3. **AccessibilityOverlay 的 `buildNodeDescription` / `buildNodeStateDescription` 需要改造**：添加 `Context` 参数或改为在 Composable 中用 `LocalContext.current` 获取字符串
4. **MainActivity 的 enum `BottomNavItem` 不能直接用 `stringResource`**：改为 `@StringRes val labelResId: Int`，在 Composable 中用 `stringResource(item.labelResId)` 获取显示文本
5. **格式字符串使用 `%s` / `%d` 占位符**，调用时通过 `stringResource(R.string.xxx, arg1, arg2)` 传入参数
6. **修改完成后运行 `./gradlew assembleDebug` 确认编译通过**

---

## Part B: Canvas 缩放回弹动画

### 问题描述

当前画布缩放使用 `detectTransformGestures` 直接赋值 `scale = (scale * zoom).coerceIn(MIN_SCALE, MAX_SCALE)`，没有回弹效果。当用户缩放到边界（MIN_SCALE=0.3f 或 MAX_SCALE=3.0f）时，体验生硬。

### PRD 要求

双指缩放结束时，200ms spring 回弹动画：
- 缩放超过 MAX_SCALE 时回弹到 MAX_SCALE
- 缩放低于 MIN_SCALE 时回弹到 MIN_SCALE
- 缩放在范围内时无回弹

### 当前代码位置

**SkillTreeScreen.kt**:
```
行 127-128: MIN_SCALE = 0.3f, MAX_SCALE = 3.0f
行 161: var scale by remember { mutableFloatStateOf(1f) }
行 429-434: detectTransformGestures { _, pan, zoom, _ -> scale = (scale * zoom).coerceIn(...) }
```

**AnimationConfig.kt**:
```kotlin
object AnimationConfig {
    // ... 现有常量
    // 需要新增缩放回弹相关常量
}
```

### 实现方案

1. 在 `AnimationConfig` 中新增：
```kotlin
const val ZOOM_BOUNCE_DURATION = 200
val ZOOM_BOUNCE_SPRING = SpringSpec<Float>(
    dampingRatio = Spring.DampingRatioMediumBouncy,
    stiffness = Spring.StiffnessMedium
)
```

2. 在 SkillTreeScreen 中，将 `scale` 从 `mutableFloatStateOf` 改为 `Animatable`：
```kotlin
val scaleAnimatable = remember { Animatable(1f) }
var scale by remember { mutableFloatStateOf(1f) }
```

3. 在 `detectTransformGestures` 中，缩放时先不 coerce，而是允许短暂超出范围：
```kotlin
detectTransformGestures { _, pan, zoom, _ ->
    val newScale = scale * zoom
    scale = newScale  // 允许短暂超出范围
    offsetX += pan.x
    offsetY += pan.y
}
```

4. 在手势结束时（`detectTransformGestures` 无法直接检测结束，需改用 `awaitPointerEventScope` + `awaitEachGesture`），触发回弹动画：
```kotlin
suspend fun animateBounce() {
    val target = scale.coerceIn(MIN_SCALE, MAX_SCALE)
    if (target != scale) {
        scaleAnimatable.animateTo(
            targetValue = target,
            animationSpec = AnimationConfig.ZOOM_BOUNCE_SPRING
        ) {
            scale = value
        }
    }
}
```

5. 或者更简单的方案：使用 `Modifier.pointerInput` + `awaitEachGesture` + `detectTransformGestures` 的组合，在手指抬起时检测是否需要回弹并启动 `Animatable.snapTo` + `animateTo`。

### 实现要求

1. 在 `AnimationConfig.kt` 中新增缩放回弹常量
2. 修改 SkillTreeScreen.kt 中的缩放手势处理，添加回弹动画
3. 回弹动画使用 SpringSpec，阻尼比 MediumBouncy，刚度 Medium
4. 回弹动画期间禁止手势缩放（避免冲突）
5. 确保回弹动画不影响 ViewportCuller 和 AccessibilityOverlay 的 scale 同步
6. 修改完成后运行 `./gradlew assembleDebug` 确认编译通过

---

## Part C: TextMeasureCache + ConnectionPathCache 性能缓存

### 问题描述

Canvas 绘制节点文本时，每帧都调用 `textMeasurer.measure()` 重新测量文本布局，在大树场景下造成不必要的性能开销。同样，节点间连线的贝塞尔曲线 Path 每帧重新计算。

### PRD 设计

**TextMeasureCache**（来自 prd/03-performance-optimization.md）:
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

**ConnectionPathCache**（来自 prd/03-performance-optimization.md）:
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

### 当前代码位置

**SkillTreeScreen.kt**:
```
行 304: val textMeasurer = rememberTextMeasurer()
行 1093: textMeasurer: TextMeasurer,  // drawNodes 函数参数
```

drawNodes 中使用 `textMeasurer.measure()` 绘制节点文本。

drawConnections 中使用 `Path().apply { cubicTo(...) }` 绘制连线。

### 实现方案

1. 在 `core/ui/render/` 目录下创建 `TextMeasureCache.kt`，实现上述 PRD 设计
2. 在 `core/ui/render/` 目录下创建 `ConnectionPathCache.kt`，实现上述 PRD 设计
3. 在 SkillTreeScreen.kt 中：
   - `remember { TextMeasureCache() }` 创建缓存实例
   - `remember { ConnectionPathCache() }` 创建缓存实例
   - 将缓存传入 `drawNodes` 和 `drawConnections` 函数
   - `drawNodes` 中用 `textMeasureCache.getOrMeasure(key, text, style, textMeasurer)` 替代直接 `textMeasurer.measure()`
   - `drawConnections` 中用 `connectionPathCache.getOrCreatePath(key, startX, startY, endX, endY)` 替代直接创建 Path
4. 缓存 key 设计：
   - TextMeasureCache key: `"${node.entity.id}_${node.entity.title}_${node.entity.level}"` （节点内容变化时自动失效）
   - ConnectionPathCache key: `"${parentPos.x}_${parentPos.y}_${childPos.x}_${childPos.y}"` （位置变化时自动生成新 key）
5. 在节点数据变化时调用 `textMeasureCache.clear()` 清除缓存
6. 在 `TreeLayoutCache` 失效时调用 `connectionPathCache.clear()` 清除路径缓存

### 实现要求

1. 两个缓存类都需要 KDoc 注释
2. TextMeasureCache 使用 LinkedHashMap 实现 LRU（accessOrder = true），maxSize = 500
3. ConnectionPathCache 使用 android.util.LruCache，maxSize = 1000
4. 缓存 key 必须包含影响渲染结果的所有变量（文本内容、样式参数、坐标位置等）
5. 在 SkillTreeScreen 中正确集成缓存，确保数据变化时缓存失效
6. 修改完成后运行 `./gradlew assembleDebug` 确认编译通过

---

## 验证清单

完成所有修改后，请依次验证：

1. `./gradlew assembleDebug` 编译通过
2. `./gradlew test` 42 测试全部通过
3. 切换到英文模式，检查所有页面无中文残留
4. 切换到中文模式，检查所有页面显示正常
5. 双指缩放画布到超出范围，松手后回弹动画流畅
6. 大树场景（50+ 节点）下 Canvas 绘制无明显卡顿
7. TalkBack 模式下节点描述朗读正确（中英文均测试）
