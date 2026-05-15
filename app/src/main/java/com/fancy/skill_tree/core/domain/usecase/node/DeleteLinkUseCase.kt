package com.fancy.skill_tree.core.domain.usecase.node

import com.fancy.skill_tree.core.data.repository.SkillTreeRepository
import com.fancy.skill_tree.core.domain.common.Outcome
import javax.inject.Inject

/**
 * 删除链接 UseCase
 * 用于删除节点之间的跨分支链接
 */
class DeleteLinkUseCase @Inject constructor(
    private val repository: SkillTreeRepository
) {
    /**
     * 删除链接
     * @param linkId 要删除的链接 ID
     * @return 操作结果
     */
    suspend operator fun invoke(
        linkId: String
    ): Outcome<Unit> {
        return try {
            repository.deleteLink(linkId)
            Outcome.Success(Unit)
        } catch (e: Exception) {
            Outcome.Error(com.fancy.skill_tree.core.domain.common.DomainException.StorageError(e))
        }
    }
}
