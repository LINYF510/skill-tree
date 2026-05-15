package com.fancy.skill_tree.core.domain.usecase.node

import com.fancy.skill_tree.core.data.repository.SkillTreeRepository
import com.fancy.skill_tree.core.domain.common.DomainException
import com.fancy.skill_tree.core.domain.common.Outcome
import javax.inject.Inject

/**
 * 移除节点标签 UseCase
 * 用于从节点移除标签关联
 */
class RemoveTagFromNodeUseCase @Inject constructor(
    private val repository: SkillTreeRepository
) {
    /**
     * 从节点移除标签
     * @param nodeId 节点 ID
     * @param tagId 标签 ID
     * @return 操作结果
     */
    suspend operator fun invoke(
        nodeId: String,
        tagId: String
    ): Outcome<Unit> {
        return try {
            repository.removeTagFromNode(nodeId, tagId)
            Outcome.Success(Unit)
        } catch (e: Exception) {
            Outcome.Error(DomainException.StorageError(e))
        }
    }
}
