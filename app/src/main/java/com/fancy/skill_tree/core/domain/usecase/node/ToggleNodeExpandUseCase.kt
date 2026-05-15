package com.fancy.skill_tree.core.domain.usecase.node

import com.fancy.skill_tree.core.data.repository.SkillTreeRepository
import com.fancy.skill_tree.core.domain.common.DomainException
import com.fancy.skill_tree.core.domain.common.Outcome
import com.fancy.skill_tree.core.domain.entity.SkillNodeEntity
import javax.inject.Inject

/**
 * 切换节点的展开/折叠状态
 */
class ToggleNodeExpandUseCase @Inject constructor(
    private val repository: SkillTreeRepository
) {
    /**
     * @param nodeId 节点 ID
     * @return 操作结果，包含更新后的节点或错误信息
     */
    suspend operator fun invoke(nodeId: String): Outcome<SkillNodeEntity> {
        val node = repository.getNodeById(nodeId)
            ?: return Outcome.Error(DomainException.NodeNotFound(nodeId))

        val updatedNode = node.copy(
            isExpanded = !node.isExpanded,
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
