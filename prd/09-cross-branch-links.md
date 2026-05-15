# 跨分支链接系统实现方案

> **关联 PRD**: 第 2.3 节（F2.4 跨分支链接）、第 5.1.2 节（node_link 表）
> **当前状态**: 数据库层完整（Entity + DAO），UI 只有硬编码占位"暂无关联节点"
> **目标**: 实现跨分支链接的创建、可视化展示和管理

---

## 1. 现状分析

| 层级 | 状态 |
|------|------|
| Entity (`NodeLinkEntity`) | ✅ 已定义（含 sourceId、targetId、linkType、confirmed） |
| DAO (`NodeLinkDao`) | ✅ 已实现 |
| Repository 接口 | ❌ 未暴露链接方法 |
| UseCase | ❌ 不存在 |
| UI | ❌ 仅有硬编码占位 |

---

## 2. 数据层补充

### 2.1 Repository 接口扩展

```kotlin
interface SkillTreeRepository {
    // ... 现有方法 ...
    fun getAllLinks(): Flow<List<NodeLinkEntity>>
    fun getLinksForNode(nodeId: String): Flow<List<NodeLinkEntity>>
    suspend fun createLink(sourceId: String, targetId: String, linkType: String = "MANUAL"): NodeLinkEntity
    suspend fun deleteLink(linkId: String)
    suspend fun confirmLink(linkId: String)
    fun getSuggestedLinks(nodeId: String): Flow<List<NodeLinkEntity>>
}
```

### 2.2 联合查询数据类

```kotlin
package com.fancy.skill_tree.core.data.repository

data class NodeLinkWithTarget(
    val link: NodeLinkEntity,
    val targetNode: SkillNodeEntity
)
```

### 2.3 Repository 实现

```kotlin
override fun getLinksForNode(nodeId: String): Flow<List<NodeLinkEntity>> {
    return nodeLinkDao.getLinksByNodeId(nodeId)
}

override suspend fun createLink(sourceId: String, targetId: String, linkType: String): NodeLinkEntity {
    val link = NodeLinkEntity(
        id = UUID.randomUUID().toString(),
        sourceId = sourceId, targetId = targetId, linkType = linkType
    )
    nodeLinkDao.insertLink(link)
    return link
}

override suspend fun deleteLink(linkId: String) { nodeLinkDao.deleteLinkById(linkId) }

override suspend fun confirmLink(linkId: String) {
    val link = nodeLinkDao.getLinkById(linkId) ?: return
    nodeLinkDao.insertLink(link.copy(confirmed = true))
}
```

---

## 3. Canvas 上的链接可视化

### 3.1 连线样式区分

| 链接类型 | 线条样式 | 颜色 |
|---------|---------|------|
| 父子关系（已有） | 实线贝塞尔曲线 | `#F0883E` (LinkOrange) |
| 手动跨分支链接 | 虚线贝塞尔曲线 | `#F0883E` (LinkOrange) |
| AI 推荐链接（未确认） | 虚线 + 发光效果 | `#79C0FF` (AiBlue) |

### 3.2 跨分支连线绘制

```kotlin
private fun DrawScope.drawCrossBranchLinks(
    links: List<NodeLinkEntity>,
    positions: List<NodePosition>
) {
    val positionMap = positions.associateBy { it.nodeId }

    links.forEach { link ->
        val sourcePos = positionMap[link.sourceId] ?: return@forEach
        val targetPos = positionMap[link.targetId] ?: return@forEach

        val startX = sourcePos.x; val startY = sourcePos.y
        val endX = targetPos.x; val endY = targetPos.y
        val midX = (startX + endX) / 2; val midY = (startY + endY) / 2

        val path = Path().apply {
            moveTo(startX, startY)
            cubicTo(midX, startY, midX, endY, endX, endY)
        }

        val linkColor = when {
            link.linkType == "AI_SUGGESTED" && !link.confirmed -> AiBlue
            else -> LinkOrange
        }

        val pathEffect = when {
            link.linkType == "AI_SUGGESTED" && !link.confirmed -> PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
            link.linkType == "MANUAL" -> PathEffect.dashPathEffect(floatArrayOf(15f, 5f), 0f)
            else -> null
        }

        drawPath(path = path, color = linkColor, style = Stroke(width = if (link.linkType == "AI_SUGGESTED") 2f else 3f, pathEffect = pathEffect))

        if (link.linkType == "AI_SUGGESTED" && !link.confirmed) {
            drawPath(path = path, color = AiBlue.copy(alpha = 0.2f), style = Stroke(width = 6f, pathEffect = pathEffect))
        }
    }
}
```

---

## 4. UI 设计

### 4.1 节点详情页 — 关联节点区域

```
┌─────────────────────────────────────┐
│  ← 返回    ⚔️ Python    ✏️  ⋮      │
├─────────────────────────────────────┤
│  🔗 关联节点 (3)              ➕    │
│  ┌──────────────────────────────┐   │
│  │ ⚔️ 数据分析             ✕   │   │
│  │ 🔗 手动关联                  │   │
│  └──────────────────────────────┘   │
│  ┌──────────────────────────────┐   │
│  │ ⚔️ Web爬虫              ✕   │   │
│  └──────────────────────────────┘   │
│                                     │
│  🤖 AI 推荐链接 (2)                 │
│  ┌──────────────────────────────┐   │
│  │ ⚔️ 机器学习    [确认] [忽略] │   │
│  └──────────────────────────────┘   │
└─────────────────────────────────────┘
```

### 4.2 添加链接弹窗

```
┌─────────────────────────────────────┐
│         添加关联节点                 │
│                                     │
│  ┌─────────────────────────────┐    │
│  │ 🔍 搜索目标节点...           │    │
│  └─────────────────────────────┘    │
│                                     │
│  可选节点：                          │
│  ┌──────────────────────────────┐   │
│  │ ⚔️ 数据分析              ✓  │   │
│  │    Python > 数据分析          │   │
│  └──────────────────────────────┘   │
│  ┌──────────────────────────────┐   │
│  │ ⚔️ Android开发                │   │
│  │    编程 > Kotlin > Android    │   │
│  └──────────────────────────────┘   │
│                                     │
│  ┌──────────┐  ┌──────────┐        │
│  │  取消     │  │  确认 (1) │        │
│  └──────────┘  └──────────┘        │
└─────────────────────────────────────┘
```

---

## 5. Composable 实现

### 5.1 节点详情页中的链接区域

```kotlin
@Composable
fun LinkedNodesSection(
    confirmedLinks: List<NodeLinkWithTarget>,
    suggestedLinks: List<NodeLinkWithTarget>,
    onLinkClick: (String) -> Unit,
    onRemoveLink: (NodeLinkEntity) -> Unit,
    onConfirmLink: (NodeLinkEntity) -> Unit,
    onIgnoreLink: (NodeLinkEntity) -> Unit,
    onAddLink: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("🔗 关联节点 (${confirmedLinks.size})", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            IconButton(onClick = onAddLink) {
                Icon(Icons.Default.Add, contentDescription = "添加链接", tint = PrimaryBlue)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (confirmedLinks.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(SurfaceDark).padding(16.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("暂无关联节点", color = TextSecondary, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = onAddLink) { Text("添加第一个关联", color = PrimaryBlue) }
                }
            }
        } else {
            confirmedLinks.forEach { linkWithTarget ->
                LinkedNodeCard(
                    node = linkWithTarget.targetNode,
                    linkType = linkWithTarget.link.linkType,
                    onClick = { onLinkClick(linkWithTarget.targetNode.id) },
                    onRemove = { onRemoveLink(linkWithTarget.link) }
                )
            }
        }

        if (suggestedLinks.isNotEmpty()) {
            Spacer(modifier = Modifier.height(20.dp))
            Text("🤖 AI 推荐链接 (${suggestedLinks.size})", color = AiBlue, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(8.dp))
            suggestedLinks.forEach { linkWithTarget ->
                SuggestedLinkCard(
                    node = linkWithTarget.targetNode,
                    onConfirm = { onConfirmLink(linkWithTarget.link) },
                    onIgnore = { onIgnoreLink(linkWithTarget.link) },
                    onClick = { onLinkClick(linkWithTarget.targetNode.id) }
                )
            }
        }
    }
}

@Composable
private fun LinkedNodeCard(node: SkillNodeEntity, linkType: String, onClick: () -> Unit, onRemove: () -> Unit) {
    Surface(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable(onClick = onClick), color = SurfaceDark, shape = RoundedCornerShape(12.dp)) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(if (node.nodeType == "ABILITY") "⚔️" else "💎", fontSize = 20.sp)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(node.title, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                Text(if (linkType == "MANUAL") "手动关联" else "AI 推荐", color = TextSecondary, fontSize = 12.sp)
            }
            IconButton(onClick = onRemove) {
                Icon(Icons.Default.Close, contentDescription = "删除链接", tint = TextSecondary)
            }
        }
    }
}

@Composable
private fun SuggestedLinkCard(node: SkillNodeEntity, onConfirm: () -> Unit, onIgnore: () -> Unit, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable(onClick = onClick),
        color = SurfaceDark, shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, AiBlue.copy(alpha = 0.3f))
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(if (node.nodeType == "ABILITY") "⚔️" else "💎", fontSize = 20.sp)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(node.title, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                Text("AI 推荐", color = AiBlue, fontSize = 12.sp)
            }
            TextButton(onClick = onConfirm) { Text("确认", color = AbilityGreen, fontSize = 13.sp) }
            TextButton(onClick = onIgnore) { Text("忽略", color = TextSecondary, fontSize = 13.sp) }
        }
    }
}
```

### 5.2 添加链接弹窗

```kotlin
@Composable
fun AddLinkDialog(
    allNodes: List<SkillNodeEntity>,
    currentNodeId: String,
    existingLinkedNodeIds: Set<String>,
    onDismiss: () -> Unit,
    onConfirm: (List<String>) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedNodeIds by remember { mutableStateOf(setOf<String>()) }

    val availableNodes = allNodes.filter { node ->
        node.id != currentNodeId && node.id !in existingLinkedNodeIds &&
        (searchQuery.isBlank() || node.title.contains(searchQuery, ignoreCase = true))
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(modifier = Modifier.fillMaxWidth().padding(16.dp), shape = RoundedCornerShape(16.dp), color = SurfaceDark) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("添加关联节点", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = searchQuery, onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("搜索目标节点...", color = TextSecondary) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = PrimaryBlue) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary, focusedBorderColor = PrimaryBlue, unfocusedBorderColor = TextSecondary)
                )

                Spacer(modifier = Modifier.height(16.dp))

                LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                    items(availableNodes) { node ->
                        val isSelected = node.id in selectedNodeIds
                        Surface(
                            modifier = Modifier.fillMaxWidth().clickable { selectedNodeIds = if (isSelected) selectedNodeIds - node.id else selectedNodeIds + node.id },
                            color = if (isSelected) PrimaryBlue.copy(alpha = 0.1f) else Color.Transparent,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(if (node.nodeType == "ABILITY") "⚔️" else "💎", fontSize = 18.sp)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(node.title, color = TextPrimary, fontSize = 15.sp)
                                    Text(getNodePath(node, allNodes), color = TextSecondary, fontSize = 12.sp)
                                }
                                Spacer(modifier = Modifier.weight(1f))
                                if (isSelected) Icon(Icons.Default.Check, contentDescription = null, tint = PrimaryBlue)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("取消", color = TextSecondary) }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(onClick = { onConfirm(selectedNodeIds.toList()) }, enabled = selectedNodeIds.isNotEmpty(), colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)) {
                        Text("确认 (${selectedNodeIds.size})")
                    }
                }
            }
        }
    }
}

private fun getNodePath(node: SkillNodeEntity, allNodes: List<SkillNodeEntity>): String {
    val path = mutableListOf<String>()
    var current: SkillNodeEntity? = node
    while (current != null) {
        path.add(current.title)
        current = allNodes.find { it.id == current?.parentId }
    }
    return path.reversed().joinToString(" > ")
}
```

---

## 6. 实施步骤

| 步骤 | 内容 |
|------|------|
| 1 | 扩展 `SkillTreeRepository` 接口添加链接方法 |
| 2 | 在 `SkillTreeRepositoryImpl` 中实现链接方法 |
| 3 | 创建链接相关 UseCase |
| 4 | 实现 Canvas 上的跨分支虚线连线绘制 |
| 5 | 创建 `LinkedNodesSection` 组件 |
| 6 | 创建 `LinkedNodeCard` 和 `SuggestedLinkCard` 组件 |
| 7 | 创建 `AddLinkDialog` 添加链接弹窗 |
| 8 | 在 `NodeDetailScreen` 中集成链接区域 |
| 9 | 在 `SkillTreeScreen` Canvas 中绘制跨分支链接 |