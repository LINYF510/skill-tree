package com.fancy.skill_tree.core.domain.usecase.node

import com.fancy.skill_tree.core.data.repository.SkillTreeRepository
import com.fancy.skill_tree.core.domain.common.DomainException
import com.fancy.skill_tree.core.domain.common.Outcome
import javax.inject.Inject

/**
 * 删除技能树节点（包括所有子节点）
 */
class DeleteNodeUseCase @Inject constructor(
    private val repository: SkillTreeRepository
) {
    /**
     * @param nodeId 要删除的节点 ID
     * @return 操作结果
     */
    suspend operator fun invoke(nodeId: String): Outcome<Unit> {
        val node = repository.getNodeById(nodeId)
            ?: return Outcome.Error(DomainException.NodeNotFound(nodeId))

        return try {
            repository.deleteNode(nodeId)
            Outcome.Success(Unit)
        } catch (e: Exception) {
            Outcome.Error(DomainException.StorageError(e))
        }
    }
}
