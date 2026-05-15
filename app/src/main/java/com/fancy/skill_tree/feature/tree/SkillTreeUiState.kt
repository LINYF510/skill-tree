package com.fancy.skill_tree.feature.tree

import com.fancy.skill_tree.core.domain.entity.SkillNodeEntity

/**
 * 技能树界面 UI 状态数据类
 * @param nodes 所有技能节点列表
 * @param isLoading 是否正在加载数据
 * @param errorMessage 错误信息（如果有）
 * @param selectedNodeId 当前选中的节点 ID
 * @param searchQuery 搜索关键词
 * @param filteredNodes 过滤后的节点列表
 * @param draggedNodeId 正在拖拽的节点 ID
 * @param dragTargetParentId 拖拽目标父节点 ID
 */
data class SkillTreeUiState(
    val nodes: List<SkillNodeEntity> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val selectedNodeId: String? = null,
    val searchQuery: String = "",
    val filteredNodes: List<SkillNodeEntity> = emptyList(),
    val draggedNodeId: String? = null,
    val dragTargetParentId: String? = null
)
