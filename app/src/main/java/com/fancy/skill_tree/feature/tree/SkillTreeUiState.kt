package com.fancy.skill_tree.feature.tree

import com.fancy.skill_tree.core.domain.entity.SkillNodeEntity

/**
 * 技能树界面 UI 状态数据类
 * @param nodes 所有技能节点列表
 * @param isLoading 是否正在加载数据
 * @param errorMessage 错误信息（如果有）
 */
data class SkillTreeUiState(
    val nodes: List<SkillNodeEntity> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)
