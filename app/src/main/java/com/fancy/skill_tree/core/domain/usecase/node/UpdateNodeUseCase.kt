package com.fancy.skill_tree.core.domain.usecase.node

import com.fancy.skill_tree.core.data.repository.SkillTreeRepository
import com.fancy.skill_tree.core.domain.common.DomainException
import com.fancy.skill_tree.core.domain.common.Outcome
import com.fancy.skill_tree.core.domain.entity.SkillNodeEntity
import javax.inject.Inject

/**
 * 更新技能树节点
 */
class UpdateNodeUseCase @Inject constructor(
    private val repository: SkillTreeRepository
) {
    /**
     * @param nodeId 要更新的节点 ID
     * @param title 新的标题（可选，null 表示不更新）
     * @param content 新的内容（可选，null 表示不更新）
     * @param nodeType 新的节点类型（可选，null 表示不更新）
     * @return 操作结果，包含更新后的节点或错误信息
     */
    suspend operator fun invoke(
        nodeId: String,
        title: String? = null,
        content: String? = null,
        nodeType: String? = null
    ): Outcome<SkillNodeEntity> {
        val node = repository.getNodeById(nodeId)
            ?: return Outcome.Error(DomainException.NodeNotFound(nodeId))

        if (nodeType != null && nodeType !in listOf("ABILITY", "RESOURCE")) {
            return Outcome.Error(DomainException.InvalidNodeType(nodeType))
        }

        val updatedNode = node.copy(
            title = title?.trim() ?: node.title,
            content = content ?: node.content,
            nodeType = nodeType ?: node.nodeType,
            updatedAt = System.currentTimeMillis()
        )

        return try {
            repository.updateNode(updatedNode)
            Outcome.Success(updatedNode)
        } catch (e: Exception) {
            Outcome.Error(DomainException.StorageError(e))
        }
    }
}
