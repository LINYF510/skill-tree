package com.fancy.skill_tree.feature.tree

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fancy.skill_tree.R
import com.fancy.skill_tree.core.data.exception.DataException
import com.fancy.skill_tree.core.data.manager.SearchHistoryManager
import com.fancy.skill_tree.core.domain.common.Outcome
import com.fancy.skill_tree.core.domain.entity.SkillNodeEntity
import com.fancy.skill_tree.core.domain.entity.TagEntity
import com.fancy.skill_tree.core.domain.usecase.gamification.CheckAchievementsUseCase
import com.fancy.skill_tree.core.domain.usecase.gamification.SearchNodesUseCase
import com.fancy.skill_tree.core.domain.usecase.node.CreateNodeUseCase
import com.fancy.skill_tree.core.domain.usecase.node.DeleteNodeUseCase
import com.fancy.skill_tree.core.domain.usecase.node.ExportToMarkdownUseCase
import com.fancy.skill_tree.core.domain.usecase.node.GetAllNodesUseCase
import com.fancy.skill_tree.core.domain.usecase.node.GetAllTagsUseCase
import com.fancy.skill_tree.core.domain.usecase.node.MoveNodeUseCase
import com.fancy.skill_tree.core.domain.usecase.node.ToggleNodeExpandUseCase
import com.fancy.skill_tree.core.domain.usecase.node.UpdateNodeUseCase
import com.fancy.skill_tree.core.ui.error.ErrorSeverity
import com.fancy.skill_tree.core.ui.error.ErrorStateManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 技能树界面 ViewModel
 * 负责从 Repository 加载数据并暴露 UI 状态，使用 ErrorStateManager 统一处理错误
 */
@HiltViewModel
class SkillTreeViewModel @Inject constructor(
    private val getAllNodesUseCase: GetAllNodesUseCase,
    private val createNodeUseCase: CreateNodeUseCase,
    private val updateNodeUseCase: UpdateNodeUseCase,
    private val deleteNodeUseCase: DeleteNodeUseCase,
    private val moveNodeUseCase: MoveNodeUseCase,
    private val toggleNodeExpandUseCase: ToggleNodeExpandUseCase,
    private val exportToMarkdownUseCase: ExportToMarkdownUseCase,
    private val checkAchievementsUseCase: CheckAchievementsUseCase,
    private val searchNodesUseCase: SearchNodesUseCase,
    private val getAllTagsUseCase: GetAllTagsUseCase,
    private val searchHistoryManager: SearchHistoryManager,
    private val errorStateManager: ErrorStateManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(SkillTreeUiState())
    val uiState: StateFlow<SkillTreeUiState> = _uiState.asStateFlow()

    private val _selectedNodeId = MutableStateFlow<String?>(null)
    val selectedNodeId: StateFlow<String?> = _selectedNodeId.asStateFlow()

    private val _newAchievements = MutableStateFlow<List<com.fancy.skill_tree.core.domain.model.Achievement>>(emptyList())
    val newAchievements: StateFlow<List<com.fancy.skill_tree.core.domain.model.Achievement>> = _newAchievements.asStateFlow()

    private val _searchHistory = MutableStateFlow<List<String>>(emptyList())
    val searchHistory: StateFlow<List<String>> = _searchHistory.asStateFlow()

    private val _selectedTags = MutableStateFlow<List<String>>(emptyList())
    val selectedTags: StateFlow<List<String>> = _selectedTags.asStateFlow()

    private val _allTags = MutableStateFlow<List<TagEntity>>(emptyList())
    val allTags: StateFlow<List<TagEntity>> = _allTags.asStateFlow()

    init {
        loadNodes()
        loadAllTags()
        _searchHistory.value = searchHistoryManager.getHistory()
    }

    /**
     * 从数据库加载所有节点
     */
    private fun loadNodes() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            getAllNodesUseCase()
                .catch { e ->
                    val message = handleFlowException(e)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = message
                        )
                    }
                }
                .collect { nodes ->
                    _uiState.update {
                        val filtered = if (it.searchQuery.isBlank()) nodes
                        else nodes.filter { node ->
                            node.title.contains(it.searchQuery, ignoreCase = true)
                        }
                        it.copy(
                            nodes = nodes,
                            filteredNodes = filtered,
                            isLoading = false,
                            errorMessage = null
                        )
                    }
                }
        }
    }

    /**
     * 从数据库加载所有标签
     */
    private fun loadAllTags() {
        viewModelScope.launch {
            getAllTagsUseCase()
                .catch { e ->
                    handleFlowException(e)
                }
                .collect { tags ->
                    _allTags.value = tags
                }
        }
    }

    /**
     * 搜索节点（带标签筛选）
     */
    fun search(query: String) {
        searchHistoryManager.addToHistory(query)
        _searchHistory.value = searchHistoryManager.getHistory()
        _uiState.update { it.copy(searchQuery = query) }

        viewModelScope.launch {
            searchNodesUseCase(query, _selectedTags.value)
                .catch { e -> handleFlowException(e) }
                .collect { results ->
                    _uiState.update { it.copy(filteredNodes = results) }
                }
        }
    }

    /**
     * 切换标签筛选
     */
    fun toggleTag(tagId: String) {
        val current = _selectedTags.value
        val newTags = if (current.contains(tagId)) {
            current - tagId
        } else {
            current + tagId
        }
        _selectedTags.value = newTags

        viewModelScope.launch {
            searchNodesUseCase(_uiState.value.searchQuery, newTags)
                .catch { e -> handleFlowException(e) }
                .collect { results ->
                    _uiState.update { it.copy(filteredNodes = results) }
                }
        }
    }

    /**
     * 清除搜索历史
     */
    fun clearSearchHistory() {
        searchHistoryManager.clearHistory()
        _searchHistory.value = emptyList()
    }

    /**
     * 检查成就
     */
    private suspend fun checkAchievements() {
        val newAchievements = checkAchievementsUseCase()
        if (newAchievements.isNotEmpty()) {
            _newAchievements.value = newAchievements
        }
    }

    /**
     * 创建新节点
     * @param title 节点标题
     * @param nodeType 节点类型 (ABILITY / RESOURCE)
     * @param parentId 父节点 ID (可选)
     */
    fun createNode(title: String, nodeType: String, parentId: String?) {
        viewModelScope.launch {
            when (val result = createNodeUseCase(title, nodeType, parentId)) {
                is Outcome.Success -> {
                    checkAchievements()
                }
                is Outcome.Error -> {
                    handleDomainException(result.exception)
                }
            }
        }
    }

    /**
     * 更新节点
     * @param node 要更新的节点
     */
    fun updateNode(node: SkillNodeEntity) {
        viewModelScope.launch {
            when (val result = updateNodeUseCase(node.id, node.title, node.content, node.nodeType)) {
                is Outcome.Success -> {
                    checkAchievements()
                }
                is Outcome.Error -> {
                    handleDomainException(result.exception)
                }
            }
        }
    }

    /**
     * 删除节点
     * @param nodeId 要删除的节点 ID
     */
    fun deleteNode(nodeId: String) {
        viewModelScope.launch {
            when (val result = deleteNodeUseCase(nodeId)) {
                is Outcome.Success -> {
                    clearSelectedNode()
                    checkAchievements()
                }
                is Outcome.Error -> {
                    handleDomainException(result.exception)
                }
            }
        }
    }

    /**
     * 移动节点到新的父节点
     * @param nodeId 要移动的节点 ID
     * @param newParentId 新的父节点 ID (null 表示变为根节点)
     */
    fun moveNode(nodeId: String, newParentId: String?) {
        viewModelScope.launch {
            when (val result = moveNodeUseCase(nodeId, newParentId)) {
                is Outcome.Success -> {
                    checkAchievements()
                }
                is Outcome.Error -> {
                    handleDomainException(result.exception)
                }
            }
        }
    }

    /**
     * 切换节点的展开/折叠状态
     * @param nodeId 节点 ID
     */
    fun toggleNodeExpand(nodeId: String) {
        viewModelScope.launch {
            when (val result = toggleNodeExpandUseCase(nodeId)) {
                is Outcome.Success -> { }
                is Outcome.Error -> {
                    handleDomainException(result.exception)
                }
            }
        }
    }

    /**
     * 导出所有节点为 Markdown 格式
     */
    fun exportToMarkdown(onSuccess: (String) -> Unit) {
        viewModelScope.launch {
            when (val result = exportToMarkdownUseCase()) {
                is Outcome.Success -> {
                    onSuccess(result.data)
                }
                is Outcome.Error -> {
                    handleDomainException(result.exception)
                }
            }
        }
    }

    /**
     * 显示导出成功的 Snackbar
     * @param path 导出文件路径
     */
    fun showExportSuccess(path: String) {
        errorStateManager.showSnackbar(R.string.tree_export_success, arrayOf(path), isError = false)
    }

    /**
     * 显示导出失败的 Snackbar
     * @param message 错误消息
     */
    fun showExportError(message: String) {
        errorStateManager.showSnackbar(R.string.tree_export_failed, arrayOf(message), isError = true)
    }

    /**
     * 清除错误消息
     */
    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    /**
     * 清除新成就通知
     */
    fun clearNewAchievements() {
        _newAchievements.value = emptyList()
    }

    /**
     * 选中节点
     * @param nodeId 被选中的节点 ID
     */
    fun onSelectNode(nodeId: String) {
        _selectedNodeId.value = nodeId
        _uiState.update { it.copy(selectedNodeId = nodeId) }
    }

    /**
     * 清除选中节点
     */
    fun clearSelectedNode() {
        _selectedNodeId.value = null
        _uiState.update { it.copy(selectedNodeId = null) }
    }

    /**
     * 键盘导航：选中当前节点的父节点
     */
    fun navigateToParent() {
        val currentId = _selectedNodeId.value ?: return
        val nodes = _uiState.value.nodes
        val currentNode = nodes.find { it.id == currentId } ?: return
        val parentId = currentNode.parentId ?: return
        onSelectNode(parentId)
    }

    /**
     * 键盘导航：选中当前节点的第一个子节点
     */
    fun navigateToFirstChild() {
        val currentId = _selectedNodeId.value ?: return
        val nodes = _uiState.value.nodes
        val firstChild = nodes.firstOrNull { it.parentId == currentId }
        if (firstChild != null) {
            onSelectNode(firstChild.id)
        }
    }

    /**
     * 键盘导航：选中上一个兄弟节点
     */
    fun navigateToPreviousSibling() {
        val currentId = _selectedNodeId.value ?: return
        val nodes = _uiState.value.nodes
        val currentNode = nodes.find { it.id == currentId } ?: return
        val siblings = nodes.filter { it.parentId == currentNode.parentId }
        val currentIndex = siblings.indexOf(currentNode)
        if (currentIndex > 0) {
            onSelectNode(siblings[currentIndex - 1].id)
        }
    }

    /**
     * 键盘导航：选中下一个兄弟节点
     */
    fun navigateToNextSibling() {
        val currentId = _selectedNodeId.value ?: return
        val nodes = _uiState.value.nodes
        val currentNode = nodes.find { it.id == currentId } ?: return
        val siblings = nodes.filter { it.parentId == currentNode.parentId }
        val currentIndex = siblings.indexOf(currentNode)
        if (currentIndex < siblings.size - 1) {
            onSelectNode(siblings[currentIndex + 1].id)
        }
    }

    /**
     * 搜索节点（兼容旧接口）
     * @param query 搜索关键词
     */
    fun onSearchQueryChanged(query: String) {
        search(query)
    }

    /**
     * 设置拖拽状态
     * @param draggedNodeId 正在拖拽的节点 ID
     */
    fun setDragState(draggedNodeId: String?) {
        _uiState.update { it.copy(draggedNodeId = draggedNodeId) }
    }

    /**
     * 设置拖拽目标
     * @param targetParentId 目标父节点 ID
     */
    fun setDragTarget(targetParentId: String?) {
        _uiState.update { it.copy(dragTargetParentId = targetParentId) }
    }

    /**
     * 加载示例数据
     */
    fun loadSampleData() {
        viewModelScope.launch {
            createNodeUseCase("技能树", "ABILITY", null, customId = "sample-root")
            createNodeUseCase("编程", "ABILITY", "sample-root", customId = "sample-programming")
            createNodeUseCase(
                "Python",
                "ABILITY",
                "sample-programming",
                content = "## Python 学习路线\n\n- 基础语法\n- 数据结构\n- 面向对象编程\n- 异步编程",
                customId = "sample-python"
            )
            createNodeUseCase(
                "Kotlin",
                "ABILITY",
                "sample-programming",
                content = "## Kotlin 学习路线\n\n- 协程\n- Compose\n- KMP",
                customId = "sample-kotlin"
            )
            createNodeUseCase("Web开发", "ABILITY", "sample-programming", customId = "sample-web")
            createNodeUseCase(
                "设计",
                "RESOURCE",
                "sample-root",
                content = "## 设计资源\n\n- Figma\n- 色彩理论\n- 排版原则",
                customId = "sample-design"
            )
            createNodeUseCase("数据分析", "RESOURCE", "sample-python", customId = "sample-data-analysis")

            checkAchievements()
        }
    }

    /**
     * 处理 DomainException，根据严重程度选择 Snackbar 或 Dialog 展示
     * @param e 领域层异常
     */
    private fun handleDomainException(e: com.fancy.skill_tree.core.domain.common.DomainException) {
        val uiError = errorStateManager.mapDomainException(e)
        when (uiError.severity) {
            ErrorSeverity.CRITICAL -> errorStateManager.showErrorDialog(uiError.titleResId, uiError.messageResId, uiError.messageArgs)
            else -> errorStateManager.showSnackbar(uiError.messageResId, uiError.messageArgs, isError = true)
        }
    }

    /**
     * 处理 Flow 中的异常，将 DataException 映射为用户可读消息
     * @param e Flow 中捕获的异常
     * @return 用户可读的错误消息
     */
    private fun handleFlowException(e: Throwable): String {
        return when (e) {
            is DataException -> {
                val uiError = errorStateManager.mapDataException(e)
                when (uiError.severity) {
                    ErrorSeverity.CRITICAL -> errorStateManager.showErrorDialog(uiError.titleResId, uiError.messageResId, uiError.messageArgs)
                    else -> errorStateManager.showSnackbar(uiError.messageResId, uiError.messageArgs, isError = true)
                }
                ""
            }
            else -> {
                errorStateManager.showSnackbar(R.string.common_unknown_error, isError = true)
                ""
            }
        }
    }
}
