# 标签系统实现方案

> **关联 PRD**: 第 5.1.2 节（tag 表 / node_tag 表）
> **当前状态**: 数据库层完整（Entity + DAO），但 UI 为零，Repository 也未暴露标签相关方法
> **目标**: 实现完整的标签 CRUD UI + 节点标签关联管理

---

## 1. 现状分析

| 层级 | 状态 |
|------|------|
| Entity (`TagEntity`, `NodeTagCrossRef`) | ✅ 已定义 |
| DAO (`TagDao`, `NodeTagDao`) | ✅ 已实现 |
| Repository 接口 | ❌ 未暴露标签方法 |
| UseCase | ❌ 不存在 |
| UI | ❌ 不存在 |

---

## 2. 数据层补充

### 2.1 Repository 接口扩展

```kotlin
// SkillTreeRepository 新增方法
interface SkillTreeRepository {
    // ... 现有方法 ...

    // 标签管理
    fun getAllTags(): Flow<List<TagEntity>>
    suspend fun getTagById(tagId: String): TagEntity?
    suspend fun createTag(name: String, color: String? = null): TagEntity
    suspend fun deleteTag(tagId: String)

    // 节点-标签关联
    suspend fun assignTagToNode(nodeId: String, tagId: String)
    suspend fun removeTagFromNode(nodeId: String, tagId: String)
    fun getTagsForNode(nodeId: String): Flow<List<TagEntity>>
    fun getNodesWithTag(tagId: String): Flow<List<SkillNodeEntity>>
}
```

### 2.2 Repository 实现

```kotlin
// SkillTreeRepositoryImpl 新增方法
override fun getAllTags(): Flow<List<TagEntity>> = tagDao.getAllTags()

override suspend fun getTagById(tagId: String): TagEntity? = tagDao.getTagById(tagId)

override suspend fun createTag(name: String, color: String?): TagEntity {
    val tag = TagEntity(
        id = UUID.randomUUID().toString(),
        name = name.trim(),
        color = color
    )
    tagDao.insertTag(tag)
    return tag
}

override suspend fun deleteTag(tagId: String) {
    nodeTagDao.deleteAllNodesForTag(tagId)
    tagDao.deleteTagById(tagId)
}

override suspend fun assignTagToNode(nodeId: String, tagId: String) {
    nodeTagDao.insertNodeTag(NodeTagCrossRef(nodeId = nodeId, tagId = tagId))
}

override suspend fun removeTagFromNode(nodeId: String, tagId: String) {
    nodeTagDao.deleteNodeTag(NodeTagCrossRef(nodeId = nodeId, tagId = tagId))
}

override fun getTagsForNode(nodeId: String): Flow<List<TagEntity>> = nodeTagDao.getTagsForNode(nodeId)

override fun getNodesWithTag(tagId: String): Flow<List<SkillNodeEntity>> = nodeTagDao.getNodesWithTag(tagId)
```

---

## 3. UI 设计

### 3.1 节点详情页中的标签区域

```
┌─────────────────────────────────────┐
│  ← 返回    ⚔️ Python    ✏️  ⋮      │
├─────────────────────────────────────┤
│  🏷️ 标签                            │
│  ┌──────┐ ┌──────┐ ┌──────┐        │
│  │ 编程  │ │Python│ │数据分析│  ➕   │  ← 标签 Chip + 添加按钮
│  └──────┘ └──────┘ └──────┘        │
│                                     │
│  ## Python 编程                      │
│  ...                                │
└─────────────────────────────────────┘
```

### 3.2 标签添加弹窗

```
┌─────────────────────────────────────┐
│         添加标签                     │
│                                     │
│  ┌─────────────────────────────┐    │
│  │ 🔍 搜索或创建标签...         │    │  ← 搜索输入框（带自动补全）
│  └─────────────────────────────┘    │
│                                     │
│  已有标签：                          │
│  ┌──────┐ ┌──────┐ ┌──────┐        │
│  │ 编程  │ │设计  │ │语言  │        │
│  └──────┘ └──────┘ └──────┘        │
│  ┌──────┐ ┌──────┐ ┌──────┐        │
│  │Python│ │Java  │ │前端  │        │
│  └──────┘ └──────┘ └──────┘        │
│                                     │
│  创建新标签：                        │
│  ┌─────────────────────────────┐    │
│  │ "机器学习" → 点击创建         │    │
│  └─────────────────────────────┘    │
│                                     │
│  ┌──────────┐  ┌──────────┐        │
│  │  取消     │  │  完成     │        │
│  └──────────┘  └──────────┘        │
└─────────────────────────────────────┘
```

### 3.3 标签管理页（设置 → 标签管理）

```
┌─────────────────────────────────────┐
│  ← 返回          标签管理           │
├─────────────────────────────────────┤
│  ┌─────────────────────────────┐    │
│  │ 🔍 搜索标签...               │    │
│  └─────────────────────────────┘    │
│                                     │
│  ┌──────────────────────────────┐   │
│  │ 编程 (12)               🗑️  │   │  ← 标签名 + 关联节点数 + 删除
│  │  ┌──────┐ ┌──────┐          │   │
│  │  │#58A6FF│ 编程 │           │   │  ← 颜色选择器
│  │  └──────┘ └──────┘          │   │
│  └──────────────────────────────┘   │
│                                     │
│  ┌──────────────────────────────┐   │
│  │ Python (5)              🗑️  │   │
│  └──────────────────────────────┘   │
└─────────────────────────────────────┘
```

---

## 4. Composable 实现

### 4.1 标签 Chip 组件

```kotlin
@Composable
fun TagChip(
    tag: TagEntity,
    isSelected: Boolean = false,
    onRemove: (() -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    val chipColor = tag.color?.let { Color(android.graphics.Color.parseColor(it)) } ?: PrimaryBlue

    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        color = if (isSelected) chipColor.copy(alpha = 0.2f) else SurfaceDark,
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(chipColor)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(tag.name, color = TextPrimary, fontSize = 13.sp)
            if (onRemove != null) {
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(onClick = onRemove, modifier = Modifier.size(16.dp)) {
                    Text("✕", color = TextSecondary, fontSize = 10.sp)
                }
            }
        }
    }
}
```

### 4.2 节点标签区域（NodeDetailScreen 中集成）

```kotlin
@Composable
fun NodeTagsSection(
    tags: List<TagEntity>,
    onAddTag: () -> Unit,
    onRemoveTag: (TagEntity) -> Unit,
    onTagClick: (TagEntity) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("🏷️ 标签", color = TextSecondary, fontSize = 14.sp)
            IconButton(onClick = onAddTag) {
                Icon(Icons.Default.Add, contentDescription = "添加标签", tint = PrimaryBlue)
            }
        }

        if (tags.isEmpty()) {
            Text("暂无标签，点击 + 添加", color = TextSecondary, fontSize = 13.sp)
        } else {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                tags.forEach { tag ->
                    TagChip(
                        tag = tag,
                        onRemove = { onRemoveTag(tag) },
                        onClick = { onTagClick(tag) }
                    )
                }
            }
        }
    }
}
```

### 4.3 标签添加弹窗

```kotlin
@Composable
fun AddTagDialog(
    allTags: List<TagEntity>,
    currentNodeTags: List<TagEntity>,
    onDismiss: () -> Unit,
    onToggleTag: (TagEntity) -> Unit,
    onCreateTag: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }

    val filteredTags = allTags.filter { tag ->
        tag.name.contains(searchQuery, ignoreCase = true) &&
        tag.id !in currentNodeTags.map { it.id }.toSet()
    }

    val isNewTag = searchQuery.isNotBlank() &&
        allTags.none { it.name.equals(searchQuery, ignoreCase = true) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            color = SurfaceDark
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("添加标签", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("搜索或创建标签...", color = TextSecondary) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = PrimaryBlue) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary,
                        focusedBorderColor = PrimaryBlue, unfocusedBorderColor = TextSecondary
                    )
                )

                // 创建新标签
                if (isNewTag) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth().clickable { onCreateTag(searchQuery.trim()) },
                        color = PrimaryBlue.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("➕", fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("创建标签 \"${searchQuery.trim()}\"", color = PrimaryBlue, fontSize = 14.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 已有标签列表
                if (filteredTags.isNotEmpty()) {
                    Text("已有标签：", color = TextSecondary, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        filteredTags.forEach { tag ->
                            TagChip(tag = tag, onClick = { onToggleTag(tag) })
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("完成", color = PrimaryBlue) }
                }
            }
        }
    }
}
```

### 4.4 按标签筛选技能树

```kotlin
@Composable
fun TagFilterBar(
    allTags: List<TagEntity>,
    selectedTagId: String?,
    onTagSelected: (String?) -> Unit
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            FilterChip(selected = selectedTagId == null, onClick = { onTagSelected(null) }, label = { Text("全部") })
        }
        items(allTags) { tag ->
            FilterChip(selected = selectedTagId == tag.id, onClick = { onTagSelected(tag.id) }, label = { Text(tag.name) })
        }
    }
}
```

---

## 5. ViewModel 扩展

```kotlin
// NodeDetailViewModel 扩展
class NodeDetailViewModel @Inject constructor(
    // ... 现有依赖 ...
    private val getAllTagsUseCase: GetAllTagsUseCase,
    private val createTagUseCase: CreateTagUseCase,
    private val assignTagToNodeUseCase: AssignTagToNodeUseCase,
    private val removeTagFromNodeUseCase: RemoveTagFromNodeUseCase
) : ViewModel() {

    fun addTag(tagId: String) {
        viewModelScope.launch {
            assignTagToNodeUseCase(nodeId, tagId)
        }
    }

    fun removeTag(tagId: String) {
        viewModelScope.launch {
            removeTagFromNodeUseCase(nodeId, tagId)
        }
    }

    fun createAndAssignTag(name: String) {
        viewModelScope.launch {
            when (val result = createTagUseCase(name)) {
                is Outcome.Success -> assignTagToNodeUseCase(nodeId, result.data.id)
                is Outcome.Error -> { /* 错误处理 */ }
            }
        }
    }
}
```

---

## 6. 实施步骤

| 步骤 | 内容 |
|------|------|
| 1 | 扩展 `SkillTreeRepository` 接口添加标签方法 |
| 2 | 在 `SkillTreeRepositoryImpl` 中实现标签方法 |
| 3 | 创建标签相关 UseCase |
| 4 | 创建 `TagChip` 通用 Composable 组件 |
| 5 | 创建 `NodeTagsSection` 节点详情标签区域 |
| 6 | 创建 `AddTagDialog` 标签添加弹窗 |
| 7 | 在 `NodeDetailScreen` 中集成标签区域 |
| 8 | 创建标签管理页面 |
| 9 | 创建 `TagFilterBar` 标签筛选栏 |
| 10 | 在 `SkillTreeScreen` 中集成标签筛选 |