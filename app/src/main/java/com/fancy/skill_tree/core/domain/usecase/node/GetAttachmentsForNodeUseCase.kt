package com.fancy.skill_tree.core.domain.usecase.node

import com.fancy.skill_tree.core.data.repository.SkillTreeRepository
import com.fancy.skill_tree.core.domain.entity.AttachmentEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * 获取节点附件 UseCase
 * 用于获取指定节点的所有附件列表
 */
class GetAttachmentsForNodeUseCase @Inject constructor(
    private val repository: SkillTreeRepository
) {
    /**
     * 获取节点的所有附件
     * @param nodeId 节点 ID
     * @return 附件列表 Flow
     */
    operator fun invoke(nodeId: String): Flow<List<AttachmentEntity>> {
        return repository.getAttachmentsForNode(nodeId)
    }
}
