package com.fancy.skill_tree.core.domain.usecase.node

import com.fancy.skill_tree.core.data.repository.SkillTreeRepository
import com.fancy.skill_tree.core.domain.common.Outcome
import javax.inject.Inject

/**
 * 确认链接 UseCase
 * 用于将 AI 推荐的链接标记为已确认
 */
class ConfirmLinkUseCase @Inject constructor(
    private val repository: SkillTreeRepository
) {
    /**
     * 确认链接
     * @param linkId 链接 ID
     * @return 操作结果
     */
    suspend operator fun invoke(
        linkId: String
    ): Outcome<Unit> {
        return try {
            repository.confirmLink(linkId)
            Outcome.Success(Unit)
        } catch (e: Exception) {
            Outcome.Error(com.fancy.skill_tree.core.domain.common.DomainException.StorageError(e))
        }
    }
}
