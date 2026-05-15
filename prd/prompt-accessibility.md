# 无障碍 Accessibility 实现提示词

## 项目背景

Skill-Tree 项目当前在无障碍方面存在严重缺失：Canvas 自定义绘制完全不可被 TalkBack 访问，无焦点导航支持，部分按钮缺少 contentDescription。需要实现 TalkBack 兼容、语义描述和基础焦点导航。

**重要**: 请先阅读项目规则 `CLAUDE.md` 和无障碍方案 `prd/04-accessibility.md`。

---

## 当前代码现状

### 已有的 contentDescription（20 处，但全部硬编码中文）
- SkillTreeScreen: "导出"、"添加节点"、"搜索"、"关闭搜索"、"清除搜索"
- NodeDetailScreen: "返回"、"查看"/"编辑"、"更多"
- MainActivity: navigation contentDescription
- ImageViewerDialog: "查看图片"、"关闭"
- AttachmentsSection: "添加附件"、"删除附件"
- LinkedNodesSection: "添加链接"、"删除链接"

### 缺失的无障碍支持
- Canvas 绘制的节点完全不可访问（TalkBack 无法感知）
- 无语义代理层
- 无键盘导航
- 无 `semantics` 修饰符
- 无 `Role` 设置
- 部分 `contentDescription` 为 `null`（如搜索图标、关闭图标）

---

## 实现任务

### 任务 1：创建无障碍扩展函数

**新增文件**: `app/src/main/java/com/fancy/skill_tree/core/ui/accessibility/AccessibilityExt.kt`

```kotlin
package com.fancy.skill_tree.core.ui.accessibility

import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.mergeDescendants
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription

/**
 * 为 Compose 组件添加无障碍标签
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

/**
 * 为切换元素添加状态描述
 */
fun Modifier.accessibilityToggle(isChecked: Boolean, label: String): Modifier {
    return this.semantics(mergeDescendants = true) {
        contentDescription = label
        role = Role.Switch
        stateDescription = if (isChecked) "已开启" else "已关闭"
    }
}
```

### 任务 2：创建 AccessibilityOverlay 语义代理层

**新增文件**: `app/src/main/java/com/fancy/skill_tree/core/ui/accessibility/AccessibilityOverlay.kt`

这是核心组件——在 Canvas 上方叠加透明的 Box 组件，为每个可见节点创建无障碍代理。

```kotlin
package com.fancy.skill_tree.core.ui.accessibility

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.mergeDescendants
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.fancy.skill_tree.feature.tree.NodePosition
import com.fancy.skill_tree.feature.tree.TreeNode

/**
 * Canvas 无障碍语义代理层
 * 在 Canvas 上方叠加透明可点击区域，使 TalkBack 能感知节点
 */
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

    Box(modifier = Modifier) {
        treeNodes.forEach { node ->
            val pos = positionMap[node.entity.id] ?: return@forEach

            if (visibleNodeIds.isNotEmpty() && node.entity.id !in visibleNodeIds) return@forEach

            val screenX = pos.x * scale + offsetX - (nodeWidth * scale) / 2
            val screenY = pos.y * scale + offsetY - (nodeHeight * scale) / 2
            val scaledWidth = nodeWidth * scale
            val scaledHeight = nodeHeight * scale

            if (scaledWidth <= 0f || scaledHeight <= 0f) return@forEach

            Box(
                modifier = Modifier
                    .offset { IntOffset(screenX.toInt(), screenY.toInt()) }
                    .size(scaledWidth.toDp(), scaledHeight.toDp())
                    .semantics {
                        mergeDescendants = true
                        contentDescription = buildString {
                            append(if (node.entity.nodeType == "ABILITY") "能力节点：" else "物质节点：")
                            append(node.entity.title)
                            append("，等级 ${node.entity.level}")
                            if (node.children.isNotEmpty()) {
                                append("，${node.children.size} 个子节点")
                            }
                            if (!node.entity.content.isNullOrBlank()) {
                                append("，有详细内容")
                            }
                        }
                        role = Role.Button
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

### 任务 3：在 SkillTreeScreen 中集成 AccessibilityOverlay

**文件**: `app/src/main/java/com/fancy/skill_tree/feature/tree/SkillTreeScreen.kt`

在 Canvas 所在的 Box 内，Canvas 之后添加 AccessibilityOverlay：

```kotlin
Box(modifier = Modifier.fillMaxSize()) {
    // 现有 Canvas
    Canvas(modifier = ...) { ... }

    // 无障碍语义代理层
    AccessibilityOverlay(
        treeNodes = treeNodes,
        nodePositions = nodePositions,
        visibleNodeIds = visibleNodeIds,
        scale = scale,
        offsetX = offsetX,
        offsetY = offsetY,
        nodeWidth = NODE_WIDTH,
        nodeHeight = NODE_HEIGHT,
        onNodeClick = { nodeId -> onSelectNode(nodeId) },
        onNodeLongClick = { nodeId -> onSelectNode(nodeId) }
    )
}
```

**重要**: AccessibilityOverlay 需要在 Canvas 的 withTransform 块之外，因为它是独立的 Composable 层，不参与 Canvas 的坐标变换。需要手动计算屏幕坐标（`pos.x * scale + offsetX`）。

### 任务 4：为所有按钮添加 Role 和 contentDescription

遍历所有 Screen 和 Component 文件，为以下元素添加无障碍语义：

**SkillTreeScreen.kt**:
- FAB 按钮：已有 `contentDescription = "添加节点"`，添加 `role = Role.Button`
- 导出按钮：已有 `contentDescription = "导出"`，添加 `role = Role.Button`
- 搜索按钮：已有 `contentDescription = "搜索"`，添加 `role = Role.Button`

**NodeDetailScreen.kt**:
- 返回按钮：已有 `contentDescription = "返回"`，添加 `role = Role.Button`
- 编辑/查看按钮：已有 contentDescription，添加 `role = Role.Button`
- 更多按钮：已有 `contentDescription = "更多"`，添加 `role = Role.Button`

**SettingsScreen.kt**:
- 所有设置项：添加 `role = Role.Button` 和 `onClickLabel`
- 清除数据按钮：添加 `stateDescription = "危险操作"`

**StatisticsScreen.kt**:
- 统计卡片：添加 `accessibilityLabel("{title}: {value}")`

**TagChip.kt**:
- 标签芯片：添加 `accessibilityButton("标签：{name}，点击移除")`

**LinkedNodesSection.kt**:
- 关联节点卡片：添加 `accessibilityButton("关联节点：{title}，点击查看详情")`

**AttachmentsSection.kt**:
- 附件项：添加 `accessibilityButton("附件：{name}，点击查看")`

### 任务 5：修复 contentDescription = null 的问题

将所有 `contentDescription = null` 替换为有意义的描述：

| 位置 | 当前 | 修改为 |
|------|------|--------|
| SkillTreeScreen 搜索图标 | `null` | `"搜索图标"` |
| NodeDetailScreen 下拉菜单图标 | `null` | `"更多操作"` |
| AddTagDialog 关闭按钮 | `null` | `"关闭"` |
| AddLinkDialog 搜索图标 | `null` | `"搜索"` |

### 任务 6：实现基础键盘导航

**文件**: `app/src/main/java/com/fancy/skill_tree/feature/tree/SkillTreeScreen.kt`

在 Canvas 容器上添加 `onKeyEvent` 处理：

```kotlin
val focusRequester = remember { FocusRequester() }

Box(
    modifier = Modifier
        .fillMaxSize()
        .focusRequester(focusRequester)
        .focusable()
        .onKeyEvent { keyEvent ->
            if (keyEvent.type == KeyEventType.KeyDown) {
                when (keyEvent.key) {
                    Key.DirectionUp -> { navigateToParent(); true }
                    Key.DirectionDown -> { navigateToFirstChild(); true }
                    Key.DirectionLeft -> { navigateToPreviousSibling(); true }
                    Key.DirectionRight -> { navigateToNextSibling(); true }
                    Key.Enter, Key.Spacebar -> {
                        selectedNodeId.value?.let { onNodeClick(it) }
                        true
                    }
                    else -> false
                }
            } else false
        }
) { ... }
```

导航逻辑需要在 ViewModel 中添加：
- `navigateToParent()`: 选中当前节点的父节点
- `navigateToFirstChild()`: 选中当前节点的第一个子节点
- `navigateToPreviousSibling()`: 选中上一个兄弟节点
- `navigateToNextSibling()`: 选中下一个兄弟节点

这些方法需要基于当前 `selectedNodeId` 和 `uiState.nodes` 的树结构计算。

### 任务 7：字体缩放验证

确保以下组件在 200% 字体缩放下布局正常：
- 节点详情页内容可滚动（已有 `verticalScroll`）
- 搜索栏不溢出
- 对话框内容可滚动
- 统计卡片文字不截断

如有问题，添加 `overflow = TextOverflow.Ellipsis` 或调整布局。

---

## 实现优先级

1. **P0**: 任务 1-3（扩展函数 + AccessibilityOverlay + SkillTreeScreen 集成）
2. **P1**: 任务 4-5（按钮 Role + contentDescription 修复）
3. **P2**: 任务 6（键盘导航）
4. **P2**: 任务 7（字体缩放验证）

---

## 编码规范提醒

- 所有 public 函数必须有 KDoc 注释
- 禁止使用 `!!` 非空断言
- contentDescription 应使用 `stringResource` 而非硬编码（与 i18n 协同）
- 如果 i18n 尚未实现，contentDescription 可暂用硬编码，后续迁移
- AccessibilityOverlay 的节点描述应包含足够信息（类型+标题+等级+子节点数）

---

## 实现完成后

1. 运行 `./gradlew assembleDebug` 确保编译通过
2. 运行 `./gradlew test` 确保测试通过
3. 将本次实现内容总结写入 `doc/rep/2026-05-15-无障碍.md`
