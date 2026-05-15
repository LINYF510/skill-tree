package com.fancy.skill_tree.feature.node

import com.fancy.skill_tree.core.data.repository.NodeLinkWithTarget
import com.fancy.skill_tree.core.domain.entity.AttachmentEntity
import com.fancy.skill_tree.core.domain.entity.SkillNodeEntity
import com.fancy.skill_tree.core.domain.entity.TagEntity

/**
 * 节点详情界面 UI 状态数据类
 * @param node 当前节点
 * @param tags 节点关联的标签列表
 * @param allTags 系统所有标签列表
 * @param allNodes 系统所有节点列表
 * @param linksWithTarget 节点的所有链接及目标节点
 * @param attachments 节点的所有附件列表
 * @param isEditing 是否处于编辑模式
 * @param isLoading 是否正在加载数据
 * @param errorMessage 错误信息（如果有）
 */
data class NodeDetailUiState(
    val node: SkillNodeEntity? = null,
    val tags: List<TagEntity> = emptyList(),
    val allTags: List<TagEntity> = emptyList(),
    val allNodes: List<SkillNodeEntity> = emptyList(),
    val linksWithTarget: List<NodeLinkWithTarget> = emptyList(),
    val attachments: List<AttachmentEntity> = emptyList(),
    val isEditing: Boolean = false,
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)
