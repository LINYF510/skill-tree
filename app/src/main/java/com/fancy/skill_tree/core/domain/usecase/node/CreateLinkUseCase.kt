package com.fancy.skill_tree.core.domain.usecase.node

import com.fancy.skill_tree.core.data.repository.SkillTreeRepository
import com.fancy.skill_tree.core.domain.common.Outcome
import com.fancy.skill_tree.core.domain.entity.NodeLinkEntity
import javax.inject.Inject

/**
 * 创建链接 UseCase
 * 用于在两个节点之间创建跨分支链接
 */
class CreateLinkUseCase @Inject constructor(
    private val repository: SkillTreeRepository
) {
    /**
     * 创建新链接
     * @param sourceId 源节点 ID
     * @param targetId 目标节点 ID
     * @param linkType 链接类型，默认为 "MANUAL"
     * @return 操作结果，包含创建的链接或错误信息
     */
    suspend operator fun invoke(
        sourceId: String,
        targetId: String,
        linkType: String = "MANUAL"
    ): Outcome<NodeLinkEntity> {
        return try {
            val link = repository.createLink(sourceId, targetId, linkType)
            Outcome.Success(link)
        } catch (e: Exception) {
            Outcome.Error(com.fancy.skill_tree.core.domain.common.DomainException.StorageError(e))
        }
    }
}
