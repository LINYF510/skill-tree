# 搜索增强方案

> **关联 PRD**: 第 2.2 节（F1.1 快速创建）、第 6.1 节（语义搜索）
> **当前状态**: 仅标题关键词模糊匹配，无内容搜索，无高亮
> **目标**: 实现标题+内容全文搜索、搜索结果高亮、搜索历史、未来语义搜索预留

---

## 1. 现状 vs 目标

| 特性 | 当前 | Phase 1 目标 | Phase 3 目标 |
|------|------|-------------|-------------|
| 标题搜索 | ✅ 模糊匹配 | ✅ 模糊匹配 | ✅ 模糊匹配 |
| 内容搜索 | ❌ | ✅ 全文匹配 | ✅ 全文匹配 |
| 结果高亮 | ❌ | ✅ Canvas 高亮 + 列表高亮 | ✅ |
| 搜索历史 | ❌ | ✅ 最近 10 条 | ✅ |
| 搜索结果列表 | ❌ | ✅ 树形路径显示 | ✅ |
| 语义搜索 | ❌ | ❌ | ✅ AI 语义匹配 |
| 标签筛选 | ❌ | ✅ 标签组合筛选 | ✅ |

---

## 2. 数据层扩展

### 2.1 DAO 搜索增强

```kotlin
@Dao
interface SkillNodeDao {
    // 现有：仅标题搜索
    @Query("SELECT * FROM skill_node WHERE title LIKE '%' || :query || '%' ORDER BY sortOrder ASC")
    fun searchByTitle(query: String): Flow<List<SkillNodeEntity>>

    // 新增：标题 + 内容全文搜索
    @Query("""
        SELECT * FROM skill_node 
        WHERE title LIKE '%' || :query || '%' 
           OR content LIKE '%' || :query || '%' 
        ORDER BY 
            CASE WHEN title LIKE '%' || :query || '%' THEN 0 ELSE 1 END,
            sortOrder ASC
    """)
    fun searchFullText(query: String): Flow<List<SkillNodeEntity>>

    // 新增：多标签筛选搜索
    @Query("""
        SELECT DISTINCT sn.* FROM skill_node sn
        INNER JOIN node_tag nt ON sn.id = nt.nodeId
        WHERE nt.tagId IN (:tagIds)
        ORDER BY sn.sortOrder ASC
    """)
    fun searchByTags(tagIds: List<String>): Flow<List<SkillNodeEntity>>
}
```

### 2.2 Repository 扩展

```kotlin
interface SkillTreeRepository {
    fun searchFullText(query: String): Flow<List<SkillNodeEntity>>
    fun searchByTags(tagIds: List<String>): Flow<List<SkillNodeEntity>>
}
```

---

## 3. 搜索 UI 设计

### 3.1 搜索栏交互流程

```
┌─────────────────────────────────────┐
│  🌳 Skill-Tree          🔍  ⚙️     │  ← 默认状态
├─────────────────────────────────────┤
│  [树形画布]                          │
└─────────────────────────────────────┘
         │ 点击 🔍
         ▼
┌─────────────────────────────────────┐
│  ← ┌───────────────────────┐ ✕     │  ← 搜索状态
│    │ 🔍 搜索节点...         │       │
│    └───────────────────────┘       │
├─────────────────────────────────────┤
│                                     │
│  搜索结果 (3):                       │
│  ┌─────────────────────────────┐    │
│  │ ⚔️ Python 编程               │    │  ← 点击跳转到该节点
│  │    编程 > Python 编程         │    │
│  └─────────────────────────────┘    │
│  ┌─────────────────────────────┐    │
│  │ ⚔️ Python 爬虫               │    │
│  │    编程 > Python 爬虫         │    │
│  └─────────────────────────────┘    │
│  ┌─────────────────────────────┐    │
│  │ ⚔️ Python 数据分析           │    │
│  │    编程 > Python 数据分析     │    │
│  └─────────────────────────────┘    │
│                                     │
│  搜索历史:                           │
│  ┌──────────┐ ┌──────────┐         │
│  │ Python   │ │ 机器学习  │         │
│  └──────────┘ └──────────┘         │
└─────────────────────────────────────┘
```

### 3.2 搜索结果列表 Composable

```kotlin
@Composable
fun SearchResultList(
    results: List<SkillNodeEntity>,
    allNodes: List<SkillNodeEntity>,
    query: String,
    searchHistory: List<String>,
    onResultClick: (String) -> Unit,
    onHistoryClick: (String) -> Unit,
    onClearHistory: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().background(BackgroundDark)) {
        if (query.isBlank() && searchHistory.isNotEmpty()) {
            // 显示搜索历史
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("搜索历史", color = TextSecondary, fontSize = 13.sp)
                TextButton(onClick = onClearHistory) {
                    Text("清除", color = PrimaryBlue, fontSize = 13.sp)
                }
            }

            FlowRow(
                modifier = Modifier.padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                searchHistory.forEach { term ->
                    AssistChip(
                        onClick = { onHistoryClick(term) },
                        label = { Text(term, color = TextPrimary, fontSize = 13.sp) }
                    )
                }
            }
        }

        if (results.isEmpty() && query.isNotBlank()) {
            // 无结果
            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🔍", fontSize = 40.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("没有找到 \"$query\" 相关的结果", color = TextSecondary, fontSize = 14.sp)
                }
            }
        }

        // 搜索结果
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(results, key = { it.id }) { node ->
                SearchResultItem(
                    node = node,
                    allNodes = allNodes,
                    query = query,
                    onClick = { onResultClick(node.id) }
                )
            }
        }
    }
}

@Composable
private fun SearchResultItem(
    node: SkillNodeEntity,
    allNodes: List<SkillNodeEntity>,
    query: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        color = SurfaceDark,
        shape = RoundedCornerShape(0.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(if (node.nodeType == "ABILITY") "⚔️" else "💎", fontSize = 20.sp)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                // 高亮匹配文本
                HighlightedText(text = node.title, query = query, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                Text(getNodePath(node, allNodes), color = TextSecondary, fontSize = 12.sp)
            }
        }
    }
}

/**
 * 高亮匹配关键字的文本
 */
@Composable
fun HighlightedText(
    text: String,
    query: String,
    color: Color,
    fontSize: TextUnit,
    fontWeight: FontWeight = FontWeight.Normal
) {
    if (query.isBlank()) {
        Text(text, color = color, fontSize = fontSize, fontWeight = fontWeight)
        return
    }

    val annotatedString = buildAnnotatedString {
        var currentIndex = 0
        val lowerText = text.lowercase()
        val lowerQuery = query.lowercase()

        while (currentIndex < text.length) {
            val matchIndex = lowerText.indexOf(lowerQuery, currentIndex)
            if (matchIndex == -1) {
                withStyle(SpanStyle(color = color)) {
                    append(text.substring(currentIndex))
                }
                break
            }

            // 匹配前的文本
            if (matchIndex > currentIndex) {
                withStyle(SpanStyle(color = color)) {
                    append(text.substring(currentIndex, matchIndex))
                }
            }

            // 匹配的文本（高亮）
            withStyle(SpanStyle(
                color = PrimaryBlue,
                fontWeight = FontWeight.Bold,
                background = PrimaryBlue.copy(alpha = 0.15f)
            )) {
                append(text.substring(matchIndex, matchIndex + query.length))
            }

            currentIndex = matchIndex + query.length
        }
    }

    Text(text = annotatedString, fontSize = fontSize, fontWeight = fontWeight)
}
```

---

## 4. 搜索历史管理

```kotlin
package com.fancy.skill_tree.feature.tree.search

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 搜索历史管理器
 * 最多保留 10 条搜索历史
 */
@Singleton
class SearchHistoryManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences("search_history", Context.MODE_PRIVATE)
    private val maxHistorySize = 10

    fun getHistory(): List<String> {
        val history = prefs.getString(KEY_HISTORY, "") ?: ""
        return if (history.isEmpty()) emptyList() else history.split(HISTORY_SEPARATOR)
    }

    fun addToHistory(query: String) {
        if (query.isBlank()) return

        val history = getHistory().toMutableList()
        history.remove(query) // 移除旧的重复项
        history.add(0, query) // 添加到最前面
        if (history.size > maxHistorySize) {
            history.removeAt(history.size - 1)
        }
        prefs.edit().putString(KEY_HISTORY, history.joinToString(HISTORY_SEPARATOR)).apply()
    }

    fun clearHistory() {
        prefs.edit().remove(KEY_HISTORY).apply()
    }

    companion object {
        private const val KEY_HISTORY = "search_history"
        private const val HISTORY_SEPARATOR = "\n"
    }
}
```

---

## 5. 搜索栏 Composable

```kotlin
@Composable
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusRequester = remember { FocusRequester() }

    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 返回按钮
        IconButton(onClick = onClose) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "关闭搜索", tint = TextPrimary)
        }

        // 搜索输入框
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.weight(1f).focusRequester(focusRequester),
            placeholder = { Text("搜索节点...", color = TextSecondary) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = PrimaryBlue) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "清除", tint = TextSecondary)
                    }
                }
            },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                focusedBorderColor = PrimaryBlue,
                unfocusedBorderColor = TextSecondary,
                focusedContainerColor = SurfaceDark,
                unfocusedContainerColor = SurfaceDark
            ),
            shape = RoundedCornerShape(12.dp)
        )

        // 关闭按钮
        TextButton(onClick = onClose) {
            Text("取消", color = PrimaryBlue)
        }
    }

    // 自动聚焦
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}
```

---

## 6. ViewModel 扩展

```kotlin
// SkillTreeViewModel 扩展
class SkillTreeViewModel @Inject constructor(
    // ... 现有依赖 ...
    private val searchNodesUseCase: SearchNodesUseCase,
    private val searchHistoryManager: SearchHistoryManager
) : ViewModel() {

    private val _searchResults = MutableStateFlow<List<SkillNodeEntity>>(emptyList())
    val searchResults: StateFlow<List<SkillNodeEntity>> = _searchResults.asStateFlow()

    val searchHistory: StateFlow<List<String>>
        get() = MutableStateFlow(searchHistoryManager.getHistory()).asStateFlow()

    fun search(query: String) {
        viewModelScope.launch {
            searchNodesUseCase(query).collect { results ->
                _searchResults.value = results
            }
        }
    }

    fun saveSearchHistory(query: String) {
        searchHistoryManager.addToHistory(query)
    }

    fun clearSearchHistory() {
        searchHistoryManager.clearHistory()
    }
}
```

---

## 7. Phase 3 语义搜索预留

```kotlin
/**
 * 语义搜索接口（Phase 3 实现）
 * 预留接口，当 AI 模块就绪后接入
 */
interface SemanticSearchEngine {
    /**
     * 对查询进行语义向量化
     */
    suspend fun encodeQuery(query: String): FloatArray

    /**
     * 对节点内容进行语义向量化
     */
    suspend fun encodeNode(node: SkillNodeEntity): FloatArray

    /**
     * 计算两个向量之间的余弦相似度
     */
    fun cosineSimilarity(a: FloatArray, b: FloatArray): Float

    /**
     * 语义搜索
     * @param query 用户查询
     * @param topK 返回前 K 个结果
     * @return 按相似度降序排列的节点列表
     */
    suspend fun semanticSearch(query: String, topK: Int = 10): List<SearchResult>

    data class SearchResult(
        val node: SkillNodeEntity,
        val similarity: Float,
        val matchedSnippet: String
    )
}
```

---

## 8. 实施步骤

| 步骤 | 内容 |
|------|------|
| 1 | 扩展 `SkillNodeDao` 添加全文搜索和标签搜索 |
| 2 | 扩展 `SkillTreeRepository` 接口 |
| 3 | 创建 `SearchNodesUseCase`（含全文+标签组合） |
| 4 | 创建 `SearchBar` Composable 组件 |
| 5 | 创建 `SearchResultList` 搜索结果列表 |
| 6 | 创建 `HighlightedText` 高亮文本组件 |
| 7 | 创建 `SearchHistoryManager` 搜索历史管理 |
| 8 | 在 `SkillTreeScreen` 中集成搜索栏和结果列表 |
| 9 | 预留 `SemanticSearchEngine` 接口 |
| 10 | 编译验证搜索功能 |