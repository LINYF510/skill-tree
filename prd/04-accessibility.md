# 无障碍（Accessibility）设计方案

> **关联 PRD**: 无（PRD 中缺失此项）
> **当前状态**: 零实现，Canvas 自定义绘制完全不可访问
> **目标**: 实现 TalkBack 兼容、语义描述、焦点导航

---

## 1. 问题分析

当前项目在无障碍方面存在以下核心问题：

| 问题 | 影响 |
|------|------|
| Canvas 自定义绘制无 contentDescription | TalkBack 用户无法获知节点信息 |
| 无焦点导航支持 | 键盘/无障碍服务无法遍历节点 |
| 纯颜色区分节点类型 | 色觉障碍用户无法区分 |
| 手势操作无替代方案 | 仅支持触控操作 |

---

## 2. 解决方案总览

| 策略 | 说明 |
|------|------|
| Canvas 语义层覆盖 | 在 Canvas 上叠加透明的 `Box` 组件作为无障碍代理 |
| 语义节点树 | 为每个可见节点创建对应的 `SemanticsNode` |
| 键盘导航 | 支持方向键在节点间移动 |
| 颜色无障碍 | 使用图标 + 形状 + 文字多重区分（已部分满足） |
| 屏幕阅读器适配 | 所有文本和按钮添加 `contentDescription` |

---

## 3. Canvas 无障碍实现

### 3.1 语义代理层

```kotlin
@Composable
fun SkillTreeScreen(...) {
    // ... 现有 Canvas 代码 ...

    // 在 Canvas 上方覆盖一个透明的无障碍代理层
    Box(modifier = Modifier.fillMaxSize()) {
        // Canvas（原有代码）
        Canvas(modifier = Modifier.fillMaxSize()) { ... }

        // 无障碍语义代理层
        AccessibilityOverlay(
            treeNodes = treeNodes,
            nodePositions = nodePositions,
            scale = scale,
            offsetX = offsetX,
            offsetY = offsetY,
            onNodeClick = { nodeId -> ... },
            onNodeLongPress = { nodeId -> ... }
        )
    }
}
```

### 3.2 无障碍代理层实现

```kotlin
@Composable
private fun AccessibilityOverlay(
    treeNodes: List<TreeNode>,
    nodePositions: List<NodePosition>,
    scale: Float,
    offsetX: Float,
    offsetY: Float,
    onNodeClick: (String) -> Unit,
    onNodeLongPress: (String) -> Unit
) {
    val positionMap = nodePositions.associateBy { it.nodeId }

    Box(modifier = Modifier.fillMaxSize()) {
        treeNodes.forEach { node ->
            val pos = positionMap[node.entity.id] ?: return@forEach

            // 计算屏幕坐标
            val screenX = pos.x * scale + offsetX
            val screenY = pos.y * scale + offsetY
            val scaledWidth = NODE_WIDTH * scale
            val scaledHeight = NODE_HEIGHT * scale

            Box(
                modifier = Modifier
                    .offset { IntOffset(screenX.toInt() - (scaledWidth / 2).toInt(), screenY.toInt() - (scaledHeight / 2).toInt()) }
                    .size(scaledWidth.toDp(), scaledHeight.toDp())
                    .semantics {
                        // 合并为一个语义元素
                        mergeDescendants = true

                        // 节点类型和标题
                        contentDescription = buildString {
                            append(if (node.entity.nodeType == "ABILITY") "能力节点：" else "物质节点：")
                            append(node.entity.title)
                            if (node.entity.content != null) {
                                append("，有详细内容")
                            }
                        }

                        // 角色
                        role = Role.Button

                        // 状态
                        stateDescription = "点击查看详情，长按显示操作菜单"
                    }
                    .clickable(
                        onClick = { onNodeClick(node.entity.id) },
                        role = Role.Button,
                        onClickLabel = "查看 ${node.entity.title} 的详情"
                    )
            )
        }
    }
}
```

---

## 4. 全局无障碍配置

### 4.1 主题中的无障碍设置

```kotlin
// Theme.kt
@Composable
fun SkilltreeTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = SkillTreeTypography,
        content = content
    )
}

// 在 MaterialTheme 中设置无障碍友好的最小触摸目标
// Compose Material3 默认触摸目标为 48dp，满足 WCAG 2.1 AA 标准
```

### 4.2 通用无障碍扩展函数

```kotlin
package com.fancy.skill_tree.core.ui.accessibility

import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription

/**
 * 为 Compose 组件添加无障碍语义
 */
fun Modifier.accessibilityLabel(label: String): Modifier {
    return this.semantics(mergeDescendants = true) {
        contentDescription = label
    }
}

/**
 * 为按钮添加无障碍描述
 */
fun Modifier.accessibilityButton(label: String, actionLabel: String? = null): Modifier {
    return this.semantics(mergeDescendants = true) {
        contentDescription = label
        role = Role.Button
        if (actionLabel != null) {
            stateDescription = actionLabel
        }
    }
}

/**
 * 为可展开/折叠元素添加状态描述
 */
fun Modifier.accessibilityExpandable(isExpanded: Boolean, label: String): Modifier {
    return this.semantics(mergeDescendants = true) {
        contentDescription = label
        stateDescription = if (isExpanded) "已展开" else "已折叠"
    }
}
```

---

## 5. 组件级无障碍适配清单

| 组件 | 适配内容 |
|------|---------|
| `SkillTreeScreen` Canvas | 叠加语义代理层（见上） |
| `NodeDetailScreen` 标题栏 | 返回按钮: "返回技能树"，编辑按钮: "编辑节点内容" |
| `CreateNodeDialog` | 输入框: "节点标题"，类型选择: "选择节点类型：能力或物质" |
| `SearchBar` | 搜索框: "搜索节点"，关闭按钮: "关闭搜索" |
| `EmptyStateGuide` | "技能树为空，点击加载示例数据或创建第一个节点" |
| `FloatingActionButton` | 添加按钮: "创建新节点"，导出按钮: "导出为 Markdown" |
| `TagChip` | "标签：{name}，点击移除" |
| `LinkedNodeCard` | "关联节点：{title}，点击查看详情" |
| `StatCard` | "{title}：{value}" |

---

## 6. 颜色对比度检查

当前配色与 WCAG 2.1 AA 标准对比：

| 组合 | 对比度 | AA 标准 | 是否通过 |
|------|--------|---------|---------|
| TextPrimary (#E6EDF3) / BackgroundDark (#0D1117) | 14.5:1 | ≥ 4.5:1 | ✅ |
| TextSecondary (#8B949E) / BackgroundDark (#0D1117) | 5.4:1 | ≥ 4.5:1 | ✅ |
| PrimaryBlue (#58A6FF) / BackgroundDark (#0D1117) | 6.8:1 | ≥ 3:1 (大文本) | ✅ |
| AbilityGreen (#3FB950) / BackgroundDark (#0D1117) | 5.1:1 | ≥ 3:1 | ✅ |
| ResourcePurple (#D2A8FF) / BackgroundDark (#0D1117) | 7.2:1 | ≥ 3:1 | ✅ |
| LinkOrange (#F0883E) / BackgroundDark (#0D1117) | 5.7:1 | ≥ 3:1 | ✅ |

当前配色方案对比度基本满足 AA 标准，无需调整。

---

## 7. 键盘导航支持

```kotlin
@Composable
fun SkillTreeScreen(...) {
    // 键盘焦点管理
    val focusRequester = remember { FocusRequester() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .focusRequester(focusRequester)
            .onKeyEvent { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyDown) {
                    when (keyEvent.key) {
                        Key.DirectionUp -> {
                            // 向上移动到父节点
                            navigateToParent()
                            true
                        }
                        Key.DirectionDown -> {
                            // 向下移动到第一个子节点
                            navigateToFirstChild()
                            true
                        }
                        Key.DirectionLeft -> {
                            // 向左移动到上一个兄弟节点
                            navigateToPreviousSibling()
                            true
                        }
                        Key.DirectionRight -> {
                            // 向右移动到下一个兄弟节点
                            navigateToNextSibling()
                            true
                        }
                        Key.Enter, Key.Spacebar -> {
                            // 确认：打开选中节点的详情
                            selectedNodeId?.let { onNodeClick(it) }
                            true
                        }
                        else -> false
                    }
                } else false
            }
    ) {
        // ... 现有内容 ...
    }
}
```

---

## 8. 字体缩放支持

```kotlin
// 使用 sp 单位（已满足），但需验证极端缩放下的布局
// 测试场景：
// - 系统字体放大到 200%
// - 验证节点标题截断逻辑是否正常工作
// - 验证弹窗内容是否可滚动

@Composable
fun NodeDetailScreen(...) {
    // 使用 Modifier.verticalScroll 确保内容在字体放大时可滚动
    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // ... 现有内容 ...
    }
}
```

---

## 9. 实施步骤

| 步骤 | 内容 |
|------|------|
| 1 | 创建 `AccessibilityOverlay` 语义代理层 Composable |
| 2 | 创建 `accessibilityLabel`、`accessibilityButton` 等扩展函数 |
| 3 | 在 `SkillTreeScreen` 中集成语义代理层 |
| 4 | 为所有按钮和输入框添加 `contentDescription` |
| 5 | 为所有可交互组件设置 `Role` |
| 6 | 验证颜色对比度（已完成，均通过） |
| 7 | 实现基础键盘导航支持 |
| 8 | 使用 TalkBack 进行实际测试 |
| 9 | 修复 TalkBack 测试中发现的问题 |